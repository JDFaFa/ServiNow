# Arquitectura de ServiNow

La aplicación usa Kotlin, Jetpack Compose, MVVM, repositorios e inyección con Hilt/KSP.
La guía de clase es https://caflorezvi.github.io/guias-apps-moviles/12.view-model-repos.html.

## Recorrido activo de la segunda fase

`MainActivity` → `CommunityRoute` → `CommunityViewModel` → `CommunityRepository` →
`InMemoryCommunityRepository`. Hilt comparte una instancia del repositorio; un
`StateFlow<CommunityState>` mantiene relacionadas las publicaciones, usuarios,
ofertas, comentarios, reseñas, avisos y puntos. El ViewModel traduce eventos de UI
en operaciones y conserva la navegación. El repositorio valida rol, propiedad y
estado antes de modificar los datos. Los modelos del recorrido están en `Community.kt`.

Las cuentas, el registro y el restablecimiento de contraseñas son funcionales dentro
de la demostración en memoria; no representan OAuth, correo remoto ni sesiones
persistentes. Consultar `FASE_2.md` para credenciales, pruebas y límites.

Las implementaciones `Local*Repository` y las pantallas previas se conservan como
trabajo de etapas anteriores. No son la fuente de datos del recorrido de esta
entrega. Las secciones siguientes describen esa base y su posible reutilización.

## Estructura

```
com.example.servinow/
  ServiNowApplication.kt       Inicializa Hilt
  MainActivity.kt              Aloja Compose y coordina las rutas principales
  core/                       Tema, componentes y utilidades compartidas
  domain/
    model/                    Publicación, cuenta, oferta, reseña, aviso y filtros
    validation/               Validaciones y reglas puras de servicios y búsqueda
    repository/               Contratos de acceso a datos
  data/
    repository/               Implementaciones locales con SharedPreferences
    local/                    Conversión entre datos de servicios y JSON
    demo/                     Catálogo y ofertas de demostración
  di/RepositoryModule.kt      Vincula interfaces con implementaciones
  features/                   Pantallas, estados de UI y ViewModels por función
```

## Flujo de una acción

Pantalla Compose → ViewModel → interfaz de repositorio → implementación local.
El resultado actualiza el StateFlow observado por Compose. Los ViewModels no abren
SharedPreferences ni convierten JSON. Las pantallas no acceden a implementaciones
de repositorios. Dominio no depende de Android, Compose, Hilt ni de la capa de datos.

Ejemplo: `PublicationViewModel.save()` valida la publicación y llama a
`PublicationRepository.save()`. `LocalPublicationRepository` guarda su JSON. Solo
si la escritura funciona, el ViewModel actualiza las publicaciones de la pantalla.

## Inyección

`@HiltAndroidApp` inicializa el contenedor en `ServiNowApplication`.
`@AndroidEntryPoint` integra `MainActivity`. Cada `@HiltViewModel` recibe sus
interfaces por constructor con `@Inject`. `RepositoryModule` usa `@Binds` para
seleccionar implementaciones locales, compartidas con alcance `@Singleton`.
La actividad obtiene ViewModels con `ViewModelProvider`, cuyo factory está
integrado por Hilt. Se mantiene su alcance de actividad porque la navegación
actual aún no utiliza Navigation Compose; no hace falta `hiltViewModel()` aquí.

## Persistencia y compatibilidad

Se conservan archivos, claves y formatos existentes: `publications_local`,
`services_local`, `explore_local`, `account_local`. No hay migración destructiva.
`AccountRepository` expone los flujos compartidos de perfil, avisos y puntos.
Los demás repositorios leen/escriben instantáneas; sus ViewModels mantienen el
estado de pantalla. `LocalDataRepository` centraliza el borrado explícito solicitado
desde la cuenta y recarga el estado de cuenta para no conservar datos en memoria.

## Alcance actual

Autenticación, registro y recuperación siguen siendo formularios de demostración:
no se añadieron cuentas reales ni se guardan contraseñas como texto plano.
Los ViewModels de esos formularios gestionan sus validaciones sin fingir un backend.
El almacenamiento sigue siendo local y síncrono; al incorporar Room o un servicio
remoto, los contratos de acceso deberán pasar a operaciones suspendidas y estados
de carga/error. La moderación no representa autorización real de servidor.
El mapa WebView/Leaflet y la selección de documentos continúan en presentación
porque requieren interacción con Android. Los URI y coordenadas son datos de dominio.

## Verificación

`./gradlew :app:assembleDebug :app:testDebugUnitTest`

Las pruebas de reglas existentes se conservan. `RepositoryBoundaryTest` comprueba
recuperación de datos al recrear el ViewModel, errores de escritura sin mutar el
estado visible y protección contra sobrescribir servicios que no se pudieron leer.
Los dobles de repositorio permiten probar ViewModels sin almacenamiento Android.
