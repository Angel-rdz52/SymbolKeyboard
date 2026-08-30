# Symbol Keyboard

**Symbol Keyboard** es un teclado personalizado (IME) para Android que
reemplaza las letras que escribís por símbolos Unicode parecidos, de forma
totalmente configurable por vos.

Es una app de **uso libre**: podés instalarla, modificarla, redistribuirla
y usar su código como base para tus propios proyectos, sin restricciones.

## ¿Qué hace?

Symbol Keyboard se instala como un teclado más del sistema operativo. Tiene
un layout QWERTY estándar, y cada vez que tocás una tecla, antes de mandar
el carácter al campo de texto, el teclado revisa si esa letra tiene un
"reemplazo" configurado. Si lo tiene, escribe el reemplazo (respetando si
la letra era mayúscula o minúscula); si no, escribe la letra normal.

Esto te permite, por ejemplo, escribir mensajes usando símbolos "al revés",
en leet speak, con runas nórdicas o con letras dentro de círculos — sin
tener que copiar y pegar carácter por carácter desde otra app.

## Funcionalidades

- **Teclado del sistema real**: aparece en *Ajustes > Sistema > Idiomas y
  entrada > Teclado virtual* y en el selector rápido de teclados, como
  cualquier otro teclado instalado.
- **Reemplazo en tiempo real**: cada letra tipeada se busca en tu mapa de
  reemplazo antes de enviarse.
- **Activar/desactivar**: un switch en la app te permite prender o apagar
  el reemplazo globalmente sin desinstalar el teclado ni perder tu mapeo
  guardado.
- **Mapeo 100% editable**: una lista de dos columnas (Original / Reemplazo)
  donde podés agregar filas, editar el texto y borrar filas individuales.
- **Presets predefinidos**, para cargar un mapeo completo con un solo toque:
  - **Al revés** — letras rotadas 180° (a→ɐ, b→q, c→ɔ, …).
  - **Leet speak** — reemplazos numéricos clásicos (a→4, e→3, i→1, o→0, s→5…).
  - **Runas** — alfabeto rúnico nórdico antiguo (a→ᚨ, b→ᛒ, c→ᚲ…).
  - **Círculos** — letras encerradas en un círculo Unicode (a→ⓐ, b→ⓑ…).
  - **Sin cambios** — resetea el mapeo para que el teclado escriba normal.
- **Guardado automático**: cada cambio (switch, filas, presets) se guarda
  al instante en SharedPreferences como JSON, y persiste aunque cierres la
  app o reinicies el celular.
- **Recarga en cada uso**: el teclado vuelve a leer el mapeo guardado cada
  vez que se abre, así que los cambios que hagas en los ajustes se aplican
  en el próximo mensaje que escribas, sin tener que reiniciar nada.

## Cómo personalizarlo

1. Abrí la app **Symbol Keyboard**.
2. Tocá **"Activar teclado en Ajustes del sistema"** y habilitá el teclado
   (ver el instructivo `INSTRUCCIONES.txt` para el paso a paso completo).
3. En la pantalla principal:
   - Usá el switch **"Reemplazo activado"** para prender/apagar el efecto.
   - Tocá **"Presets"** para cargar uno de los mapeos predefinidos.
   - Editá cualquier fila tocando los campos "Original" o "Reemplazo".
   - Tocá el ícono de basura para borrar una fila.
   - Tocá **"Agregar fila"** para sumar tus propios reemplazos personalizados
     (por ejemplo, mapear "a" a cualquier símbolo Unicode que quieras).
4. Cambiá de teclado al de Symbol Keyboard desde la barra de idioma del
   teclado del sistema (ícono de teclado en la barra inferior al escribir)
   y probá a escribir en cualquier app.

## Estructura técnica del proyecto

- `MainActivity.kt` — pantalla de configuración hecha en Jetpack Compose +
  Material 3.
- `SymbolKeyboardService.kt` — el `InputMethodService` (IME) que dibuja el
  teclado QWERTY y aplica el reemplazo de caracteres antes de enviarlos.
- `CharMapRepository.kt` — capa de persistencia: guarda/carga el mapa como
  JSON en `SharedPreferences`, y define los presets como constantes.
- `res/xml/qwerty.xml` y `res/xml/symbols.xml` — definición del layout de
  teclas (letras y símbolos/números) usando la API clásica
  `android.inputmethodservice.Keyboard`.
- `res/xml/method.xml` — declaración del IME y su subtype, requerido por
  el sistema para listar el teclado en los ajustes.

Requisitos: Android 7.0 (API 24) o superior.

## Licencia

Proyecto de uso libre. Podés usar, copiar, modificar y distribuir este
código con o sin fines comerciales, sin necesidad de pedir permiso ni dar
crédito (aunque siempre se agradece 🙂).
