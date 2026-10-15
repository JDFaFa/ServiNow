package com.example.servinow.features.services
import com.example.servinow.domain.model.ServiceData
import com.example.servinow.domain.model.ServiceReview
import com.example.servinow.domain.validation.acceptProposal
import com.example.servinow.domain.validation.addReview
import com.example.servinow.domain.validation.finishService
import com.example.servinow.domain.validation.proposalError
import com.example.servinow.data.demo.demoOffers

import org.junit.Assert.*
import org.junit.Test
class ServiceTest {
    @Test fun selectionIsUniqueAndPreservesWorker() {
        val data=ServiceData(offers=demoOffers)
        val accepted=acceptProposal(data,"demo-l")
        assertEquals("demo-l",accepted.accepted["demo-paint"])
        assertEquals(accepted,acceptProposal(accepted,"demo-a"))
        assertEquals(data,acceptProposal(data,"missing"))
    }
    @Test fun cannotFinishUnassignedWork() {
        val data=ServiceData(offers=demoOffers)
        assertEquals(data,finishService(data,"demo-paint"))
        assertTrue("demo-paint" in finishService(acceptProposal(data,"demo-a"),"demo-paint").completed)
    }
    @Test fun reviewRequiresCompletionCorrectWorkerAndUniqueJob() {
        val assigned=acceptProposal(ServiceData(offers=demoOffers),"demo-l")
        val review=ServiceReview("demo-paint","laura",5,"Excelente trabajo")
        assertEquals(assigned,addReview(assigned,review))
        val done=finishService(assigned,"demo-paint")
        assertEquals(done,addReview(done,review.copy(worker="andres")))
        assertEquals(done,addReview(done,review.copy(stars=0)))
        assertEquals(done,addReview(done,review.copy(text=" ")))
        val rated=addReview(done,review)
        assertEquals(1,rated.reviews.size)
        assertEquals(rated,addReview(rated,review))
    }
    @Test fun offerRequiresValidPriceAndCompleteInformation() {
        assertNotNull(proposalError("-1","Servicio","Mañana","2 horas"))
        assertNotNull(proposalError("20000","","Mañana","2 horas"))
        assertNotNull(proposalError("999999999999999999999999","Servicio","Mañana","2 horas"))
        assertNull(proposalError("20000","Servicio","Mañana","2 horas"))
    }
}
