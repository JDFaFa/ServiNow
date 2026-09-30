package com.example.servinow.features.explore
import org.junit.Assert.*
import org.junit.Test
class PublicationTest {
    private fun valid() = Publication(title="Reparar puerta",description="Ajustar bisagras",budget="50000",photos=listOf("content://photo"),latitude=4.6,longitude=-74.0)
    @Test fun stagesRequireTheirFields() {
        assertNotNull(publicationError(Publication(),1))
        assertNull(publicationError(Publication(title="Reparar"),1))
        assertNotNull(publicationError(valid().copy(photos=emptyList()),2))
        assertNotNull(publicationError(valid().copy(latitude=null),3))
        assertNull(publicationError(valid(),3))
    }
    @Test fun rejectsInvalidMoneyAndCoordinates() {
        assertNotNull(publicationError(valid().copy(budget="-1"),3))
        assertNotNull(publicationError(valid().copy(latitude=Double.NaN),3))
        assertNotNull(publicationError(valid().copy(longitude=190.0),3))
    }
}
