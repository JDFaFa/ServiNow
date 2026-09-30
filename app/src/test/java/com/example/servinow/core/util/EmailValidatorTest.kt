package com.example.servinow.core.util

import org.junit.Assert.*
import org.junit.Test

class EmailValidatorTest {
    @Test fun acceptsKnownProviders() {
        for (domain in listOf("gmail.com", "hotmail.com", "outlook.com", "yahoo.com")) {
            assertNull(EmailValidator.error("persona@$domain"))
        }
        assertNull(EmailValidator.error("  Persona+trabajo@GMAIL.COM  "))
    }
    @Test fun rejectsMisspellingsAndDomainImpersonation() {
        for (value in listOf("a@gmial.com", "a@gmail.com.ejemplo.com", "a@sub.gmail.com", "a@empresa.com", "a@yahoo", "a@GMAIL.COMx")) {
            assertNotNull(value, EmailValidator.error(value))
        }
    }
    @Test fun rejectsMalformedAddresses() {
        for (value in listOf("", "gmail.com", "@gmail.com", "a@@gmail.com", "a b@gmail.com", ".a@gmail.com", "a.@gmail.com", "a..b@gmail.com")) {
            assertNotNull(value, EmailValidator.error(value))
        }
    }
}
