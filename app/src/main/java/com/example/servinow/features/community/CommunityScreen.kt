package com.example.servinow.features.community

import com.example.servinow.features.welcome.WelcomeScreen
import com.example.servinow.features.welcome.OnboardingScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.servinow.navigation.NavigationEffects
import com.example.servinow.navigation.ServiNowNavHost
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.servinow.R
import com.example.servinow.core.component.PasswordVisibilityButton
import com.example.servinow.core.component.screenSpacing
import com.example.servinow.domain.model.*
import com.example.servinow.domain.validation.reputationLevel
import com.example.servinow.features.explore.ExploreMap
import com.example.servinow.features.recovery.RecoveryHeader
import java.text.NumberFormat
import java.util.Locale

private fun money(n: Long) =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(n) + " COP"

@Composable
private fun Title(s: String) {
    Text(s, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun Action(s: String, click: () -> Unit) {
    Button(
        click,
        Modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(s)
    }
}

@Composable
private fun Link(s: String, click: () -> Unit) {
    OutlinedButton(click, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(s) }
}

@Composable
private fun Field(
    s: String,
    value: String,
    change: (String) -> Unit,
    multi: Boolean = false,
    password: Boolean = false,
) {
    var visible by rememberSaveable(s) { mutableStateOf(false) }
    OutlinedTextField(
        value,
        change,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(s) },
        shape = RoundedCornerShape(16.dp),
        singleLine = !multi,
        minLines = if (multi) 3 else 1,
        visualTransformation =
            if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon =
            if (password) {
                { PasswordVisibilityButton(visible) { visible = !visible } }
            } else null,
    )
}

@Composable
private fun Choice(label: String, value: String, values: List<String>, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Link("$label: $value ▾") { open = true }
        DropdownMenu(open, { open = false }) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v) },
                    onClick = {
                        choose(v)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExampleImage() {
    Image(
        painterResource(R.drawable.service_example),
        "Imagen de ejemplo del servicio",
        modifier = Modifier.fillMaxWidth().height(145.dp),
    )
    Text(
        "Imagen ilustrativa de prueba · carga externa exenta en esta fase",
        style = MaterialTheme.typography.labelSmall,
    )
}

private fun title(page: String) =
    when (page) {
        "login" -> "ServiNow"
        "register" -> "Crear cuenta"
        "recovery" -> "Recuperar contraseña"
        "sent" -> "Enlace de recuperación"
        "new-password" -> "Nueva contraseña"
        "home" -> "ServiNow"
        "filters" -> "Filtros de búsqueda"
        "map",
        "location",
        "post-location" -> "Elegir ubicación"
        "detail" -> "Detalle del trabajo"
        "create" -> "Nueva solicitud"
        "create-details" -> "Detalles del trabajo"
        "preview" -> "Revisar publicación"
        "submitted" -> "Solicitud guardada"
        "mine" -> "Mis publicaciones"
        "delete-post" -> "Eliminar publicación"
        "offers" -> "Ofertas recibidas"
        "confirm-offer" -> "Confirmar trabajador"
        "tracking" -> "Seguimiento del trabajo"
        "finish",
        "mod-finish" -> "Finalizar publicación"
        "review" -> "Calificar experiencia"
        "offer" -> "Enviar oferta"
        "my-offers" -> "Mis ofertas"
        "worker" -> "Panel del trabajador"
        "profile" -> "Perfil profesional"
        "cv" -> "Currículum"
        "professional" -> "Editar perfil profesional"
        "notifications" -> "Avisos"
        "dashboard" -> "Mis estadísticas"
        "achievements" -> "Mis logros"
        "account" -> "Mi cuenta"
        "edit-account" -> "Datos personales"
        "delete-account" -> "Eliminar cuenta"
        "mod-login" -> "Acceso de moderador"
        "moderation" -> "Bandeja de moderación"
        "mod-detail" -> "Revisar publicación"
        "reject" -> "Motivo del rechazo"
        "empty" -> "Sin resultados"
        "error" -> "Error de conexión"
        else -> "Comentarios"
    }

@Composable
fun CommunityRoute(vm: CommunityViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentPage = entry?.destination?.route ?: "welcome"
    NavigationEffects(navController, vm)
    LaunchedEffect(currentPage) { vm.destinationChanged(currentPage) }
    val s by vm.state.collectAsState()
    val u by vm.ui.collectAsState()
    val me = s.me
    val post = s.posts.find { it.id == u.postId }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Todas") }
    var city by rememberSaveable { mutableStateOf("Todas") }
    var order by rememberSaveable { mutableStateOf("Recientes") }
    var onlyOpen by rememberSaveable { mutableStateOf(true) }
    var lat by rememberSaveable { mutableStateOf(4.6486) }
    var lng by rememberSaveable { mutableStateOf(-74.0628) }
    var nearby by rememberSaveable { mutableStateOf(false) }
    fun name(id: String?) = s.users.find { it.id == id }?.name ?: "Usuario eliminado"
    fun publicPosts() =
        s.posts.filter {
            it.status in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED)
        }
    val results =
        publicPosts()
            .filter {
                (category == "Todas" || it.category == category) &&
                    (city == "Todas" || it.city == city) &&
                    (!onlyOpen || it.status == PostStatus.VERIFIED) &&
                    it.title.contains(query, true) &&
                    (!nearby ||
                        com.example.servinow.domain.validation.distanceKm(
                            Job(
                                0,
                                it.title,
                                it.category,
                                it.city,
                                0,
                                0,
                                0,
                                it.latitude,
                                it.longitude,
                            ),
                            SearchFilters(latitude = lat, longitude = lng),
                        ) <= 5)
            }
            .let {
                if (order == "Más relevantes") it.sortedByDescending { it.interested.size }
                else it.sortedByDescending { it.createdAt }
            }
    u.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.message(null) },
            title = { Text("ServiNow") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.message(null) }) { Text("Entendido") } },
        )
    }
    Scaffold(
        bottomBar = {
            if (me != null && currentPage !in setOf("welcome", "onboarding")) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val tabs =
                            if (me.role == MemberRole.MODERATOR)
                                listOf(
                                    "Revisar" to "moderation",
                                    "Avisos" to "notifications",
                                    "Cuenta" to "account",
                                )
                            else
                                listOf(
                                    "Inicio" to "home",
                                    "Trabajos" to "mine",
                                    "Avisos" to "notifications",
                                    "Cuenta" to "account",
                                )
                        tabs.forEachIndexed { i, (label, page) ->
                            Surface(
                                onClick = { vm.go(page) },
                                color =
                                    if (currentPage == page) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.primary,
                                contentColor =
                                    if (currentPage == page) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        painterResource(
                                            when (page) {
                                                "home",
                                                "moderation" -> R.drawable.ic_home
                                                "mine" -> R.drawable.ic_work
                                                "notifications" -> R.drawable.ic_bell
                                                else -> R.drawable.ic_account
                                            }
                                        ),
                                        null,
                                        Modifier.size(22.dp),
                                    )
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (currentPage !in setOf("welcome", "onboarding")) RecoveryHeader(
                title(currentPage),
                vm::back,
                { vm.go("notifications") },
                showNotifications = currentPage in setOf("profile", "account"),
            )
            ServiNowNavHost(navController, Modifier.weight(1f)) { destination ->
                val u = u.copy(page = destination)
                val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(screenSpacing()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                key(u.page) {
                    when (u.page) {
                        "welcome" -> WelcomeScreen(onStart = { vm.go("onboarding") }, onLogin = { vm.go("login") })
                        "onboarding" -> OnboardingScreen(onFinish = { vm.go("register") }, onLogin = { vm.go("login") })
                        "login",
                        "mod-login" -> {
                            var email by rememberSaveable { mutableStateOf("") }
                            var password by rememberSaveable { mutableStateOf("") }
                            Text(
                                "QUÉ BUENO VERTE",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Title(
                                if (u.page == "mod-login") "Gestiona tu comunidad."
                                else "Bienvenido de nuevo."
                            )
                            Text("Tus trabajos, tus conexiones y nuevas oportunidades te esperan.")
                            Field("Correo electrónico", email, { email = it })
                            Field("Contraseña", password, { password = it }, password = true)
                            TextButton(onClick = { vm.go("recovery") }) {
                                Text("Olvidé mi contraseña")
                            }
                            Action("Iniciar sesión") {
                                vm.login(email, password, u.page == "mod-login")
                            }
                            if (u.page == "login") {
                                Action("Continuar con Google") {
                                    vm.message(
                                        "Google es un servicio externo. En esta fase se evalúa el acceso con las cuentas de prueba; no se inicia una sesión de Google."
                                    )
                                }
                                Link("¿Eres nuevo? Crea tu cuenta") { vm.go("register") }
                                TextButton(onClick = { vm.go("mod-login") }) {
                                    Text("Soy moderador")
                                }
                            }
                            Card {
                                Text("Acceso para la evaluación", fontWeight = FontWeight.Bold)
                                Text(
                                    if (u.page == "mod-login") "moderador@gmail.com\nModerador1!"
                                    else "valentina@gmail.com o andres@gmail.com\nServiNow1!"
                                )
                                Text(
                                    "Datos en memoria. Se reinician al cerrar el proceso.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        "register" -> {
                            var n by rememberSaveable { mutableStateOf("") }
                            var e by rememberSaveable { mutableStateOf("") }
                            var p by rememberSaveable { mutableStateOf("") }
                            var c by rememberSaveable { mutableStateOf("") }
                            Title("Crea tu cuenta.")
                            Text("Solicita ayuda y ofrece tus servicios desde la misma cuenta.")
                            Field("Nombre completo", n, { n = it })
                            Field("Correo electrónico", e, { e = it })
                            Field("Contraseña", p, { p = it }, password = true)
                            Field("Confirmar contraseña", c, { c = it }, password = true)
                            Action("Crear cuenta") { vm.register(n, e, p, c) }
                            Text(
                                "La dirección se puede completar desde Mi cuenta. No se requiere mapa para esta entrega.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        "recovery" -> {
                            var e by rememberSaveable { mutableStateOf("") }
                            Icon(
                                painterResource(R.drawable.ic_mail),
                                null,
                                Modifier.align(Alignment.CenterHorizontally).size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title("Recupera tu acceso.")
                            Text("Escribe el correo de tu cuenta.")
                            Field("Correo electrónico", e, { e = it })
                            Action("Solicitar enlace") { vm.recovery(e) }
                            Text("Simulación académica: no se enviará un correo real.")
                        }
                        "sent" -> {
                            Icon(
                                painterResource(R.drawable.ic_circle_check),
                                null,
                                Modifier.align(Alignment.CenterHorizontally).size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title("Solicitud preparada.")
                            Text(
                                "${u.recoveryEmail}\nSi corresponde a una cuenta, podrás restablecer su contraseña."
                            )
                            Card {
                                Text("Enlace simulado", fontWeight = FontWeight.Bold)
                                Text(
                                    "Para evaluar el flujo sin un servicio de correo, abre el enlace de prueba dentro de la app."
                                )
                                Action("Abrir enlace de prueba") { vm.go("new-password") }
                            }
                            Link("Reenviar solicitud") { vm.recovery(u.recoveryEmail) }
                            Link("Volver al login") { vm.go("login") }
                        }
                        "new-password" -> {
                            var p by rememberSaveable { mutableStateOf("") }
                            var c by rememberSaveable { mutableStateOf("") }
                            Text(
                                "Recuperación de cuenta",
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Title("Una contraseña nueva.")
                            Text(
                                "Elige una contraseña que puedas recordar y sea difícil de adivinar."
                            )
                            Field("Nueva contraseña", p, { p = it }, password = true)
                            Field("Confirmar contraseña", c, { c = it }, password = true)
                            Action("Guardar contraseña") { vm.reset(p, c) }
                        }
                        "home",
                        "empty" -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("TU COMUNIDAD, A UN PASO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Title("Hola, ${me?.name?.substringBefore(' ')}.")
                                    Text("¿Qué resolvemos hoy?", style = MaterialTheme.typography.titleMedium)
                                    Text("Encuentra ayuda para tus proyectos o comparte lo que sabes hacer.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Surface(
                                onClick = vm::startPost,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("+", style = MaterialTheme.typography.headlineLarge)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Publicar un trabajo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Cuéntanos qué necesitas y recibe ofertas.", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("→", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Surface(
                                onClick = { vm.go("worker") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Quiero trabajar", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text("Encuentra oportunidades para tus habilidades.", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("→", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Text("Explora los trabajos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Field("Buscar un trabajo", query, { query = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { vm.go("filters") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Filtros") }
                                OutlinedButton(onClick = { vm.go("map") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Ver mapa") }
                            }
                            Text("${results.size} ${if (results.size == 1) "trabajo disponible" else "trabajos disponibles"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (results.isEmpty())
                                Card {
                                    Text("No encontramos trabajos con esos filtros.")
                                    Action("Restablecer filtros") {
                                        query = ""
                                        category = "Todas"
                                        city = "Todas"
                                        nearby = false
                                        onlyOpen = true
                                    }
                                }
                            results.forEach { p ->
                                PostCard(p, name(p.author)) { vm.openPost(p.id) }
                            }
                        }
                        "filters" -> {
                            Title("Encuentra lo que buscas.")
                            Choice("Categoría", category, categories) { category = it }
                            Choice("Ciudad", city, listOf("Todas", "Bogotá", "Medellín", "Cali")) {
                                city = it
                            }
                            Choice("Orden", order, listOf("Recientes", "Más relevantes")) {
                                order = it
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(onlyOpen, { onlyOpen = it })
                                Text("Solo trabajos abiertos")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(nearby, { nearby = it })
                                Text("Cercanos: hasta 5 km")
                            }
                            Link("Elegir centro en mapa") { vm.go("location") }
                            Action("Aplicar filtros") { vm.go("home") }
                            Link("Restablecer filtros") {
                                query = ""
                                category = "Todas"
                                city = "Todas"
                                nearby = false
                                onlyOpen = true
                            }
                        }
                        "location",
                        "map" -> {
                            Title("Elige tu zona.")
                            Text("Toca el mapa para seleccionar un punto.")
                            val jobs = results.mapIndexed { i, p ->
                                Job(
                                    i,
                                    p.title,
                                    p.category,
                                    p.city,
                                    p.budget.toInt(),
                                    0,
                                    p.interested.size,
                                    p.latitude,
                                    p.longitude,
                                )
                            }
                            ExploreMap(
                                if (u.page == "map") jobs else emptyList(),
                                SearchFilters(latitude = lat, longitude = lng),
                                Modifier.fillMaxWidth().height(340.dp),
                                { i -> results.getOrNull(i)?.let { vm.openPost(it.id) } },
                                onLocation =
                                    if (u.page == "location") {
                                        { x, y ->
                                            lat = x
                                            lng = y
                                        }
                                    } else null,
                            )
                            Text("%.5f, %.5f".format(Locale.US, lat, lng))
                            Action(if (u.page == "map") "Ver lista" else "Usar esta ubicación") {
                                if (u.page == "location") {
                                    nearby = true
                                    vm.go("filters")
                                } else vm.go("home")
                            }
                        }
                        "detail",
                        "comments" -> {
                            if (post == null) Text("Esta publicación fue eliminada.")
                            else {
                                Title(post.title)
                                Text(
                                    "${post.category} · ${post.status.label}",
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (post.hasImage) ExampleImage()
                                Text(post.description)
                                Text("${post.city} · ${money(post.budget)}")
                                Text(
                                    "${post.latitude}, ${post.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text("Publicado por ${name(post.author)}")
                                if (post.reason.isNotEmpty())
                                    Card {
                                        Text("Motivo del rechazo", fontWeight = FontWeight.Bold)
                                        Text(post.reason)
                                    }
                                if (
                                    post.status in
                                        listOf(
                                            PostStatus.VERIFIED,
                                            PostStatus.IN_PROGRESS,
                                            PostStatus.FINISHED,
                                        )
                                ) {
                                    Action(
                                        "${if(me?.id in post.interested)"✓ Te interesa" else "Me interesa"} · ${post.interested.size}",
                                        vm::vote,
                                    )
                                    if (post.author != me?.id && post.status == PostStatus.VERIFIED)
                                        Action("Enviar una oferta") { vm.go("offer") }
                                }
                                if (post.author == me?.id) {
                                    Link("Ver ofertas (${s.offers.count{it.post==post.id}})") {
                                        vm.go("offers")
                                    }
                                    Link("Seguimiento del trabajo") { vm.go("tracking") }
                                    if (
                                        post.status in
                                            listOf(
                                                PostStatus.PENDING,
                                                PostStatus.VERIFIED,
                                                PostStatus.REJECTED,
                                            )
                                    )
                                        Link("Editar publicación") { vm.editPost(post) }
                                    Link("Eliminar publicación") { vm.go("delete-post") }
                                }
                                Title("Comentarios")
                                s.comments
                                    .filter { it.post == post.id }
                                    .forEach { c ->
                                        Card {
                                            Text(name(c.author), fontWeight = FontWeight.Bold)
                                            Text(c.text)
                                        }
                                    }
                                var text by rememberSaveable { mutableStateOf("") }
                                Field("Agregar comentario", text, { text = it }, true)
                                Action("Publicar comentario") { if (vm.comment(text)) text = "" }
                            }
                        }
                        "create",
                        "create-details",
                        "post-location",
                        "preview" -> {
                            val d = u.draft
                            if (d == null) Text("Comienza una nueva solicitud.")
                            else
                                when (u.page) {
                        "welcome" -> WelcomeScreen(onStart = { vm.go("onboarding") }, onLogin = { vm.go("login") })
                        "onboarding" -> OnboardingScreen(onFinish = { vm.go("register") }, onLogin = { vm.go("login") })
                                    "create" -> {
                                        Text(
                                            "Paso 1 de 4",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title("¿Qué servicio necesitas?")
                                        Choice("Categoría", d.category, categories.drop(1)) {
                                            vm.draft(d.copy(category = it))
                                        }
                                        Field(
                                            "Título del trabajo",
                                            d.title,
                                            { vm.draft(d.copy(title = it)) },
                                        )
                                        Action("Continuar") {
                                            if (d.title.trim().length < 4)
                                                vm.message(
                                                    "Escribe un título de al menos 4 caracteres."
                                                )
                                            else vm.go("create-details")
                                        }
                                    }
                                    "create-details" -> {
                                        Text(
                                            "Paso 2 de 4",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title("Cuéntanos los detalles.")
                                        Field(
                                            "Descripción",
                                            d.description,
                                            { vm.draft(d.copy(description = it)) },
                                            true,
                                        )
                                        var budget by rememberSaveable {
                                            mutableStateOf(
                                                d.budget.takeIf { it > 0 }?.toString().orEmpty()
                                            )
                                        }
                                        Field(
                                            "Presupuesto en COP",
                                            budget,
                                            {
                                                budget = it
                                                vm.draft(d.copy(budget = it.toLongOrNull() ?: 0))
                                            },
                                        )
                                        if (d.hasImage) ExampleImage()
                                        Link(
                                            if (d.hasImage) "Quitar imagen de ejemplo"
                                            else "Agregar imagen de ejemplo"
                                        ) {
                                            vm.draft(d.copy(hasImage = !d.hasImage))
                                        }
                                        Action("Continuar") {
                                            if (
                                                d.description.trim().length < 10 ||
                                                    d.budget <= 0 ||
                                                    !d.hasImage
                                            )
                                                vm.message(
                                                    "Completa la descripción, un presupuesto positivo y la imagen de ejemplo."
                                                )
                                            else vm.go("post-location")
                                        }
                                    }
                                    "post-location" -> {
                                        Text(
                                            "Paso 3 de 4",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title("¿Dónde es el trabajo?")
                                        Choice(
                                            "Ciudad",
                                            d.city,
                                            listOf("Bogotá", "Medellín", "Cali"),
                                        ) { c ->
                                            val point =
                                                when (c) {
                                                    "Medellín" -> 6.244 to -75.581
                                                    "Cali" -> 3.451 to -76.532
                                                    else -> 4.6486 to -74.0628
                                                }
                                            vm.draft(
                                                d.copy(
                                                    city = c,
                                                    latitude = point.first,
                                                    longitude = point.second,
                                                )
                                            )
                                        }
                                        Text(
                                            "Ubicación de referencia: ${d.latitude}, ${d.longitude}"
                                        )
                                        Text(
                                            "Esta fase permite una ubicación de ejemplo sin mapas ni permisos."
                                        )
                                        Action("Revisar publicación") { vm.go("preview") }
                                    }
                                    else -> {
                                        Text(
                                            "Paso 4 de 4",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title(d.title)
                                        ExampleImage()
                                        Text("${d.category} · ${d.city}")
                                        Text(d.description)
                                        Text(money(d.budget))
                                        Text(
                                            "La publicación quedará pendiente y no aparecerá en el feed hasta su verificación."
                                        )
                                        Action("Enviar a revisión", vm::savePost)
                                    }
                                }
                        }
                        "submitted" -> {
                            Icon(
                                painterResource(R.drawable.ic_circle_check),
                                null,
                                Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title("¡Ya diste el primer paso!")
                            Text("Pendiente de verificación")
                            Text(post?.title.orEmpty())
                            Action("Ver mis publicaciones") { vm.go("mine") }
                        }
                        "mine" -> {
                            Title("Mis solicitudes.")
                            Action("Nueva solicitud", vm::startPost)
                            var filter by rememberSaveable { mutableStateOf("Todas") }
                            Choice(
                                "Estado",
                                filter,
                                listOf("Todas") + PostStatus.entries.map { it.label },
                            ) {
                                filter = it
                            }
                            val list =
                                s.posts.filter {
                                    it.author == me?.id &&
                                        (filter == "Todas" || it.status.label == filter)
                                }
                            if (list.isEmpty()) Text("No tienes publicaciones con este estado.")
                            list.forEach { p -> PostCard(p, name(p.author)) { vm.openPost(p.id) } }
                        }
                        "delete-post" -> {
                            Title("¿Eliminar esta publicación?")
                            Text(post?.title.orEmpty())
                            Text("También se eliminarán sus ofertas y comentarios en memoria.")
                            Action("Sí, eliminar publicación", vm::deletePost)
                            Link("Conservar publicación") { vm.go("detail") }
                        }
                        "offers" -> {
                            Title("Elige con confianza.")
                            Text(post?.title.orEmpty())
                            val offers = s.offers.filter { it.post == u.postId }
                            if (offers.isEmpty()) Text("Todavía no hay ofertas.")
                            offers.forEach { o ->
                                Card {
                                    Text(name(o.worker), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(o.description)
                                    Text(o.availability)
                                    Link("Ver perfil y CV") { vm.profile(o.worker) }
                                    if (post?.status == PostStatus.VERIFIED)
                                        Action("Elegir trabajador") { vm.selectOffer(o.id) }
                                    else
                                        Text(
                                            if (post?.assigned == o.worker) "Trabajador asignado"
                                            else "Oferta no seleccionada"
                                        )
                                }
                            }
                        }
                        "confirm-offer" -> {
                            val o = s.offers.find { it.id == u.offerId }
                            Title("Un paso más.")
                            if (o != null)
                                Card {
                                    Text(name(o.worker), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(o.description)
                                }
                            Text(
                                "Al confirmar, el trabajo queda asignado y deja de recibir ofertas."
                            )
                            Action("Confirmar trabajador", vm::accept)
                            Link("Seguir comparando") { vm.go("offers") }
                        }
                        "tracking" -> {
                            Title("Un trabajo en buenas manos.")
                            Text(post?.title.orEmpty())
                            Text(
                                post?.status?.label.orEmpty(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            post?.assigned?.let { id ->
                                Card {
                                    Text(name(id), fontWeight = FontWeight.Bold)
                                    Text(s.users.find { it.id == id }?.profession.orEmpty())
                                    Link("Ver perfil y CV") { vm.profile(id) }
                                }
                            }
                            listOf(
                                    "Publicación verificada",
                                    "Oferta aceptada",
                                    "Trabajo en progreso",
                                    "Finalización y reseña",
                                )
                                .forEachIndexed { i, label ->
                                    Card {
                                        Text(label, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (
                                                post?.status == PostStatus.FINISHED ||
                                                    i < 3 && post?.assigned != null
                                            )
                                                "Completado ✓"
                                            else "Pendiente"
                                        )
                                    }
                                }
                            if (post != null && post.author == me?.id) {
                                if (post.status != PostStatus.FINISHED)
                                    Action("Marcar como finalizado") { vm.go("finish") }
                                else if (
                                    post.assigned != null && s.reviews.none { it.post == post.id }
                                )
                                    Action("Calificar experiencia") { vm.go("review") }
                            }
                            Link("Ver conversación") { vm.go("comments") }
                        }
                        "finish",
                        "mod-finish" -> {
                            Title("Finalizar publicación.")
                            Text(post?.title.orEmpty())
                            Text(
                                "Confirma que el trabajo ya está resuelto. El autor y el trabajador recibirán un aviso."
                            )
                            Action("Confirmar finalización", vm::finish)
                            Link("Volver", vm::back)
                        }
                        "review" -> {
                            Title("¿Cómo fue tu experiencia?")
                            Text(name(post?.assigned))
                            var stars by rememberSaveable { mutableIntStateOf(0) }
                            var text by rememberSaveable { mutableStateOf("") }
                            Row(Modifier.fillMaxWidth()) {
                                (1..5).forEach { n ->
                                    TextButton(
                                        onClick = { stars = n },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            if (n <= stars) "★" else "☆",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                    }
                                }
                            }
                            Text("$stars de 5 estrellas")
                            Field("Tu reseña", text, { text = it }, true)
                            Action("Publicar reseña") { vm.review(stars, text) }
                        }
                        "worker" -> {
                            Title("Comparte tu talento.")
                            Card {
                                Text(
                                    "${s.posts.count{it.assigned==me?.id&&it.status==PostStatus.FINISHED}} trabajos finalizados"
                                )
                                Text(
                                    "${s.posts.count{it.assigned==me?.id&&it.status==PostStatus.IN_PROGRESS}} trabajos activos"
                                )
                                Text("${s.offers.count{it.worker==me?.id}} ofertas enviadas")
                            }
                            Action("Buscar trabajos") { vm.go("home") }
                            Link("Mis ofertas") { vm.go("my-offers") }
                            Link("Mi perfil profesional") { me?.let { vm.profile(it.id) } }
                            s.posts
                                .filter { it.assigned == me?.id }
                                .forEach { p ->
                                    PostCard(p, name(p.author)) { vm.openPost(p.id, "tracking") }
                                }
                        }
                        "offer" -> {
                            Title("Presenta tu propuesta.")
                            Text(post?.title.orEmpty())
                            var price by rememberSaveable { mutableStateOf("") }
                            var text by rememberSaveable { mutableStateOf("") }
                            var whenText by rememberSaveable { mutableStateOf("") }
                            Field("Precio en COP", price, { price = it })
                            Field("Qué incluye", text, { text = it }, true)
                            Field("Disponibilidad", whenText, { whenText = it })
                            Action("Enviar oferta") { vm.offer(price, text, whenText) }
                        }
                        "my-offers" -> {
                            Title("Tus oportunidades.")
                            val offers = s.offers.filter { it.worker == me?.id }
                            if (offers.isEmpty()) Text("Todavía no has enviado ofertas.")
                            offers.forEach { o ->
                                val p = s.posts.find { it.id == o.post }
                                Card {
                                    Text(p?.title.orEmpty(), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(
                                        if (p != null && p.assigned == me?.id)
                                            "Aceptada · ${p.status.label}"
                                        else if (p?.assigned != null) "No seleccionada"
                                        else "Pendiente"
                                    )
                                    Action("Ver trabajo") { vm.openPost(o.post) }
                                }
                            }
                        }
                        "profile",
                        "cv" -> {
                            val person = s.users.find { it.id == u.profileId }
                            if (person != null) {
                                Title(person.name)
                                Text(person.profession.ifBlank { "Profesional de la comunidad" })
                                val reviews = s.reviews.filter { it.worker == person.id }
                                Card {
                                    Text(
                                        if (reviews.isEmpty()) "Aún sin calificaciones"
                                        else
                                            "%.1f ★ · %d reseñas"
                                                .format(
                                                    reviews.map { it.stars }.average(),
                                                    reviews.size,
                                                )
                                    )
                                    Text("${person.years} años de experiencia")
                                    Text(person.bio.ifBlank { "Sin presentación todavía." })
                                }
                                if (u.page == "profile") {
                                    Action("Ver currículum") { vm.go("cv") }
                                    Title("Lo que dice la comunidad")
                                    reviews.forEach { r ->
                                        Card {
                                            Text(name(r.author), fontWeight = FontWeight.Bold)
                                            Text("★".repeat(r.stars))
                                            Text(r.text)
                                        }
                                    }
                                } else {
                                    listOf(
                                            "Experiencia" to person.experience,
                                            "Formación" to person.training,
                                            "Habilidades" to person.skills,
                                        )
                                        .forEach { (label, value) ->
                                            Card {
                                                Text(label, fontWeight = FontWeight.Bold)
                                                Text(value.ifBlank { "Sin información" })
                                            }
                                        }
                                }
                                if (person.id == me?.id)
                                    Link("Editar perfil profesional") { vm.go("professional") }
                            }
                        }
                        "account" -> {
                            val accountPoints = s.points[me?.id] ?: 0
                            val initials = me?.name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.size(76.dp),
                                        shape = RoundedCornerShape(38.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initials, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(me?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text(me?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                                        Text("${reputationLevel(accountPoints)} · $accountPoints puntos", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                    TextButton(onClick = { vm.go("edit-account") }) { Text("Editar mis datos") }
                                }
                            }
                            Text("Gestiona tu cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            listOf(
                                    "Datos personales" to "edit-account",
                                    "Perfil profesional" to "professional",
                                    "Mis estadísticas" to "dashboard",
                                    "Logros" to "achievements",
                                    "Avisos" to "notifications",
                                )
                                .forEach { (label, page) -> Link(label) { vm.go(page) } }
                            Link("Ver mi perfil") { me?.let { vm.profile(it.id) } }
                            Link("Recuperar contraseña") { vm.go("recovery") }
                            if (me?.role == MemberRole.USER)
                                OutlinedButton(
                                    onClick = { vm.go("delete-account") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text("Eliminar mi cuenta", color = MaterialTheme.colorScheme.error)
                                }
                            Action("Cambiar usuario / salir", vm::logout)
                        }
                        "edit-account",
                        "professional" -> {
                            if (me != null) {
                                var m by remember { mutableStateOf(me) }
                                Title(
                                    if (u.page == "professional") "Tu experiencia abre puertas."
                                    else "Tus datos personales."
                                )
                                if (u.page == "professional") {
                                    Field(
                                        "Título profesional",
                                        m.profession,
                                        { m = m.copy(profession = it) },
                                    )
                                    Field("Acerca de ti", m.bio, { m = m.copy(bio = it) }, true)
                                    var years by rememberSaveable {
                                        mutableStateOf(m.years.toString())
                                    }
                                    Field(
                                        "Años de experiencia",
                                        years,
                                        {
                                            years = it
                                            m = m.copy(years = it.toIntOrNull() ?: -1)
                                        },
                                    )
                                    Field(
                                        "Experiencia laboral",
                                        m.experience,
                                        { m = m.copy(experience = it) },
                                        true,
                                    )
                                    Field("Formación", m.training, { m = m.copy(training = it) })
                                    Field("Habilidades", m.skills, { m = m.copy(skills = it) })
                                    Text(
                                        "Tu currículum se genera con estos datos; no es necesario subir un archivo en esta fase."
                                    )
                                } else {
                                    Field("Nombre completo", m.name, { m = m.copy(name = it) })
                                    Field("Correo electrónico", m.email, { m = m.copy(email = it) })
                                    Field(
                                        "Teléfono (opcional)",
                                        m.phone,
                                        { m = m.copy(phone = it) },
                                    )
                                    Choice("Ciudad", m.city, listOf("Bogotá", "Medellín", "Cali")) {
                                        m = m.copy(city = it)
                                    }
                                }
                                Action("Guardar cambios") { vm.saveMember(m) }
                            }
                        }
                        "delete-account" -> {
                            Title("¿Eliminar tu cuenta?")
                            Text(
                                "Se eliminarán tus publicaciones y datos asociados de la demostración. Esta acción no se puede deshacer durante esta ejecución."
                            )
                            var text by rememberSaveable { mutableStateOf("") }
                            Field("Escribe ELIMINAR", text, { text = it })
                            Action("Eliminar cuenta") { vm.deleteAccount(text) }
                            Link("Conservar mi cuenta") { vm.go("account") }
                        }
                        "notifications" -> {
                            val notices = s.notices.filter { it.recipient == me?.id }
                            Title("Tus avisos.")
                            Text("${notices.count{!it.read}} sin leer")
                            Link("Marcar todos como leídos", vm::markAll)
                            if (notices.isEmpty()) Text("No tienes avisos todavía.")
                            notices.forEach { n ->
                                Card {
                                    Text(n.title, fontWeight = FontWeight.Bold)
                                    Text(n.body)
                                    Text(
                                        if (n.read) "Leído" else "Nuevo",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    TextButton(onClick = { vm.mark(n.id, !n.read) }) {
                                        Text(if (n.read) "Marcar no leído" else "Marcar leído")
                                    }
                                    Action("Ver detalle") {
                                        vm.mark(n.id, true)
                                        if (n.post == null) vm.go("achievements")
                                        else if (s.posts.none { it.id == n.post })
                                            vm.message("La publicación fue eliminada.")
                                        else vm.openPost(n.post)
                                    }
                                }
                            }
                        }
                        "dashboard" -> {
                            val own = s.posts.filter { it.author == me?.id }
                            val completed = own.count { it.status == PostStatus.FINISHED }
                            val points = s.points[me?.id] ?: 0
                            Title("Tu actividad, de un vistazo")
                            Text("Consulta cómo van tus publicaciones y tus aportes a la comunidad.", style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(22.dp),
                            ) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("TUS PUBLICACIONES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(own.size.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                    Text(if (own.isEmpty()) "Tu actividad empieza con tu primera publicación." else "$completed de ${own.size} finalizadas", style = MaterialTheme.typography.titleMedium)
                                    if (own.isNotEmpty()) {
                                        LinearProgressIndicator(
                                            progress = { completed.toFloat() / own.size },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            trackColor = MaterialTheme.colorScheme.surface,
                                        )
                                    }
                                    Text("Resumen de las publicaciones disponibles en tu cuenta.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text("¿Cómo van tus trabajos?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val statuses = listOf(
                                Triple("Buscando trabajador", own.count { it.status == PostStatus.VERIFIED }, "Ya pueden recibir ofertas"),
                                Triple("En progreso", own.count { it.status == PostStatus.IN_PROGRESS }, "Con un trabajador asignado"),
                                Triple("Finalizados", completed, "Trabajos que ya cerraste"),
                                Triple("En revisión", own.count { it.status == PostStatus.PENDING }, "Esperan aprobación"),
                            )
                            statuses.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { (label, count, description) ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        ) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(count.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(description, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                            val rejected = own.count { it.status == PostStatus.REJECTED }
                            if (rejected > 0) Card {
                                Text("$rejected ${if (rejected == 1) "publicación necesita" else "publicaciones necesitan"} ajustes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Fueron rechazadas en la revisión. Consulta el motivo en Mis publicaciones y corrige los detalles.", style = MaterialTheme.typography.bodyMedium)
                            }
                            Action(if (own.isEmpty()) "Publicar mi primer trabajo" else "Ver mis publicaciones") { vm.go(if (own.isEmpty()) "create" else "mine") }
                            Text("Tu aporte a la comunidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Card {
                                listOf(
                                    "Comentarios que escribiste" to s.comments.count { it.author == me?.id },
                                    "Personas interesadas en tus publicaciones" to own.sumOf { it.interested.size },
                                ).forEach { (label, value) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Text(value.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("El interés se cuenta por publicación: una persona puede interesarse en varios trabajos.", style = MaterialTheme.typography.bodySmall)
                            }
                            Card {
                                Text("Tu reputación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${reputationLevel(points)} · $points puntos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("Descubre cuánto te falta para subir de nivel y conseguir nuevas insignias.", style = MaterialTheme.typography.bodyMedium)
                                Action("Ver mis logros") { vm.go("achievements") }
                            }
                        }
                        "achievements" -> {
                            val points = s.points[me?.id] ?: 0
                            val levels = listOf("Vecino" to 0, "Colaborador" to 100, "Experto" to 500, "Referente" to 1500)
                            val levelIndex = levels.indexOfLast { points >= it.second }.coerceAtLeast(0)
                            val current = levels[levelIndex]
                            val next = levels.getOrNull(levelIndex + 1)
                            Title("Tu participación cuenta")
                            Text("Ayuda a la comunidad, suma puntos y descubre tus logros.", style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("TU NIVEL ACTUAL", style = MaterialTheme.typography.labelMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(current.first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        Text("$points pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { if (next == null) 1f else ((points - current.second).toFloat() / (next.second - current.second)).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                    )
                                    Text(if (next == null) "¡Llegaste al nivel más alto! Gracias por aportar." else "Te faltan ${next.second - points} puntos para ser ${next.first}.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Text("Tus insignias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val firstCount = s.posts.count { it.author == me?.id }
                            val commentCount = s.comments.count { it.author == me?.id }
                            val verifiedCount = s.rewards.count { it.startsWith("${me?.id}:verify:") }
                            listOf(
                                Triple("first", "Tu primera publicación", Triple(firstCount, 1, "Publica un trabajo para empezar.")),
                                Triple("comments", "Conversaciones que ayudan", Triple(commentCount, 10, "Comparte 10 comentarios en los trabajos.")),
                                Triple("verified", "Publicaciones de confianza", Triple(verifiedCount, 10, "Consigue que el moderador apruebe 10 publicaciones.")),
                            ).forEach { (key, label, details) ->
                                val unlocked = "${me?.id}:badge:$key" in s.rewards
                                val count = if (unlocked) details.second else details.first.coerceAtMost(details.second)
                                Card {
                                    Text(if (unlocked) "✓ LOGRO CONSEGUIDO" else "EN CAMINO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(if (unlocked) "¡Lo lograste! Esta insignia ya es tuya." else details.third, style = MaterialTheme.typography.bodyMedium)
                                    if (!unlocked) {
                                        LinearProgressIndicator(progress = { count.toFloat() / details.second }, modifier = Modifier.fillMaxWidth())
                                        Text("$count de ${details.second} completados", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Text("¿Cómo gano puntos?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Card {
                                listOf("Publicar un trabajo" to "+10", "Tu publicación es aprobada" to "+20", "Escribir un comentario" to "+2", "Alguien marca Me interesa" to "+1").forEach { (label, value) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Text("$value pts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Cada acción cuenta una vez. Quitar y volver a marcar Me interesa no suma puntos adicionales.", style = MaterialTheme.typography.bodySmall)
                            }
                            var showLevels by rememberSaveable { mutableStateOf(false) }
                            TextButton(onClick = { showLevels = !showLevels }) { Text(if (showLevels) "Ocultar niveles" else "Conocer todos los niveles") }
                            if (showLevels) Card {
                                levels.forEachIndexed { index, (name, minimum) ->
                                    val maximum = levels.getOrNull(index + 1)?.second?.minus(1)
                                    Text("$name · ${if (maximum == null) "$minimum o más" else "$minimum–$maximum"} puntos", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        "moderation" -> {
                            Title("Cuidemos la comunidad.")
                            Text(
                                "${s.posts.count{it.status==PostStatus.PENDING}} pendientes · ${s.posts.count{it.status==PostStatus.VERIFIED}} verificadas"
                            )
                            var filter by rememberSaveable { mutableStateOf("Pendiente") }
                            Choice(
                                "Estado",
                                filter,
                                listOf("Todas") + PostStatus.entries.map { it.label },
                            ) {
                                filter = it
                            }
                            val list =
                                s.posts.filter { filter == "Todas" || it.status.label == filter }
                            if (list.isEmpty()) Text("No hay publicaciones en este estado.")
                            list.forEach { p ->
                                PostCard(p, name(p.author)) { vm.openPost(p.id, "mod-detail") }
                            }
                        }
                        "mod-detail" -> {
                            if (post != null) {
                                Title(post.title)
                                Text("${post.status.label} · ${name(post.author)}")
                                ExampleImage()
                                Text(post.description)
                                Text("${post.category} · ${post.city} · ${money(post.budget)}")
                                if (post.status == PostStatus.PENDING) {
                                    Action("Verificar publicación", vm::verify)
                                    Link("Rechazar con motivo") { vm.go("reject") }
                                }
                                if (post.status != PostStatus.FINISHED)
                                    Link("Marcar como finalizada") { vm.go("mod-finish") }
                            }
                        }
                        "reject" -> {
                            Title("Explica el motivo.")
                            Text(post?.title.orEmpty())
                            var reason by rememberSaveable { mutableStateOf("") }
                            Field("Motivo del rechazo", reason, { reason = it }, true)
                            Text(
                                "El autor recibirá el motivo. La publicación no será visible en el feed."
                            )
                            Action("Confirmar rechazo") { vm.reject(reason) }
                        }
                        "error" -> {
                            Title("No pudimos cargar la información.")
                            Text(
                                "Estado de error de referencia. Los datos de esta fase están en memoria y no necesitan conexión."
                            )
                            Action("Reintentar") { vm.go("home") }
                        }
                        else -> Text("Selecciona una opción del menú.")
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PostCard(p: WorkPost, author: String, open: () -> Unit) {
    Card {
        Text(
            "${p.category} · ${p.status.label}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(p.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(p.description, maxLines = 2)
        Text("${p.city} · ${money(p.budget)}")
        Text(
            "$author · ${p.interested.size} interesados",
            style = MaterialTheme.typography.bodySmall,
        )
        Action("Ver detalle", open)
    }
}
