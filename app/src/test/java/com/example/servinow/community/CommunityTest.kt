package com.example.servinow.community

import com.example.servinow.data.repository.InMemoryCommunityRepository
import com.example.servinow.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CommunityTest {
    private fun repo() = InMemoryCommunityRepository()

    private fun login(r: InMemoryCommunityRepository, user: String = "valentina") {
        assertNull(
            r.login(
                "$user@gmail.com",
                if (user == "moderador") "Moderador1!" else "ServiNow1!",
                user == "moderador",
            )
        )
    }

    @Test
    fun registrationLoginDuplicateAndPasswordRules() {
        val r = repo()
        assertNotNull(r.register("Nuevo", "nuevo@gmail.com", "123", "123"))
        assertNull(r.register("Nuevo Usuario", "nuevo@gmail.com", "Nuevo123!", "Nuevo123!"))
        assertNotNull(r.register("Otro", "NUEVO@gmail.com", "Nuevo123!", "Nuevo123!"))
        assertNotNull(r.login("nuevo@gmail.com", "Incorrecta1!"))
        assertNull(r.login("nuevo@gmail.com", "Nuevo123!"))
        assertEquals("Nuevo Usuario", r.state.value.me?.name)
    }

    @Test
    fun recoveryChangesPasswordAndConsumesRequest() {
        val r = repo()
        assertNotNull(r.resetPassword("valentina@gmail.com", "Nueva123!", "Nueva123!"))
        assertNull(r.requestRecovery("valentina@gmail.com"))
        assertNull(r.resetPassword("valentina@gmail.com", "Nueva123!", "Nueva123!"))
        assertNotNull(r.login("valentina@gmail.com", "ServiNow1!"))
        assertNull(r.login("valentina@gmail.com", "Nueva123!"))
        assertNotNull(r.resetPassword("valentina@gmail.com", "Otra123!", "Otra123!"))
    }

    @Test
    fun moderatorRoleCannotBeChosenByOrdinaryUser() {
        val r = repo()
        assertNotNull(r.login("valentina@gmail.com", "ServiNow1!", true))
        assertNull(r.state.value.me)
        login(r)
        assertNotNull(r.verify("p2"))
        assertNotNull(
            r.updateMember(r.state.value.me!!.copy(role = MemberRole.MODERATOR, email = "bad"))
        )
        assertNull(r.updateMember(r.state.value.me!!.copy(role = MemberRole.MODERATOR)))
        assertEquals(MemberRole.USER, r.state.value.me!!.role)
    }

    @Test
    fun moderationChangesVisibilityAndNotifiesAuthor() {
        val r = repo()
        login(r, "moderador")
        assertNotNull(r.reject("p2", ""))
        assertNull(r.reject("p2", "Describe mejor el trabajo solicitado."))
        assertEquals(PostStatus.REJECTED, r.state.value.posts.first { it.id == "p2" }.status)
        assertTrue(r.state.value.notices.any { it.post == "p2" && it.recipient == "valentina" })
        login(r)
        val p = r.state.value.posts.first { it.id == "p2" }
        assertNull(r.savePost(p.copy(description = "Descripción corregida para evaluación.")))
        login(r, "moderador")
        assertNull(r.verify("p2"))
        assertEquals(PostStatus.VERIFIED, r.state.value.posts.first { it.id == "p2" }.status)
    }

    @Test
    fun commentsGenerateOwnedNotificationsAndPoints() {
        val r = repo()
        login(r, "andres")
        val before = r.state.value.points["andres"]!!
        assertNull(r.comment("p1", "Puedo revisar la conexión mañana."))
        val n = r.state.value.notices.first()
        assertEquals("valentina", n.recipient)
        assertEquals(before + 2, r.state.value.points["andres"])
        r.markNotice(n.id, true)
        assertFalse(r.state.value.notices.first().read)
        login(r)
        r.markNotice(n.id, true)
        assertTrue(r.state.value.notices.first().read)
    }

    @Test
    fun togglingVotesDoesNotFarmPoints() {
        val r = repo()
        login(r, "andres")
        val before = r.state.value.points["valentina"]!!
        repeat(3) { assertNull(r.vote("p1")) }
        assertEquals(before + 1, r.state.value.points["valentina"])
        assertEquals(setOf("andres"), r.state.value.posts.first { it.id == "p1" }.interested)
    }

    @Test
    fun offerAcceptanceAndReviewAreRestricted() {
        val r = repo()
        login(r, "andres")
        assertNotNull(r.offer("p1", "100000", "Nueva propuesta", "Mañana"))
        assertNull(r.offer("p3", "90000", "Ajuste de bisagras y puertas", "Mañana"))
        assertNotNull(r.accept("o1"))
        login(r)
        assertNull(r.accept("o1"))
        assertNotNull(r.accept("o2"))
        assertNotNull(r.review("p1", 5, "Bien"))
        assertNull(r.finish("p1"))
        assertNull(r.review("p1", 5, "Muy buen trabajo"))
        assertNotNull(r.review("p1", 4, "Otra reseña"))
        assertEquals(1, r.state.value.reviews.count { it.post == "p1" })
    }

    @Test
    fun deletionRequiresOwnerAndConfirmation() {
        val r = repo()
        login(r, "andres")
        assertNotNull(r.deletePost("p1"))
        assertNotNull(r.deleteAccount("si"))
        assertNull(r.deleteAccount("ELIMINAR"))
        assertNull(r.state.value.me)
        assertFalse(r.state.value.users.any { it.id == "andres" })
        assertFalse(r.state.value.offers.any { it.worker == "andres" })
        assertNotNull(r.login("andres@gmail.com", "ServiNow1!"))
    }

    @Test
    fun newPostIsPendingAndAwardsOnlyOnce() {
        val r = repo()
        login(r)
        val p = r.state.value.posts.first().copy(id = "new", title = "Nuevo trabajo")
        val points = r.state.value.points["valentina"]!!
        assertNull(r.savePost(p))
        assertEquals(PostStatus.PENDING, r.state.value.posts.last().status)
        assertNull(r.savePost(p.copy(description = "Descripción modificada de la solicitud.")))
        assertEquals(points + 10, r.state.value.points["valentina"])
        assertTrue("valentina:badge:first" in r.state.value.rewards)
    }
}
