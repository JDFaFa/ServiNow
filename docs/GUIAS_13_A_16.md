# Integración de las guías 13 a 16

## Estado

- Textos del flujo activo de CommunityRoute y bienvenida en inglés (`values`) y español (`values-es`). Se traducen validaciones y etiquetas de dominio en la capa de presentación; los textos escritos por usuarios y los datos de ejemplo conservan su idioma.
- DataStore almacena ID, rol, preferencia «Recordar mi sesión» y bienvenida completada. No almacena contraseñas. La restauración valida que ID y rol correspondan a una cuenta existente. Una cuenta nueva en memoria no sobrevive al cierre del proceso; su sesión se descarta al no encontrarla. Las cuentas precargadas sí pueden restaurarse. No es autenticación remota.
- Los estados de las pantallas activas se observan con `collectAsStateWithLifecycle`.
- Foto de perfil y del trabajo mediante galería o cámara. La cámara usa permiso de ejecución y FileProvider. La galería solo entrega el archivo elegido, sin permiso de acceso completo. La copia se conserva en almacenamiento privado y se valida su formato y tamaño máximo (20 MB).
- Mapbox 11.22.0 con bibliotecas ndk27: componente `ServiceMap`, marcadores por categoría y agrupación, selección de punto, detalle sin gestos y ubicación actual con permiso preciso o aproximado. Los listeners se retiran al dejar el mapa y los permisos se revisan al reanudar.
- Pantallas conectadas: búsqueda con tarjeta del trabajo seleccionado, ubicación del trabajo, detalle y datos personales.
- Geocodificación inversa preparada en `MapboxGeocodingRepository`. Se consulta desde el ViewModel, con cancelación de solicitudes anteriores, espera breve y límites de tiempo. Sin respuesta se conservan las coordenadas. La dirección temporal no se guarda en publicaciones ni en disco.

## Activar Mapbox

En `local.properties`, que está excluido de Git, agregar:

```properties
MAPBOX_ACCESS_TOKEN=pk.TU_TOKEN_PUBLICO
```

Sincronizar y recompilar. Nunca colocar un token secreto `sk.` en la aplicación. Cada integrante configura su propia copia local. Sin token se usa OpenStreetMap con el botón «Mi ubicación» conectado al GPS/red de Android. Las direcciones se consultan mediante Geocoder del dispositivo. El token solo es necesario para activar el proveedor Mapbox.

La integración compila sin token. No se ha verificado una conexión autenticada a Mapbox ni resultados reales de su geocodificación: falta configurar la cuenta del equipo. La API usa resultados temporales únicamente para mostrarlos; guardarlos permanentemente requiere condiciones adicionales del servicio.

## Imágenes y datos

No se configuró almacenamiento remoto. Seleccionar una foto no equivale a subirla a Firebase o Cloudinary. Los archivos quedan privados en el dispositivo; sus asociaciones con publicaciones y perfiles siguen el repositorio en memoria. Las preferencias y fotos no se transfieren por copia de seguridad.

## Corrutinas

- `viewModelScope`: lectura de preferencias, guardar/cerrar sesión y consulta de dirección; se cancela al destruir el ViewModel.
- `rememberCoroutineScope`: copiar la foto elegida; se cancela al salir del selector.
- `LaunchedEffect`: sincronizar coordenadas y consultar dirección cuando cambia el punto; se reinicia al cambiar sus claves.
- `MapEffect`: registrar el listener de posición de Mapbox; se retira en `finally` al cancelarse.

## Fuentes

- https://caflorezvi.github.io/guias-apps-moviles/13.data-store.html
- https://caflorezvi.github.io/guias-apps-moviles/14.strings.html
- https://caflorezvi.github.io/guias-apps-moviles/15.manejo-permisos.html
- https://caflorezvi.github.io/guias-apps-moviles/16.mapas.html
- https://docs.mapbox.com/api/search/geocoding/

