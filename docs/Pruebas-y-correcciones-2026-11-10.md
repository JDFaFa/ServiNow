# ServiNow: pruebas y correcciones del 10 de noviembre de 2026

Se probó la aplicación instalada en el emulador Pixel 7 Pro, conectada a Firebase `servinow-4efcf` y al Worker de ServiNow. Los datos creados están identificados como PRUEBA. No se aprobaron ni modificaron publicaciones reales de otros usuarios.

## Resultados

| Rol | Recorrido | Resultado |
| --- | --- | --- |
| Moderador | Inicio de sesión, cuenta, avisos y bandeja; pendientes en español e inglés | Aprobado |
| Moderador | Abrir una publicación pendiente y aprobarla | Aprobado |
| Moderador | Rechazar una publicación con motivo | Aprobado |
| Usuario | Registro repetido, acceso inválido, validación del título, publicación con foto, selección en Mapbox y envío a revisión | Aprobado |
| Usuario / trabajador | Ver trabajo de otra cuenta, enviar propuesta, verla como cliente, aceptar, chat y finalizar | Aprobado |
| Usuario | Cuenta, idioma español/inglés, recreación de Activity, navegación por perfil, trabajos, filtros, estadísticas y logros; cierre de sesión | Aprobado |

La primera ejecución requirió corregir el selector del campo de motivo de rechazo: el encabezado y el campo compartían texto. Después los seis recorridos pasaron. Se ampliaron y volvieron a ejecutar las pruebas de publicaciones, propuestas y cuentas tras la solicitud del usuario.

- 40 comprobaciones de servicios reales: todas aprobadas. Incluyen permisos, persistencia, propuestas, notificaciones guardadas, chat, finalización, reseña, IA y subida/lectura de una imagen.
- 55 pruebas unitarias Android: sin fallos.
- 18 pruebas del servidor: sin fallos.
- Compilación e instalación del APK: correctas.

## Publicaciones y propuestas entre cuentas

La consulta pública devolvió 19 publicaciones, 16 de otros autores para el cliente de prueba. En ese momento había 8 pendientes. Las pendientes solo son visibles para su autor y los moderadores; se muestran a otros usuarios al aprobarlas. Los trabajos finalizados no aparecen con el filtro predeterminado de solo trabajos abiertos.

Las propuestas se guardan en `offers` y las consultas del cliente y del trabajador respondieron correctamente. Una propuesta solo es visible para el trabajador que la envía y el autor del trabajo. No se relajaron las reglas de privacidad para hacerlas públicas.

Cambios realizados:

- Los filtros se reinician al cambiar de cuenta, evitando heredar una búsqueda o ubicación de otro usuario.
- Armenia se incluye en el selector de ciudad y es el centro inicial del mapa de búsqueda.
- Inicio informa cuántas publicaciones propias esperan aprobación y por qué aún no aparecen a otras cuentas.
- Trabajos incluye un acceso a propuestas enviadas y un botón con el número de propuestas recibidas debajo de cada publicación.
- El filtro de pendientes del moderador usa un valor estable, independiente del idioma.

## Cuentas

Firebase rechazó el registro de correos existentes con `EMAIL_EXISTS`. También se comprobó el aviso en el formulario real. Se amplió el mensaje con instrucciones para iniciar sesión, recuperar contraseña o continuar con Google.

Para el acceso inválido se muestra «El correo o la contraseña son incorrectos», sin afirmar que únicamente falló la contraseña. La respuesta de autenticación no permite distinguir de forma fiable un correo inexistente de una contraseña equivocada; no se agregó un listado público de cuentas.

Se habilitó `derook7598@gmail.com` como MODERATOR mediante su documento `moderators/{uid}` con `enabled: true`. Es un rol de la aplicación, no acceso administrativo a la consola Firebase. La contraseña no está guardada en este informe ni en el repositorio.

## Otras correcciones

- El formulario exige al menos 5 caracteres para el título, igual que el servidor.
- Solo el autor puede ver la acción de finalizar un trabajo en progreso. Se eliminan acciones visibles que el servidor rechazaba por estado o permisos.

## Evidencias y repetición

Capturas y resultados en `evidencias-pruebas-2026-11-10/`. El test `LiveRoleFlowsTest.kt` es opcional: requiere `live-role-config.json` en el almacenamiento privado de la app con cuentas y publicaciones de prueba. No contiene credenciales en el código y se omite cuando falta ese archivo. Las pruebas de aprobar, rechazar y contratar cambian datos: antes de repetirlas se deben preparar publicaciones nuevas.

## Límites de esta verificación

Las pruebas cubren los recorridos enumerados; no demuestran ausencia de todos los errores posibles. Los avisos persistidos se verificaron, pero no la llegada de una notificación push a otro teléfono físico. No se repitió la entrega del correo de recuperación, el acceso interactivo con Google ni la eliminación permanente de una cuenta. La IA respondió correctamente a un caso válido y rechazó uno inválido, sin garantizar disponibilidad permanente del proveedor. La selección del punto en Mapbox se comprobó, no la precisión GPS de un dispositivo físico.
