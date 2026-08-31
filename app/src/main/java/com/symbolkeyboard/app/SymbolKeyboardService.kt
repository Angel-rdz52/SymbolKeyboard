package com.symbolkeyboard.app

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * Servicio de teclado (IME) que dibuja un layout QWERTY estándar de Android
 * usando la API clásica android.inputmethodservice.Keyboard/KeyboardView, y
 * antes de enviar cada carácter tipeado lo busca en el mapa de reemplazo
 * configurable por el usuario (CharMapRepository). Si existe reemplazo,
 * envía el reemplazo (respetando mayúsculas/minúsculas); si no, envía el
 * carácter original.
 *
 * Si el usuario activó la "vista previa" en los ajustes, además redibuja
 * las teclas mostrando directamente el símbolo de reemplazo en vez de la
 * letra normal.
 */
class SymbolKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard

    private lateinit var repository: CharMapRepository

    // Mapa de reemplazo vigente.
    private var charMap: Map<String, String> = emptyMap()
    private var replacementEnabled: Boolean = true
    private var keyPreviewEnabled: Boolean = false

    // Guarda la letra "de fábrica" de cada tecla (codes[0] -> carácter
    // original), para poder restaurar las etiquetas cuando se apaga la
    // vista previa o cuando no hay reemplazo configurado para esa letra.
    private val originalLetterByCode = HashMap<Int, Char>()

    private var isShiftOn = false
    private var isCapsLock = false
    private var isSymbolsMode = false

    override fun onCreate() {
        super.onCreate()
        repository = CharMapRepository(this)
    }

    /**
     * onCreateInputView se llama cada vez que el teclado se muestra,
     * por lo que aprovechamos este punto para recargar el mapa de
     * reemplazo por si el usuario lo cambió en los ajustes desde la
     * última vez que se usó el teclado.
     */
    override fun onCreateInputView(): View {
        reloadCharMap()

        qwertyKeyboard = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard = Keyboard(this, R.xml.symbols)

        originalLetterByCode.clear()
        for (key in qwertyKeyboard.keys) {
            val code = key.codes.firstOrNull() ?: continue
            if (code in 32..0x2764) {
                val c = code.toChar()
                if (Character.isLetter(c)) {
                    originalLetterByCode[code] = c
                }
            }
        }

        keyboardView = KeyboardView(this, null)
        keyboardView.keyboard = qwertyKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        refreshKeyLabels()

        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Recargamos también acá por si el usuario volvió de los ajustes
        // sin que el teclado se haya recreado del todo.
        reloadCharMap()
        isShiftOn = false
        isCapsLock = false
        isSymbolsMode = false
        if (::keyboardView.isInitialized) {
            keyboardView.keyboard = qwertyKeyboard
            qwertyKeyboard.isShifted = false
            refreshKeyLabels()
        }
    }

    private fun reloadCharMap() {
        replacementEnabled = repository.isReplacementEnabled()
        keyPreviewEnabled = repository.isKeyPreviewEnabled()
        charMap = repository.loadMap()
    }

    // --- Lógica de reemplazo de caracteres -------------------------------

    /**
     * Dado un carácter recién tipeado, busca su reemplazo y devuelve el
     * resultado a enviar. Primero busca una coincidencia EXACTA en el mapa
     * (respetando mayúscula/minúscula tal como el usuario la configuró:
     * por ejemplo, se puede mapear "a" y "A" a símbolos distintos). Si no
     * hay coincidencia exacta, prueba con la versión en minúscula del
     * carácter y ajusta automáticamente a mayúscula si corresponde.
     */
    private fun applyReplacement(char: Char): String {
        if (!replacementEnabled) return char.toString()

        charMap[char.toString()]?.let { return it }

        val isUpper = char.isUpperCase()
        val lowerKey = char.lowercaseChar().toString()
        val replacement = charMap[lowerKey] ?: return char.toString()

        return if (isUpper) {
            if (replacement.length == 1) replacement.uppercase() else replacement
        } else {
            replacement
        }
    }

    private fun commitReplacedChar(codeChar: Char) {
        val output = applyReplacement(codeChar)
        currentInputConnection?.commitText(output, 1)
    }

    /**
     * Recorre las teclas de letras y actualiza su etiqueta visible según
     * corresponda: si la vista previa está activada y hay un reemplazo
     * configurado para esa letra, muestra el símbolo; si no, muestra la
     * letra normal (en mayúscula si el shift/caps lock está activo).
     */
    private fun refreshKeyLabels() {
        if (!::qwertyKeyboard.isInitialized || !::keyboardView.isInitialized) return

        val shifted = isShiftOn || isCapsLock

        for (key in qwertyKeyboard.keys) {
            val code = key.codes.firstOrNull() ?: continue
            val baseChar = originalLetterByCode[code] ?: continue

            val displayChar = if (shifted) baseChar.uppercaseChar() else baseChar

            key.label = if (keyPreviewEnabled && replacementEnabled) {
                applyReplacement(displayChar)
            } else {
                displayChar.toString()
            }
        }
        keyboardView.invalidateAllKeys()
    }

    // --- KeyboardView.OnKeyboardActionListener ----------------------------

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }
            Keyboard.KEYCODE_SHIFT -> {
                toggleShift()
            }
            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            KEYCODE_SYMBOLS -> {
                switchToSymbols()
            }
            KEYCODE_LETTERS -> {
                switchToLetters()
            }
            KEYCODE_SPACE -> {
                ic.commitText(" ", 1)
            }
            else -> {
                var code = primaryCode.toChar()
                if (Character.isLetter(code) && isShiftOn) {
                    code = code.uppercaseChar()
                }
                commitReplacedChar(code)

                // Shift no-lock se desactiva automáticamente tras una letra.
                if (isShiftOn && !isCapsLock) {
                    isShiftOn = false
                    qwertyKeyboard.isShifted = false
                    refreshKeyLabels()
                }
            }
        }
    }

    private fun toggleShift() {
        if (isSymbolsMode) return
        if (isShiftOn && !isCapsLock) {
            // Segundo tap rápido -> caps lock (simplificado: doble toque no
            // se detecta por tiempo, cada tap alterna shift/capslock).
            isCapsLock = true
        } else if (isCapsLock) {
            isCapsLock = false
            isShiftOn = false
        } else {
            isShiftOn = true
        }
        qwertyKeyboard.isShifted = isShiftOn || isCapsLock
        refreshKeyLabels()
    }

    private fun switchToSymbols() {
        isSymbolsMode = true
        keyboardView.keyboard = symbolsKeyboard
    }

    private fun switchToLetters() {
        isSymbolsMode = false
        keyboardView.keyboard = qwertyKeyboard
        refreshKeyLabels()
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {
        text ?: return
        val ic = currentInputConnection ?: return
        if (!replacementEnabled) {
            ic.commitText(text, 1)
            return
        }
        val builder = StringBuilder()
        for (c in text) {
            builder.append(applyReplacement(c))
        }
        ic.commitText(builder.toString(), 1)
    }

    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    companion object {
        // Códigos custom usados en xml/symbols.xml y xml/qwerty.xml para las
        // teclas especiales de cambio de modo y espacio.
        const val KEYCODE_SYMBOLS = -101
        const val KEYCODE_LETTERS = -102
        const val KEYCODE_SPACE = 32
    }
}
