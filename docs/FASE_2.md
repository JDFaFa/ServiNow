# ServiNow - segunda fase

Integrantes: Santiago Dorado, Juan David Saza y Fabián López.

## Ejecutar

Abrir este proyecto en Android Studio y ejecutar `app`.
La entrada activa es `MainActivity` → `CommunityRoute` → `ServiNowNavHost`.
Navigation Compose gestiona los destinos y la pila de regreso; `hiltViewModel()`
proporciona el `CommunityViewModel` compartido desde Compose.
Hilt inyecta `CommunityRepository`, implementado por `InMemoryCommunityRepository`.
Sus modelos están en `domain/model/Community.kt`. La UI observa un StateFlow único.

Los datos se conservan durante el proceso y al rotar el dispositivo; se reinician
al cerrar el proceso. No se usan sesiones persistentes, permisos ni servicios de
autenticación. Las implementaciones locales de la etapa anterior se conservan en
el proyecto, pero no son la fuente de datos del recorrido de esta entrega.

## Cuentas de evaluación

| Cuenta | Clave | Uso |
|---|---|---|
| valentina@gmail.com | ServiNow1! | Solicitar, editar y finalizar trabajos |
| andres@gmail.com | ServiNow1! | Ofertar, comentar y consultar asignaciones |
| laura@hotmail.com | ServiNow1! | Otro usuario de la comunidad |
| moderador@gmail.com | Moderador1! | Verificar, rechazar y finalizar |

Estas claves son públicas de ejemplo y no corresponden a cuentas reales.
El registro agrega usuarios en memoria. La recuperación actualiza la contraseña
mediante un enlace explícitamente simulado. El ejemplo calcula un resumen SHA-256
en memoria para comparar claves: no es una solución de autenticación de producción.
No se implementó OAuth de Google; el botón informa que requiere el proveedor externo.

## Recorrido sugerido

1. Entrar como Valentina, crear una publicación con imagen de ejemplo y verla pendiente.
2. Salir, entrar como moderador y verificarla. Probar también un rechazo con motivo.
3. Entrar como Andrés: verla en el feed, enviar oferta, comentar y marcar interés.
4. Entrar como Valentina: consultar avisos, aceptar la oferta, finalizar y reseñar.
5. Revisar el perfil/CV del trabajador, estadísticas, puntos y logros.
6. Probar el registro con un correo nuevo y luego su login; cambiar su contraseña.
7. Eliminar la publicación o la cuenta creada para probar las confirmaciones.

La carga externa de imágenes se sustituye por una ilustración incluida con la app.
La publicación permite elegir una ciudad de referencia sin permisos. El mapa de
búsqueda existente se mantiene como función opcional y requiere internet para sus
mosaicos; el resto del recorrido se puede evaluar sin conexión.

## Verificar

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.servinow.DeliveryEvidenceTest
```

Las pruebas unitarias nuevas validan: alta y duplicados, contraseña y recuperación,
roles y propiedad, revisión y rechazo, avisos al autor, puntos de votos, ofertas,
reseñas únicas, eliminación y estado inicial pendiente. La prueba instrumentada
opera formularios de registro/login/reseña y recorre las demás pantallas mediante
el mismo ViewModel de la interfaz. Las capturas provienen del emulador, no de HTML.

## Responsabilidad del grupo

Leer los repositorios, validaciones y ViewModel, ejecutar el recorrido y explicar
las decisiones. Las pruebas automatizadas no sustituyen la revisión de los tres
integrantes. No se afirma que una persona haya revisado código si no lo ha hecho.
Consultar el PDF para el registro del apoyo de IA, dificultades y correcciones.

## Documento de entrega

[Descargar PDF de la segunda fase](entrega/ServiNow-entrega-fase-2.pdf)

## Identidad y bienvenida

El ícono adaptable usa un símbolo vectorial propio de casa y rayo, con fondo azul.
Android 12 y posteriores muestran el logo durante el arranque. La primera pantalla
es `welcome`, con acceso a `onboarding` (tres pasos), registro o login.
Se puede omitir la guía; Anterior y el botón Atrás permiten recorrer los pasos.
El paso actual sobrevive a la recreación de la Activity con `rememberSaveable`.
La bienvenida se muestra en cada nuevo inicio del proceso: esta fase no guarda
una preferencia persistente de onboarding completado.
