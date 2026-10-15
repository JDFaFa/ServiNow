package com.example.servinow.domain.repository
import com.example.servinow.domain.model.Publication
interface PublicationRepository {
 fun read(): List<Publication>
 fun save(posts: List<Publication>): Boolean
}
