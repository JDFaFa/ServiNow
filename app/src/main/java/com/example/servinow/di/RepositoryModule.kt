package com.example.servinow.di
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.servinow.domain.repository.*
import com.example.servinow.data.repository.*
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
 @Binds @Singleton abstract fun remoteCommunity(impl:com.example.servinow.data.cloud.FirestoreCommunityRepository):RemoteCommunityRepository
 @Binds @Singleton abstract fun authentication(impl:com.example.servinow.data.cloud.FirebaseAuthenticationRepository):AuthenticationRepository
 @Binds abstract fun geocoding(impl:com.example.servinow.data.repository.remote.MapboxGeocodingRepository):GeocodingRepository
 @Binds @Singleton abstract fun sessions(impl:com.example.servinow.data.repository.local.SessionRepositoryImpl):SessionRepository
 @Binds @Singleton abstract fun community(impl:InMemoryCommunityRepository):CommunityRepository
 @Binds abstract fun localData(impl:LocalDataRepositoryImpl):LocalDataRepository
 @Binds @Singleton abstract fun publications(impl:LocalPublicationRepository):PublicationRepository
 @Binds @Singleton abstract fun services(impl:LocalServiceRepository):ServiceRepository
 @Binds @Singleton abstract fun explore(impl:LocalExploreRepository):ExploreRepository
 @Binds @Singleton abstract fun account(impl:LocalAccountRepository):AccountRepository
}
