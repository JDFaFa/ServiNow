package com.example.servinow.features.recovery

import org.junit.Assert.*
import org.junit.Test

class RecoveryViewModelTest {
    @Test fun recoveryFollowsAllThreeSteps() {
        val vm = RecoveryViewModel()
        vm.previewLink()
        assertEquals(RecoveryStep.EMAIL, vm.uiState.value.step)
        vm.emailChanged("persona@gmail.com")
        vm.submit()
        assertEquals(RecoveryStep.SENT, vm.uiState.value.step)
        vm.resend()
        assertTrue(vm.uiState.value.message!!.contains("no se envían"))
        vm.dismissMessage()
        vm.previewLink()
        assertEquals(RecoveryStep.NEW_PASSWORD, vm.uiState.value.step)
        vm.back()
        assertEquals(RecoveryStep.SENT, vm.uiState.value.step)
        vm.restart()
        assertEquals(RecoveryStep.EMAIL, vm.uiState.value.step)
    }
    @Test fun emptyEmailShowsDialog() {
        val vm = RecoveryViewModel()
        vm.submit()
        assertEquals("Escribe tu correo electrónico", vm.uiState.value.message)
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }
    @Test fun rejectsUnsupportedProvider() {
        val vm = RecoveryViewModel()
        vm.emailChanged("persona@ejemplo.com")
        vm.submit()
        assertTrue(vm.uiState.value.message!!.contains("Solo se admiten"))
    }
    @Test fun validEmailDoesNotClaimDelivery() {
        val vm = RecoveryViewModel()
        vm.emailChanged(" persona@gmail.com ")
        assertNull(vm.uiState.value.message)
        vm.submit()
        assertEquals("persona@gmail.com", vm.uiState.value.email)
        assertNull(vm.uiState.value.message)
        assertEquals(RecoveryStep.SENT, vm.uiState.value.step)
    }
}
