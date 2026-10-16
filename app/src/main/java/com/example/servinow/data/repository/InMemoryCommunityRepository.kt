package com.example.servinow.data.repository

import com.example.servinow.core.util.EmailValidator
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.CommunityRepository
import com.example.servinow.domain.validation.RegistrationPasswordValidator
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Repositorio único de demostración. Todas las operaciones comprueban el actor y el estado. */
class InMemoryCommunityRepository @Inject constructor() : CommunityRepository {
    private val _state = MutableStateFlow(seed())
    override val state = _state.asStateFlow()

    private fun hash(value: String) =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    private val passwords =
        mutableMapOf(
            "valentina" to hash("ServiNow1!"),
            "andres" to hash("ServiNow1!"),
            "laura" to hash("ServiNow1!"),
            "moderador" to hash("Moderador1!"),
        )
    private val recovery = mutableSetOf<String>()

    private fun id() = UUID.randomUUID().toString()

    private fun notice(to: String, title: String, body: String, post: String? = null) {
        _state.value =
            _state.value.copy(
                notices = listOf(MemberNotice(id(), to, title, body, post)) + state.value.notices
            )
    }

    private fun reward(user: String, key: String, amount: Int) {
        val s = state.value
        val k = "$user:$key"
        if (k in s.rewards) return
        _state.value =
            s.copy(
                rewards = s.rewards + k,
                points = s.points + (user to ((s.points[user] ?: 0) + amount)),
            )
    }

    private fun badges(user: String) {
        val s = state.value
        val achievements =
            listOf(
                "first" to (s.posts.any { it.author == user }),
                "comments" to (s.comments.count { it.author == user } >= 10),
                "verified" to (s.rewards.count { it.startsWith("$user:verify:") } >= 10),
            )
        achievements
            .filter { it.second }
            .forEach { (name, _) ->
                val key = "$user:badge:$name"
                if (key !in state.value.rewards) {
                    reward(user, "badge:$name", 0)
                    notice(
                        user,
                        "Nuevo logro",
                        when (name) {
                            "first" -> "Primera publicación"
                            "comments" -> "10 comentarios"
                            else -> "10 publicaciones verificadas"
                        },
                    )
                }
            }
    }

    override fun login(email: String, password: String, moderator: Boolean): String? {
        EmailValidator.error(email)?.let {
            return it
        }
        val user =
            state.value.users.find { it.email.equals(email.trim(), true) }
                ?: return "Correo o contraseña incorrectos."
        if (passwords[user.id] != hash(password))
            return "La contraseña es incorrecta. Inténtalo de nuevo."
        if (moderator && user.role != MemberRole.MODERATOR)
            return "Esta cuenta no tiene permisos de moderador."
        _state.value = state.value.copy(currentUser = user.id)
        return null
    }

    override fun logout() {
        _state.value = state.value.copy(currentUser = null)
    }

    override fun register(
        name: String,
        email: String,
        password: String,
        confirmation: String,
    ): String? {
        if (name.trim().length !in 2..100) return "Escribe un nombre de 2 a 100 caracteres."
        EmailValidator.error(email)?.let {
            return it
        }
        RegistrationPasswordValidator.error(password)?.let {
            return it
        }
        if (password != confirmation) return "Las contraseñas no coinciden."
        if (state.value.users.any { it.email.equals(email.trim(), true) })
            return "Ya existe una cuenta con este correo."
        val u = Member(id(), name.trim(), email.trim().lowercase())
        passwords[u.id] = hash(password)
        _state.value = state.value.copy(users = state.value.users + u)
        return null
    }

    override fun requestRecovery(email: String): String? {
        EmailValidator.error(email)?.let {
            return it
        }
        recovery.add(email.trim().lowercase())
        return null
    }

    override fun resetPassword(email: String, password: String, confirmation: String): String? {
        val key = email.trim().lowercase()
        if (key !in recovery) return "Primero solicita el enlace de recuperación."
        RegistrationPasswordValidator.error(password)?.let {
            return it
        }
        if (password != confirmation) return "Las contraseñas no coinciden."
        val u =
            state.value.users.find { it.email.equals(key, true) }
                ?: return "No existe esta cuenta en la demostración."
        passwords[u.id] = hash(password)
        recovery.remove(key)
        return null
    }

