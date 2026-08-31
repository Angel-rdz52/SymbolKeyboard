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
 */
class SymbolKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard

    private lateinit var repository: CharMapRepository

    // Mapa de reemplazo vigente (siempre en minúsculas como clave).
    private var charMap: Map<String, String> = emptyMap()
    private var replacementEnabled: Boolean = true

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

        keyboardView = KeyboardView(this, null)
        keyboardView.keyboard = qwertyKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

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
        }
    }

    private fun reloadCharMap() {
        replacementEnabled = repository.isReplacementEnabled()
        charMap = repository.loadMap()
    }

    // --- Lógica de reemplazo de caracteres -------------------------------

    /**
     * Dado un carácter recién tipeado, busca su reemplazo en el mapa
     * (siempre indexado por la versión en minúscula) y devuelve el
     * reemplazo respetando si el original era mayúscula o minúscula.
     * Si no existe reemplazo o el reemplazo está desactivado, devuelve
     * el carácter original sin modificar.
     */
    private fun applyReplacement(char: Char): String {
        if (!replacementEnabled) return char.toString()

        val isUpper = char.isUpperCase()
        val lowerKey = char.lowercaseChar().toString()
        val replacement = charMap[lowerKey] ?: return char.toString()

        return if (isUpper) {
            // Si el reemplazo tiene un único carácter, lo pasamos a
            // mayúscula. Si es un glifo compuesto (ej. "x" -> "ᚴᛋ"),
            // lo dejamos tal cual ya que no siempre tiene mayúscula.
            if (replacement.length == 1) {
                replacement.uppercase()
            } else {
                replacement
            }
        } else {
            replacement
        }
    }

    private fun commitReplacedChar(codeChar: Char) {
        val output = applyReplacement(codeChar)
        currentInputConnection?.commitText(output, 1)
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
                    keyboardView.invalidateAllKeys()
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
        keyboardView.invalidateAllKeys()
    }

    private fun switchToSymbols() {
        isSymbolsMode = true
        keyboardView.keyboard = symbolsKeyboard
    }

    private fun switchToLetters() {
        isSymbolsMode = false
        keyboardView.keyboard = qwertyKeyboard
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
