package com.example.servinow.architecture

import com.example.servinow.domain.model.Professional
import com.example.servinow.domain.model.Publication
import com.example.servinow.domain.model.ServiceData
import com.example.servinow.domain.repository.*
import com.example.servinow.features.explore.PublicationViewModel
import com.example.servinow.features.services.ServiceViewModel
import org.junit.Assert.*
import org.junit.Test

class RepositoryBoundaryTest {
    private class Publications : PublicationRepository {
        var stored = emptyList<Publication>()
        var fail = false
        override fun read() = stored
        override fun save(posts: List<Publication>): Boolean {
            if (fail) return false
            stored = posts
            return true
        }
    }

    private fun validPost() = Publication(title = "Reparar puerta", description = "Ajustar bisagras",
        budget = "50000", photos = listOf("content://test/photo"), latitude = 4.6, longitude = -74.0)

    @Test fun publicationSurvivesViewModelRecreationThroughRepository() {
        val repository = Publications()
        val first = PublicationViewModel(repository)
        first.change(validPost())
        assertTrue(first.save())
        val second = PublicationViewModel(repository)
        assertEquals(first.state.value.posts, second.state.value.posts)
        assertEquals("Pendiente", second.state.value.posts.single().status)
    }

    @Test fun failedWriteDoesNotPublishOrDeleteUiData() {
        val repository = Publications()
        val vm = PublicationViewModel(repository)
        vm.change(validPost())
        assertTrue(vm.save())
        val original = vm.state.value.posts
        repository.fail = true
        vm.change(validPost())
        assertFalse(vm.save())
        vm.delete(original.single())
        assertEquals(original, vm.state.value.posts)
        assertEquals(original, repository.stored)
        assertNotNull(vm.state.value.message)
    }

    @Test fun unreadableServiceRepositoryCannotBeOverwritten() {
        var writes = 0
        val repository = object : ServiceRepository {
            override fun professionals() = emptyList<Professional>()
            override fun read(): Result<ServiceData> = Result.failure(IllegalStateException("Invalid local data"))
            override fun save(data: ServiceData): Boolean { writes++; return true }
        }
        val vm = ServiceViewModel(repository)
        vm.forJob("job-1", "Reparación")
        vm.form(price = "50000", message = "Mano de obra", availability = "Mañana", duration = "2 horas")
        assertFalse(vm.send())
        assertEquals(0, writes)
        assertNotNull(vm.state.value.error)
    }
}
