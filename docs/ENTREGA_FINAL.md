# Entrega final — ServiNow

## Decisiones confirmadas

- Propietario de Firebase: cuenta institucional juand.sazaa@uqvirtual.edu.co, autorizada explícitamente el 30 de octubre. OpenRouter: cuenta personal del equipo.
- No habilitar facturación de Firebase.
- Autenticación y recuperación por enlace seguro: Firebase Authentication. No enviar contraseñas por correo ni almacenar sus hashes en Firestore.
- Base de datos y chat: Cloud Firestore; modelos de dominio y repositorios remotos detrás de interfaces, estado observable en ViewModel y Navigation Compose.
- Imágenes: Cloudinary con subida firmada mediante Cloudflare Worker. Credenciales guardadas como secretos del servidor.
- IA: OpenRouter a través de backend autenticado. La clave privada no se incluye en el APK. Propuesta: mejorar el texto de una solicitud, sin publicarlo automáticamente.
- Mapas: Mapbox; español/inglés mediante recursos Android y selector persistente de idioma.

## Estado real al 31 de octubre de 2026

| Función | Estado |
|---|---|
| Mapbox | Token local configurado, API de estilos respondió HTTP 200; mapa abierto en emulador |
| Ubicación | Posición simulada de Armenia recibida por Android y mostrada en la app; no equivale a medir GPS físico |
| Idioma | Selector español/inglés/sistema implementado y probado |
| Persistencia del negocio | Pendiente: usuarios, publicaciones, ofertas y comentarios todavía en memoria |
| Auth real y recuperación | Firebase Authentication conectado a registro/login/recuperación. Correo y contraseña habilitados; política de 8 caracteres, mayúscula, número y símbolo exigida en servidor. Enlace de recuperación manejado por Firebase. Google Sign-In pendiente; recepción real del correo y prueba visual Android pendientes. |
| Fotos en nube | Endpoint autenticado probado: subida sintética a Cloudinary HTTP 200. Integración Android preparada; prueba completa en dispositivo pendiente. |
| Chat privado | Interfaz y reglas locales implementadas; historial remoto aún no conectado a las pantallas. |
| Notificaciones push | Pendiente; avisos actuales son del repositorio en memoria |
| Finalizar publicación | Autor o moderador ya autorizados por repositorio; acceso directo agregado al detalle y confirmación existente |
| Compartir | Selector nativo Android para compartir texto de trabajos públicos; no existe todavía un enlace público de trabajo |
| IA | OpenRouter gratuito mediante Worker autenticado probado HTTP 200. Propuesta editable integrada; prueba visual Android pendiente. |

## Contratos de los servicios pendientes

- Chat por trabajo entre cliente y trabajador: ambos deben ser participantes autorizados; los comentarios públicos se mantienen separados. Mensajes con autor validado, fecha del servidor, estado de envío y carga de historial paginada. No hacer públicos correos ni tokens.
- Publicaciones y ofertas: reglas de Firestore deben validar propietario, campos editables y transiciones. El rol de moderador no puede asignarse desde el cliente. Probar denegaciones con usuario ajeno y sin sesión.
- Finalización: confirmar explícitamente; persistir estado y avisar a las partes. Impedir cambios o aceptación de ofertas después del cierre y permitir reseña cuando corresponda.
- Avisos push: Firebase Cloud Messaging con servidor autenticado; permiso de notificaciones Android 13+, tokens por dispositivo, limpieza al salir y apertura del trabajo/chat desde el aviso. No permitir al cliente enviar push arbitrarios.
- Recuperación: correo con enlace de Firebase, caducidad y uso único manejados por el proveedor; no simular que el correo fue enviado. El reset en memoria no forma parte de la versión final conectada.
- Compartir enlaces: requiere URL pública o app link desplegado y verificable. Por ahora se comparte título, categoría, ciudad, presupuesto y descripción, sin teléfono, correo ni coordenadas precisas.
- Imágenes: validar autenticación, MIME, tamaño y propietario; firma de subida en backend, no secretos en Android; persistir URL/identificador remoto, mostrar progreso/error y limpiar archivos huérfanos.
- IA: validar sesión, limitar tamaño/frecuencia y tiempo de respuesta, mostrar propuesta editable; no enviar datos personales ni mensajes privados como contexto por defecto.

