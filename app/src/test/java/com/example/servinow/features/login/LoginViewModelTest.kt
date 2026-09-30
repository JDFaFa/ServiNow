package com.example.servinow.features.login

import com.example.servinow.features.register.RegistrationPasswordValidator
import org.junit.Assert.*
import org.junit.Test

class LoginViewModelTest {
    @Test fun registrationEnforcesComplexity() {
        for (password in listOf("Aa1!abc", "abcdefgh1!", "Abcdefgh!", "Abcdefg12", "Abcdefg1 ")) {
            assertNotNull(RegistrationPasswordValidator.error(password))
        }
        assertNull(RegistrationPasswordValidator.error("Abcdef1!"))
    }
    @Test fun loginDoesNotEnforceRegistrationRules() {
        val vm = LoginViewModel()
        vm.onEmailChange("persona@gmail.com")
        vm.onPasswordChange("simple")
        vm.login()
        assertNull(vm.uiState.value.passwordError)
        assertTrue(vm.uiState.value.message!!.contains("Falta conectar"))
    }
    @Test fun unknownProviderProducesDialog() {
        val vm = LoginViewModel()
        vm.onEmailChange("persona@empresa.com")
        vm.onPasswordChange("simple")
        vm.login()
        assertTrue(vm.uiState.value.message!!.contains("Solo se admiten"))
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }
    @Test fun missingFieldsProduceDialogMessage() {
        val vm = LoginViewModel()
        vm.login()
        assertEquals("Escribe tu correo electrónico", vm.uiState.value.message)
    }
    @Test fun rejectedCredentialsProduceDismissibleDialog() {
        val vm = LoginViewModel()
        vm.onCredentialsRejected()
        assertTrue(vm.uiState.value.message!!.contains("contraseña es incorrecta"))
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }
}