    override fun savePost(post: WorkPost): String? {
        val me = state.value.me ?: return "Ingresa con una cuenta."
        if (me.role != MemberRole.USER) return "Publica desde una cuenta de usuario."
        if (post.title.trim().length !in 4..120 || post.description.trim().length !in 10..2000)
            return "Escribe un título de 4 a 120 caracteres y una descripción de 10 a 2000."
        if (post.category !in categories.drop(1) || post.budget <= 0 || post.city.isBlank())
            return "Completa categoría, ciudad y presupuesto válido."
        if (
            !post.latitude.isFinite() ||
                !post.longitude.isFinite() ||
                post.latitude !in -90.0..90.0 ||
                post.longitude !in -180.0..180.0
        )
            return "La ubicación no es válida."
        if (!post.hasImage) return "Selecciona una imagen de ejemplo para esta fase."
        val old = state.value.posts.find { it.id == post.id }
        if (old != null && old.author != me.id) return "Solo el autor puede editar."
        if (old?.status in listOf(PostStatus.IN_PROGRESS, PostStatus.FINISHED))
            return "No se puede editar un trabajo asignado o finalizado."
        val p =
            post.copy(
                author = me.id,
                title = post.title.trim(),
                description = post.description.trim(),
                status = PostStatus.PENDING,
                reason = "",
                assigned = null,
                interested = old?.interested ?: emptySet(),
            )
        _state.value = state.value.copy(posts = state.value.posts.filterNot { it.id == p.id } + p)
        if (old == null) reward(me.id, "create:${p.id}", 10)
        notice(me.id, "Publicación pendiente", p.title, p.id)
        badges(me.id)
        return null
    }

    override fun deletePost(id: String): String? {
        val p = state.value.posts.find { it.id == id } ?: return "La publicación no existe."
        if (p.author != state.value.currentUser) return "Solo el autor puede eliminar."
        val s = state.value
        _state.value =
            s.copy(
                posts = s.posts.filterNot { it.id == id },
                offers = s.offers.filterNot { it.post == id },
                comments = s.comments.filterNot { it.post == id },
                reviews = s.reviews.filterNot { it.post == id },
            )
        return null
    }

    private fun moderatorError() =
        if (state.value.me?.role != MemberRole.MODERATOR) "Se requiere una cuenta de moderador."
        else null

    override fun verify(id: String): String? {
        moderatorError()?.let {
            return it
        }
        val p = state.value.posts.find { it.id == id } ?: return "La publicación no existe."
        if (p.status != PostStatus.PENDING) return "Solo se revisan publicaciones pendientes."
        _state.value =
            state.value.copy(
                posts =
                    state.value.posts.map {
                        if (it.id == id) it.copy(status = PostStatus.VERIFIED, reason = "") else it
                    }
            )
        reward(p.author, "verify:$id", 20)
        notice(p.author, "Publicación verificada", p.title, id)
        badges(p.author)
        return null
    }

    override fun reject(id: String, reason: String): String? {
        moderatorError()?.let {
            return it
        }
        if (reason.trim().length !in 5..500) return "Escribe un motivo de 5 a 500 caracteres."
        val p = state.value.posts.find { it.id == id } ?: return "La publicación no existe."
        if (p.status != PostStatus.PENDING) return "Solo se rechazan publicaciones pendientes."
        _state.value =
            state.value.copy(
                posts =
                    state.value.posts.map {
                        if (it.id == id)
                            it.copy(status = PostStatus.REJECTED, reason = reason.trim())
                        else it
                    }
            )
        notice(p.author, "Publicación rechazada", reason.trim(), id)
        return null
    }