## Pruebas de aceptación antes de entregar

1. Crear cuenta real; iniciar sesión, cerrar proceso y recuperar cuenta; correo real de recuperación y enlace expirado/inválido.
2. Crear/editar publicación desde un dispositivo y verla desde otro; datos intactos después del cierre.
3. Subir foto y abrirla desde otro dispositivo autenticado; denegar subida no autorizada.
4. Chat entre dos cuentas; impedir lectura/escritura a una tercera; recibir aviso en segundo plano y abrir conversación correcta.
5. Autor finaliza trabajo y trabajador ve el nuevo estado; tercero no puede finalizarlo.
6. IA responde desde el servicio, permite aceptar o descartar la propuesta y muestra errores reales.
7. Compartir texto y, cuando exista, enlace funcional; probar español e inglés en todos estos flujos.
8. Documentar proveedor, biblioteca, configuración sin secretos, límites, capturas reales, uso de IA y resultados. No presentar conexiones pendientes como completas.

Validación de compartir y finalizar: 49 pruebas unitarias aprobadas y `ShareAndFinishTest` con 2 casos instrumentados aprobados en el emulador (selector nativo, contenido compartido y finalización del autor con confirmación).

## Avance verificado del servidor — 31 de octubre de 2026

Worker publicado: https://servinow-services.juand-sazaa.workers.dev. `/health` responde HTTP 200; subir imágenes sin identidad devuelve HTTP 401. Credenciales de Cloudinary guardadas como secretos del Worker, fuera del APK y del repositorio. La copia temporal fue eliminada.

Pendiente: configurar OpenRouter y comprobar imágenes con un usuario autenticado. Autenticación real aplazada por solicitud del usuario. El repositorio Firestore está preparado pero no sustituye aún al repositorio en memoria; el chat actual es local. Notificaciones push y pruebas integrales de servicios externos siguen pendientes. El despliegue del servidor no demuestra todavía estas funcionalidades completas.

### OpenRouter configurado — 31 de octubre de 2026

OPENROUTER_API_KEY y OPENROUTER_MODEL se guardaron como secretos en Cloudflare. Modelo: openrouter/free. Una solicitud sintética directa al proveedor respondió HTTP 200 con respuesta válida usando límite de gasto $0. Clave con vencimiento 30 de noviembre de 2026. Se eliminó el archivo temporal usado para transferirla. Esta prueba verifica el proveedor, no el flujo completo desde Android: aún requiere autenticación Firebase, aplazada.

## Autenticación real implementada y validada

Se autorizó retomar la autenticación el 31 de octubre. AuthenticationRepository separa el dominio del SDK; FirebaseAuthenticationRepository usa creación de cuenta, login, perfil básico y envío de enlace seguro. CommunityViewModel maneja espera, errores y navegación; la identidad local se asigna solo después de respuesta válida del proveedor. No hay fallback a claves de demostración. Los permisos de moderador proceden de custom claims del servidor. No se almacenan contraseñas en DataStore ni en estado guardado de pantalla.

El registro y recuperación muestran errores emergentes localizados. Se retiran el acceso demo y enlace de recuperación ficticio del flujo conectado. El correo abre el formulario seguro de Firebase, no la vista de reset sin verificación. Salir cierra Firebase y limpia la identidad de la app. La opción de recordar sesión se respeta al iniciar otro proceso. La eliminación de cuenta remota queda pendiente y muestra un mensaje explícito, sin fingir eliminación.

Pruebas: alta real temporal y acceso HTTP 200; contraseña débil y contraseña incorrecta rechazadas. IA e imagen sintética a través del Worker con ID token real: HTTP 200. Cuentas de prueba eliminadas; imagen sintética de un píxel conservada como evidencia de subida. Estas pruebas de servicios no equivalen al recorrido visual Android ni demuestran persistencia del negocio. SDK según documentación: https://firebase.google.com/docs/auth/android/password-auth y https://firebase.google.com/docs/auth/android/manage-users.

Se añadieron cuatro pruebas unitarias: rechazo sin fallback demo, identidad Firebase y cierre de sesión, registro sin contraseña local y bloqueo del reset local no verificado. El emulador no arrancó: /dev/kvm no está disponible en este entorno. No se ha instalado esta compilación en un dispositivo.
