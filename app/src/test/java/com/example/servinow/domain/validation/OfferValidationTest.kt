package com.example.servinow.domain.validation
import org.junit.Assert.*
import org.junit.Test
class OfferValidationTest {
 @Test fun acceptsValidOffer() { assertNull(offerInputError("60000","Reparar el lavamanos","Mañana")) }
 @Test fun explainsShortDescription() { assertEquals("OFFER_DESCRIPTION_SHORT",offerInputError("60000","todo","si")) }
 @Test fun rejectsMissingAvailability() { assertEquals("OFFER_AVAILABILITY_INVALID",offerInputError("60000","Reparación completa","  ")) }
 @Test fun validatesServerPriceLimits() { for (p in listOf("0","-1","60.000","1000000001","")) assertEquals("OFFER_PRICE_INVALID",offerInputError(p,"Reparación completa","Hoy")) }
}