    override fun finish(id: String): String? {
        val p = state.value.posts.find { it.id == id } ?: return "La publicación no existe."
        val me = state.value.me ?: return "Ingresa con una cuenta."
        if (me.id != p.author && me.role != MemberRole.MODERATOR)
            return "Solo el autor o un moderador pueden finalizar."
        if (p.status == PostStatus.FINISHED) return "Ya está finalizada."
        _state.value =
            state.value.copy(
                posts =
                    state.value.posts.map {
                        if (it.id == id) it.copy(status = PostStatus.FINISHED) else it
                    }
            )
        notice(p.author, "Trabajo finalizado", p.title, id)
        p.assigned?.let { notice(it, "Trabajo finalizado", p.title, id) }
        return null
    }

    override fun vote(id: String): String? {
        val me = state.value.me ?: return "Ingresa con una cuenta."
        val p = state.value.posts.find { it.id == id } ?: return "No existe la publicación."
        if (p.status !in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED))
            return "La publicación no es pública."
        if (p.author == me.id) return "No puedes votar por tu propia publicación."
        val add = me.id !in p.interested
        _state.value =
            state.value.copy(
                posts =
                    state.value.posts.map {
                        if (it.id == id)
                            it.copy(
                                interested =
                                    if (add) it.interested + me.id else it.interested - me.id
                            )
                        else it
                    }
            )
        if (add) reward(p.author, "vote:$id:${me.id}", 1)
        return null
    }

    override fun comment(id: String, text: String): String? {
        val me = state.value.me ?: return "Ingresa con una cuenta."
        val p = state.value.posts.find { it.id == id } ?: return "No existe la publicación."
        if (p.status !in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED))
            return "Solo se comentan publicaciones públicas."
        if (text.trim().length !in 1..1000) return "Escribe un comentario de hasta 1000 caracteres."
        val c = WorkComment(id(), id, me.id, text.trim())
        _state.value = state.value.copy(comments = state.value.comments + c)
        reward(me.id, "comment:${c.id}", 2)
        if (p.author != me.id) notice(p.author, "Nuevo comentario", "${me.name}: ${c.text}", id)
        badges(me.id)
        return null
    }

    override fun offer(id: String, price: String, text: String, availability: String): String? {
        val me = state.value.me ?: return "Ingresa con una cuenta."
        val p = state.value.posts.find { it.id == id } ?: return "No existe la publicación."
        if (me.role != MemberRole.USER || me.id == p.author || p.status != PostStatus.VERIFIED)
            return "No puedes ofertar en este trabajo."
        if (state.value.offers.any { it.post == id && it.worker == me.id })
            return "Ya enviaste una oferta."
        val amount = price.toLongOrNull()
        if (
            amount == null ||
                amount <= 0 ||
                text.trim().length !in 5..1000 ||
                availability.isBlank()
        )
            return "Completa un precio positivo, la propuesta y disponibilidad."
        val o = WorkOffer(id(), id, me.id, amount, text.trim(), availability.trim())
        _state.value = state.value.copy(offers = state.value.offers + o)
        notice(p.author, "Nueva oferta", "${me.name} ofertó $amount COP", id)
        return null
    }

    override fun accept(offerId: String): String? {
        val o = state.value.offers.find { it.id == offerId } ?: return "Oferta no disponible."
        val p = state.value.posts.find { it.id == o.post } ?: return "Publicación no disponible."
        if (p.author != state.value.currentUser) return "Solo el autor puede aceptar ofertas."
        if (p.status != PostStatus.VERIFIED || p.assigned != null)
            return "El trabajo ya no está disponible."
        _state.value =
            state.value.copy(
                posts =
                    state.value.posts.map {
                        if (it.id == p.id)
                            it.copy(status = PostStatus.IN_PROGRESS, assigned = o.worker)
                        else it
                    }
            )
        notice(o.worker, "Oferta aceptada", p.title, p.id)
        return null
    }

    override fun review(id: String, stars: Int, text: String): String? {
        val p = state.value.posts.find { it.id == id } ?: return "Trabajo no disponible."
        val worker = p.assigned ?: return "No hay trabajador asignado."
        if (p.author != state.value.currentUser || p.status != PostStatus.FINISHED)
            return "Solo el autor puede reseñar un trabajo finalizado."
        if (state.value.reviews.any { it.post == id }) return "Ya calificaste este trabajo."
        if (stars !in 1..5 || text.trim().length !in 1..1000)
            return "Selecciona estrellas y escribe tu experiencia."
        _state.value =
            state.value.copy(
                reviews =
                    state.value.reviews +
                        WorkerReview(id(), id, worker, p.author, stars, text.trim())
            )
        notice(worker, "Nueva reseña", "Recibiste $stars estrellas", id)
        return null
    }

    override fun updateMember(member: Member): String? {
        if (member.id != state.value.currentUser) return "Solo puedes editar tu perfil."
        val old = state.value.me ?: return "Ingresa con una cuenta."
        EmailValidator.error(member.email)?.let {
            return it
        }
        if (member.name.isBlank() || member.years !in 0..80)
            return "Verifica el nombre y los años de experiencia."
        if (
            state.value.users.any {
                it.id != member.id && it.email.equals(member.email.trim(), true)
            }
        )
            return "El correo ya está registrado."
        _state.value =
            state.value.copy(
                users =
                    state.value.users.map {
                        if (it.id == member.id)
                            member.copy(role = old.role, email = member.email.trim().lowercase())
                        else it
                    }
            )
        return null
    }

    override fun deleteAccount(confirmation: String): String? {
        if (confirmation != "ELIMINAR") return "Escribe ELIMINAR para confirmar."
        val me = state.value.me ?: return "Ingresa con una cuenta."
        if (me.role == MemberRole.MODERATOR)
            return "Las cuentas de moderación se administran externamente."
        val s = state.value
        val own = s.posts.filter { it.author == me.id }.map { it.id }.toSet()
        passwords.remove(me.id)
        _state.value =
            s.copy(
                users = s.users.filterNot { it.id == me.id },
                posts =
                    s.posts
                        .filterNot { it.id in own }
                        .map {
                            if (it.assigned == me.id)
                                it.copy(assigned = null, status = PostStatus.VERIFIED)
                            else it.copy(interested = it.interested - me.id)
                        },
                offers = s.offers.filterNot { it.post in own || it.worker == me.id },
                comments = s.comments.filterNot { it.post in own || it.author == me.id },
                reviews =
                    s.reviews.filterNot {
                        it.post in own || it.author == me.id || it.worker == me.id
                    },
                notices = s.notices.filterNot { it.recipient == me.id },
                points = s.points - me.id,
                currentUser = null,
            )
        return null
    }

    override fun markNotice(id: String, read: Boolean) {
        _state.value =
            state.value.copy(
                notices =
                    state.value.notices.map {
                        if (it.id == id && it.recipient == state.value.currentUser)
                            it.copy(read = read)
                        else it
                    }
            )
    }

    override fun markAllNotices() {
        _state.value =
            state.value.copy(
                notices =
                    state.value.notices.map {
                        if (it.recipient == state.value.currentUser) it.copy(read = true) else it
                    }
            )
    }

    private fun seed(): CommunityState {
        val users =
            listOf(
                Member("valentina", "Valentina Gómez", "valentina@gmail.com"),
                Member(
                    "andres",
                    "Andrés Moreno",
                    "andres@gmail.com",
                    profession = "Plomería y mantenimiento",
                    bio =
                        "Soluciones claras para tu hogar. Seis años atendiendo reparaciones y mantenimiento.",
                    experience =
                        "Independiente · 2021 a la fecha. Reparación de redes y mantenimiento residencial.",
                    training = "Técnico en mantenimiento · 2020",
                    skills = "Redes hidráulicas, diagnóstico y presupuestos",
                    years = 6,
                ),
                Member(
                    "laura",
                    "Laura Pérez",
                    "laura@hotmail.com",
                    profession = "Pintura y acabados",
                    bio = "Acabados limpios y atención puntual.",
                    experience = "Pintora independiente desde 2022",
                    training = "Curso de acabados",
                    skills = "Pintura interior, preparación de superficies",
                    years = 4,
                ),
                Member(
                    "moderador",
                    "Moderador ServiNow",
                    "moderador@gmail.com",
                    MemberRole.MODERATOR,
                ),
            )
        val posts =
            listOf(
                WorkPost(
                    "p1",
                    "valentina",
                    "Reparar fuga en la cocina",
                    "Plomería",
                    "Hay una fuga debajo del lavaplatos. Necesito revisar el sifón y reparar la conexión.",
                    "Bogotá",
                    4.659,
                    -74.062,
                    120000,
                    status = PostStatus.VERIFIED,
                ),
                WorkPost(
                    "p2",
                    "valentina",
                    "Instalar dos lámparas",
                    "Electricidad",
                    "Instalar dos lámparas de techo y revisar las conexiones del apartamento.",
                    "Bogotá",
                    4.63,
                    -74.074,
                    90000,
                ),
                WorkPost(
                    "p3",
                    "laura",
                    "Ajustar puertas del armario",
                    "Carpintería",
                    "Las puertas rozan al cerrar. Necesito ajustar bisagras y alineación.",
                    "Bogotá",
                    4.664,
                    -74.056,
                    150000,
                    status = PostStatus.VERIFIED,
                ),
                WorkPost(
                    "p4",
                    "valentina",
                    "Mantenimiento del baño",
                    "Mantenimiento",
                    "Revisar las llaves del baño y cambiar empaques de la ducha.",
                    "Medellín",
                    6.244,
                    -75.581,
                    80000,
                    status = PostStatus.REJECTED,
                    reason =
                        "Especifica el alcance del mantenimiento y los elementos que requieren reparación.",
                ),
                WorkPost(
                    "p5",
                    "valentina",
                    "Pintar una habitación",
                    "Pintura",
                    "Preparación y pintura de una habitación de 12 metros cuadrados.",
                    "Bogotá",
                    4.65,
                    -74.061,
                    140000,
                    status = PostStatus.IN_PROGRESS,
                    assigned = "andres",
                ),
                WorkPost(
                    "p6",
                    "valentina",
                    "Reparar puerta principal",
                    "Carpintería",
                    "Se ajustaron las bisagras y la cerradura de la puerta principal.",
                    "Bogotá",
                    4.65,
                    -74.06,
                    110000,
                    status = PostStatus.FINISHED,
                    assigned = "andres",
                ),
            )
        val offers =
            listOf(
                WorkOffer(
                    "o1",
                    "p1",
                    "andres",
                    110000,
                    "Revisión y cambio de empaques. Mano de obra incluida.",
                    "Mañana a las 9",
                ),
                WorkOffer(
                    "o2",
                    "p1",
                    "laura",
                    125000,
                    "Revisión inicial y reparación de la conexión.",
                    "Viernes",
                ),
                WorkOffer(
                    "o3",
                    "p5",
                    "andres",
                    140000,
                    "Preparación y dos manos de pintura.",
                    "Hoy",
                ),
            )
        val initial = CommunityState(
            users,
            posts,
            offers,
            comments =
                listOf(
                    WorkComment("c1", "p1", "andres", "¿La fuga continúa con la llave cerrada?")
                ),
            reviews =
                listOf(
                    WorkerReview(
                        "r1",
                        "p6",
                        "andres",
                        "valentina",
                        5,
                        "Puntual, amable y muy claro con el presupuesto.",
                    )
                ),
            notices =
                listOf(
                    MemberNotice(
                        "n1",
                        "valentina",
                        "Nuevo comentario",
                        "Andrés preguntó por la fuga",
                        "p1",
                    ),
                    MemberNotice(
                        "n2",
                        "valentina",
                        "Publicación rechazada",
                        "Revisa el motivo y corrige la publicación",
                        "p4",
                    ),
                ),

        )
        val earned = mutableMapOf<String, Int>()
        initial.posts.forEach { post ->
            earned["${post.author}:create:${post.id}"] = 10
            earned["${post.author}:badge:first"] = 0
            if (post.status in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED)) {
                earned["${post.author}:verify:${post.id}"] = 20
            }
            post.interested.forEach { voter -> earned["${post.author}:vote:${post.id}:$voter"] = 1 }
        }
        initial.comments.forEach { earned["${it.author}:comment:${it.id}"] = 2 }
        return initial.copy(
            rewards = earned.keys.toSet(),
            points = initial.users.associate { user -> user.id to earned.filterKeys { it.startsWith("${user.id}:") }.values.sum() },
        )
    }
}
