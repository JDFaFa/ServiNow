# Mejora de descripciones con IA

Se reprodujeron dos errores: rechazo de solicitudes breves de dos palabras y respuestas del proveedor que no cumplían el formato JSON. También se detectaron respuestas que cambiaban detalles del trabajo, por lo que se ajustó la selección del modelo y las instrucciones.

El servidor utiliza OpenRouter con `nvidia/nemotron-3-ultra-550b-a55b:free` como primera opción. Hay un segundo intento con `openrouter/free` si la primera respuesta falla. Ambos son gratuitos; no se habilitó facturación. El segundo intento es limitado y no garantiza disponibilidad del proveedor. El usuario siempre revisa la propuesta antes de aplicarla.

Cambios:
- Se admiten solicitudes breves, como «necesito plomero».
- Se acepta JSON válido envuelto en un bloque Markdown, sin aceptar explicaciones arbitrarias como descripciones.
- Las instrucciones piden corregir ortografía y claridad conservando los hechos y sin inventar causas, horarios o precios.
- Se amplió el presupuesto de salida y se desactivó el razonamiento opcional del modelo principal para esta tarea breve.
- La app distingue saturación y tiempo de espera; muestra progreso y conserva el borrador.
- La vista de la sugerencia permite desplazamiento para textos largos.

Verificación: 25 pruebas del servidor aprobadas y compilación Android correcta. Tres solicitudes reales devolvieron descripciones válidas. El recorrido instrumentado en el emulador comprobó que el texto no se modifica antes de aceptar, que Usar esta descripción actualiza el borrador y que una entrada sin sentido conserva el original y muestra un aviso. La prueba no garantiza que todas las respuestas futuras del modelo sean correctas.

Ejemplo real: «se daño la llave dela cosina bota agua» → «Necesito reparar la llave de la cocina, ya que está dañada y pierde agua.»

Evidencias: `evidencias-ia-2026-11-10/`.

Referencia técnica: https://openrouter.ai/docs/guides/best-practices/reasoning-tokens
