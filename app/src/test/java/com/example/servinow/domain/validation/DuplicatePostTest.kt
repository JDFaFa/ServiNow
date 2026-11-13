package com.example.servinow.domain.validation
import com.example.servinow.domain.model.*
import org.junit.Assert.*
import org.junit.Test
class DuplicatePostTest {
    private val old = WorkPost("old", "author", "Reparación de tubería", "Plomería", "La llave pierde agua.", "Armenia", 4.5, -75.6, 50000)
    @Test fun detectsFormattingVariations() {
        assertTrue(isDuplicatePost(old.copy(id="new",title=" REPARACION de tuberia! ",description="La   llave pierde agua",budget=60000),old))
    }
    @Test fun allowsEditingSamePostAndOtherAuthors() {
        assertFalse(isDuplicatePost(old,old))
        assertFalse(isDuplicatePost(old.copy(id="new",author="other"),old))
    }
    @Test fun allowsNewWorkAndDifferentContent() {
        assertFalse(isDuplicatePost(old.copy(id="new"),old.copy(status=PostStatus.FINISHED)))
        assertFalse(isDuplicatePost(old.copy(id="new",description="La ducha no tiene agua."),old))
        assertFalse(isDuplicatePost(old.copy(id="new",city="Pereira"),old))
    }
    @Test fun blocksPendingApprovedRejectedAndAssignedDuplicates() {
        for(status in listOf(PostStatus.PENDING,PostStatus.VERIFIED,PostStatus.REJECTED,PostStatus.IN_PROGRESS))
            assertTrue(isDuplicatePost(old.copy(id="new"),old.copy(status=status)))
    }
}