Las grabaciones de SharePoint requieren una sesión institucional disponible en el navegador. No se afirma haberlas revisado.

## Verificación del 24 de octubre de 2026

- Compilación de aplicación y APK de pruebas correcta (`assembleDebug`, `assembleDebugAndroidTest`).
- 49 pruebas unitarias sin fallos (`testDebugUnitTest`).
- 12 casos instrumentados aprobados en ejecuciones separadas en Pixel 7 Pro / Android 17: fotos (3), selección y conservación de ubicación al recrear Activity (1), preferencias e idioma (3), navegación (1), bienvenida (2), preparación y restauración/cierre de sesión entre procesos (2).
- Se verificó copia real de imagen al almacenamiento privado mediante resultado de galería simulado, cancelación sin perder la foto anterior y rechazo del permiso de cámara en el diálogo real del sistema. No se verificó una captura exitosa de cámara ni subida remota.
- Se corrigió el selector de la prueba de permisos: Android 17 presenta el botón por texto y no expone el identificador esperado de versiones anteriores. Se espera también a que vuelva el contenido de la app.
- Recursos español/inglés con claves equivalentes salvo `app_name`, compartido desde los recursos predeterminados. `git diff --check` sin errores.

Para reproducir, compilar con Java 21 (JBR de Android Studio), instalar ambos APK y establecer español antes de iniciar instrumentación:

```sh
adb shell cmd locale set-app-locales com.example.servinow --user current --locales es
adb shell am instrument -w -e class com.example.servinow.PhotoPickerTest,com.example.servinow.MapSelectionTest,com.example.servinow.PreferencesAndLanguageTest,com.example.servinow.NavigationFlowTest,com.example.servinow.WelcomeFlowTest com.example.servinow.test/androidx.test.runner.AndroidJUnitRunner
```

La comprobación de reinicio requiere dos procesos, en este orden (no ejecutar esos dos métodos como una suite sin orden):

```sh
adb shell am instrument -w -e class 'com.example.servinow.SessionRestartTest#prepare' com.example.servinow.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop com.example.servinow
adb shell am instrument -w -e class 'com.example.servinow.SessionRestartTest#restoreAndLogout' com.example.servinow.test/androidx.test.runner.AndroidJUnitRunner
```

El PDF de la entrega anterior no se regeneró con estas modificaciones.


## Actualización de ubicación — 29 de octubre de 2026

- «Mi ubicación» disponible en el mapa OpenStreetMap, sin credenciales externas. Solicita ubicación precisa o aproximada únicamente al pulsar el botón.
- Una posición nueva actualiza el centro y la selección del mapa. El formulario conserva las coordenadas elegidas.
- La petición dura como máximo 20 segundos y retira listeners al terminar, cancelar o salir de la pantalla. No hay seguimiento en segundo plano.
- Si la ubicación está apagada se ofrece abrir Ajustes. Si se deniega el permiso o no hay señal, se informa al usuario y se mantiene la selección manual.
- Sin token se consulta Geocoder de Android; necesita un proveedor disponible y puede no devolver dirección. En Android 13+ la consulta es asíncrona con espera máxima de 8 segundos; en versiones anteriores se ejecuta fuera del hilo de interfaz. Las coordenadas siguen siendo utilizables si no hay respuesta.
- No se ha agregado ningún token de Mapbox ni se ha probado su conexión autenticada.

Verificación de esta actualización: `assembleDebug`, `assembleDebugAndroidTest` y 49 pruebas unitarias aprobados. Se agregó `MapSelectionTest.currentGpsLocationUpdatesDraft`, habilitada explícitamente con el argumento de instrumentación `gpsFix=true` y una posición suministrada por el anfitrión (`adb emu geo fix -74.08 4.65`). Esta prueba nueva no se pudo ejecutar en esta sesión: no hay `/dev/kvm` y el emulador por software no llegó a tener disponible el servicio de paquetes. No se afirma haber verificado GPS ni direcciones reales en un dispositivo en esta actualización.
