package com.example.servinow.features.explore
import org.junit.Assert.*
import org.junit.Test
class ExploreTest {
    @Test fun interestTogglesWithoutDuplicateVotes() {
        val vm = ExploreViewModel()
        vm.openJob(1)
        vm.toggleInterest()
        assertEquals(13, vm.state.value.jobs.first().votes)
        vm.toggleInterest()
        assertEquals(12, vm.state.value.jobs.first().votes)
    }
    @Test fun commentsAreScopedAndEmptyCommentsRejected() {
        val vm = ExploreViewModel()
        vm.openJob(1)
        vm.comment("  ")
        vm.postComment()
        assertTrue(vm.state.value.comments.isEmpty())
        vm.comment("Consulta de prueba")
        vm.postComment()
        assertEquals(listOf("Consulta de prueba"), vm.state.value.comments[1])
        assertEquals("", vm.state.value.commentDraft)
        vm.page("map")
        vm.openJob(2)
        assertNull(vm.state.value.comments[2])
        vm.back()
        assertEquals("map", vm.state.value.page)
    }

    @Test fun categoryAndSearchCombine() {
        assertEquals(listOf(2), filterJobs(exampleJobs, "lámparas", SearchFilters(category = "Electricidad")).map { it.id })
        assertTrue(filterJobs(exampleJobs, "lámparas", SearchFilters(category = "Plomería")).isEmpty())
    }
    @Test fun radiusAndClosedJobsAreExcluded() {
        assertTrue(filterJobs(exampleJobs, "", SearchFilters(latitude = 0.0, longitude = 0.0)).isEmpty())
        assertTrue(filterJobs(listOf(exampleJobs.first().copy(open = false)), "", SearchFilters()).isEmpty())
    }
    @Test fun draftOnlyAppliesOnConfirmation() {
        val vm = ExploreViewModel()
        vm.editFilters()
        vm.draft(SearchFilters(category = "Plomería"))
        assertEquals(3, vm.state.value.results.size)
        vm.apply()
        assertEquals(1, vm.state.value.results.size)
        vm.editFilters()
        vm.reset()
        vm.apply()
        assertEquals(3, vm.state.value.results.size)
    }
    @Test fun distanceSorting() {
        val f = SearchFilters(order = "Menor distancia")
        val distances = filterJobs(exampleJobs, "", f).map { distanceKm(it, f) }
        assertEquals(distances.sorted(), distances)
    }
}
