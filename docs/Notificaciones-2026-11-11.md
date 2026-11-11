# Notificaciones de ServiNow

Se añadieron textos específicos para propuestas, mensajes, aceptación, finalización, comentarios y aprobación o rechazo de publicaciones. La pestaña Avisos muestra el contador de avisos sin leer. Abrir un aviso marca su lectura y navega al chat, las propuestas o el trabajo relacionado.

Los avisos se guardan en Firestore, en members/{uid}/notices. Firebase Cloud Messaging entrega las alertas al dispositivo registrado. El servidor verifica participantes y estado del evento, comprueba la respuesta de FCM y elimina tokens caducados. Los límites de eventos se separaron de los límites de IA e imágenes.

Android utiliza el canal «Mensajes y trabajos», con importancia alta y sonido predeterminado. Su presentación y sonido dependen del permiso del usuario, volumen, configuración del canal y modo No molestar. La bandeja interna funciona aunque se desactiven las alertas del dispositivo. No se afirma recepción en un teléfono físico: la prueba se realizó en el emulador.

## Verificación

- 55 pruebas unitarias Android sin errores.
- 27 pruebas del servidor sin errores.
- Prueba integrada con Firebase y FCM: propuesta en primer plano, mensaje en segundo plano, contenido localizado, apertura del chat desde el aviso y confirmación de lectura.
- Captura y resultado de instrumentación en evidencias-notificaciones-2026-11-11.

La cuenta temporal usada como trabajador no crea un perfil público y se elimina al terminar; por eso la evidencia puede mostrar «Usuario eliminado». No se guardan credenciales en este documento ni en el test.
