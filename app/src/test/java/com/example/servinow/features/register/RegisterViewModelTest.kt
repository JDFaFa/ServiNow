package com.example.servinow.features.register

import org.junit.Assert.*
import org.junit.Test

class RegisterViewModelTest {
    private fun validForm() = RegisterViewModel().apply {
        nameChanged("Laura Gómez")
        emailChanged("laura@gmail.com")
        passwordChanged("Segura12!")
        confirmationChanged("Segura12!")
    }
    @Test fun rejectsMissingName() {
        val vm = validForm().apply { nameChanged("  "); submit() }
        assertEquals("Escribe tu nombre completo.", vm.uiState.value.message)
    }
    @Test fun rejectsUnknownProvider() {
        val vm = validForm().apply { emailChanged("laura@ejemplo.com"); submit() }
        assertTrue(vm.uiState.value.message!!.contains("Solo se admiten"))
    }
    @Test fun passwordRulesOnlyAppearAfterSubmission() {
        val vm = validForm().apply { passwordChanged("abc") }
        assertNull(vm.uiState.value.message)
        vm.submit()
        assertTrue(vm.uiState.value.message!!.contains("mínimo 8"))
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }
    @Test fun rejectsMismatchedConfirmation() {
        val vm = validForm().apply { confirmationChanged("Distinta1!"); submit() }
        assertEquals("Las contraseñas no coinciden.", vm.uiState.value.message)
    }
    @Test fun locationMustBeSelectedAndValid() {
        val vm = validForm()
        vm.confirmLocation()
        assertFalse(vm.uiState.value.locationConfirmed)
        vm.selectLocation(Double.NaN, 0.0)
        assertNull(vm.uiState.value.latitude)
        vm.selectLocation(4.711, -74.072)
        vm.confirmLocation()
        assertTrue(vm.uiState.value.locationConfirmed)
        vm.backToForm()
        assertEquals(4.711, vm.uiState.value.latitude!!, 0.00001)
        vm.selectLocation(5.0, -74.0)
        assertFalse(vm.uiState.value.locationConfirmed)
    }
    @Test fun validFormDoesNotClaimAccountWasCreated() {
        val vm = validForm().apply { submit() }
        assertTrue(vm.uiState.value.choosingLocation)
        assertNull(vm.uiState.value.message)
    }
}
