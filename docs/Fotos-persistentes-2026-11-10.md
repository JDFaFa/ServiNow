# Fotos persistentes — 10 de noviembre de 2026

La app incluía Coil Compose, pero faltaba el módulo de red de Coil 3. Esto permitía previsualizar archivos locales, pero impedía descargar las imágenes HTTPS guardadas en Cloudinary.

Se añadió `coil-network-okhttp:3.3.0`, con la misma versión de Coil utilizada en el proyecto. Se creó el componente `ServicePhoto` para mostrar carga, errores y reintento. Se usa en publicaciones, vista previa, moderación y perfiles; las tarjetas de trabajos ahora también incluyen su fotografía.

Al guardar una publicación, se sube el archivo local a Cloudinary y se escribe su URL en `posts/{id}.imageUri`. Al guardar un perfil, la subida es automática y la URL queda en `profiles/{uid}.photoUri`. Los bytes se almacenan en Cloudinary; Firestore conserva la referencia persistente. No se guardan rutas locales del teléfono como imágenes compartidas. La dirección solo se escribe después de una subida satisfactoria.

Verificación en el emulador con servicios reales: selección de un archivo de prueba, subida, publicación, lectura de `imageUri` desde el servidor, lectura del mismo documento desde una segunda cuenta, carga HTTPS en Coil y visualización tras recrear la Activity. También se verificó `photoUri` guardado en el servidor después de guardar un perfil con archivo local. La cuenta secundaria temporal se eliminó y se restauró la foto anterior del perfil usado para la prueba. La publicación identificada como PRUEBA FOTO permanece disponible.

El primer intento de la prueba se omitió porque el emulador reiniciado no tenía sesión; se inició sesión y se repitió correctamente. La captura `evidencias-fotos-2026-11-10/foto-publicacion.png` corresponde a la ejecución real completada.

Compilación y 55 pruebas unitarias Android correctas. La prueba instrumentada `LivePhotoPersistenceTest` requiere activación explícita y una foto local de prueba. El límite actual del servidor es de 5 MB por imagen y admite JPEG, PNG y WebP.

Referencia: https://coil-kt.github.io/coil/network/
