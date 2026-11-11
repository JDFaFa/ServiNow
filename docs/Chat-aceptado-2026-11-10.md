# Chat después de aceptar un trabajo

Se corrigió la suscripción a mensajes en Firestore. Antes se decidía si escuchar el chat a partir de una copia local del trabajo al abrir la pantalla. Si la asignación todavía no había llegado, el chat no comenzaba a escuchar. Además, la aceptación anulaba la selección del documento.

Ahora se escucha el documento del trabajo directamente. Cuando cambian su estado o trabajador asignado, se actualizan las escuchas del contenido y del chat. Se eliminan las escuchas anteriores al cambiar de trabajo o cerrar sesión. Se mantiene el acceso privado para el autor y el trabajador asignado; no se cambiaron las reglas de seguridad.

Se añadió el acceso Chat privado en el detalle para ambos participantes y en Mis propuestas para el trabajador aceptado. El acceso de Seguimiento sigue disponible.

Prueba real en el emulador: dos cuentas nuevas, publicación aprobada y propuesta de trabajador. Con Seguimiento ya abierto, se aceptó desde otra sesión y llegó un primer mensaje sin salir de la pantalla. El cliente respondió en la app, el trabajador abrió desde su propuesta aceptada, leyó y contestó; el cliente volvió a entrar y vio los tres mensajes. También se verificó la conservación de la conversación al recrear la Activity. La prueba terminó correctamente en 22,8 segundos. Se restauró la sesión del moderador y se eliminaron las cuentas de autenticación temporales usadas en la prueba; los documentos identificados como PRUEBA CHAT permanecen como evidencia.

Compilación correcta y 55 pruebas unitarias Android sin fallos. No se comprobó entrega push en un teléfono físico en este recorrido. Evidencia: `evidencias-chat-2026-11-10/`.
