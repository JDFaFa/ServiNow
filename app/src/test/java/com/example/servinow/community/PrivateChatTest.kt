package com.example.servinow.community
import com.example.servinow.data.repository.InMemoryCommunityRepository
import org.junit.Assert.*
import org.junit.Test
class PrivateChatTest {
 private fun login(r: InMemoryCommunityRepository, name: String) { assertNull(r.login(if (name == "laura") "laura@hotmail.com" else "$name@gmail.com","ServiNow1!")) }
 @Test fun chatRequiresAssignedWorkerAndValidText() {
  val r = InMemoryCommunityRepository(); login(r,"valentina")
  assertNotNull(r.sendMessage("p1","Hola"))
  assertNull(r.accept("o1"))
  assertNotNull(r.sendMessage("p1"," "))
  assertNotNull(r.sendMessage("p1","a".repeat(2001)))
  assertNull(r.sendMessage("p1","Hola, coordinemos el horario."))
  val sent = r.state.value.messages.single()
  assertEquals("valentina",sent.sender); assertEquals("andres",sent.recipient)
  assertTrue(r.state.value.notices.any { it.recipient == "andres" && it.title == "Nuevo mensaje" })
 }
 @Test fun thirdPartyCannotSendAndWorkerCanReply() {
  val r=InMemoryCommunityRepository(); login(r,"valentina"); assertNull(r.accept("o1"))
  login(r,"laura"); assertNotNull(r.sendMessage("p1","No autorizado")); assertTrue(r.state.value.messages.isEmpty())
  login(r,"andres"); assertNull(r.sendMessage("p1","Mañana a las 9.")); assertEquals("valentina",r.state.value.messages.single().recipient)
  r.logout(); assertNotNull(r.sendMessage("p1","Sin sesión"))
 }
}
