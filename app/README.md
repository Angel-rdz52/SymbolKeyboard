# Symbol Keyboard (prototipo)

Teclado Android que reemplaza letras por símbolos Unicode parecidos,
configurables por vos desde una pantalla de 2 columnas (original / reemplazo).

## Qué incluye este prototipo

- `MainActivity.kt` — pantalla de configuración (Compose): lista de 2 columnas
  editable, botón para abrir los ajustes de teclado del sistema, switch para
  activar/desactivar el reemplazo, y presets rápidos (Al revés, Leet speak,
  Runas, Círculos).
- `SymbolKeyboardService.kt` — el teclado (`InputMethodService`) que aplica
  el mapeo carácter por carácter mientras escribís.
- `CharMapRepository.kt` — guarda tu mapeo en SharedPreferences (persiste
  entre sesiones) y define los presets.
- `res/xml/method.xml` + el `<service>` en `AndroidManifest.xml` — esto es lo
  que hace que el teclado aparezca listado en Ajustes > Sistema > Idiomas y
  entrada > Teclado virtual, y en el selector rápido de teclados.

## Cómo generar el APK usando GitHub (sin instalar Android Studio)

1. Creá un repositorio nuevo en GitHub (puede ser privado).
2. Subí **todo** el contenido de esta carpeta `SymbolKeyboard/` a la raíz
   del repo (podés arrastrar los archivos desde la web de GitHub, o con git:
   `git init && git add . && git commit -m "init" && git remote add origin <url> && git push -u origin main`).
3. Andá a la pestaña **Actions** de tu repo en GitHub. El workflow
   `Build APK` (`.github/workflows/build-apk.yml`) se dispara solo al hacer
   push a `main`. Si no arrancó, tocá "Run workflow" manualmente.
4. Esperá a que termine (2-4 min). Entrá al run terminado y bajá el archivo
   **Artifacts > symbol-keyboard-apk** — es un `.zip` que contiene el
   `app-debug.apk`.
5. Pasá ese APK a tu celular (Drive, cable, etc.) e instalalo. Vas a tener
   que permitir "instalar apps de fuentes desconocidas" la primera vez.

No hace falta ninguna keystore ni secreto: el workflow firma el APK con el
certificado de debug que genera automáticamente el propio runner de GitHub,
suficiente para instalar y probar en tu propio teléfono.

## Cómo activarlo en el celular

1. Abrí la app "Symbol Keyboard" → botón **"Abrir ajustes de teclado"** →
   activá el switch de "Symbol Keyboard" en la lista de teclados.
2. Andá a cualquier campo de texto, tocá el ícono de teclado en la barra de
   navegación (o mantené apretada la barra espaciadora en Gboard) y elegí
   "Symbol Keyboard" en el selector.
3. Volvé a la app para personalizar el mapeo o cargar un preset.

## Ideas para seguir personalizando

- Agregar más presets (griego, cirílico visual, emojis por letra).
- Botón dentro del propio teclado para alternar "modo normal / modo símbolos"
  sin salir del campo de texto (evita ir al selector del sistema cada vez).
- Exportar/importar el mapeo como texto para compartir "skins" con amigos.
- Dibujar las teclas mostrando ya el símbolo reemplazado (hoy el teclado usa
  el layout QWERTY estándar de Android, así que muestra la letra original en
  la tecla aunque tipee el símbolo).
