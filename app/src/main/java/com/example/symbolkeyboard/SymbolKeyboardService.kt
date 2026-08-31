package com.example.symbolkeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.InputConnection

/**
 * Teclado QWERTY simple. La única diferencia con un teclado normal es
 * que, antes de mandar el carácter presionado, lo busca en el mapa
 * configurado por el usuario en MainActivity y manda el reemplazo
 * si existe.
 *
 * NOTA: usa la API clásica Keyboard/KeyboardView (simple y estable
 * para un prototipo). Se puede migrar a un teclado dibujado con
 * Compose/View custom más adelante para más control visual.
 */
class SymbolKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var repo: CharMapRepository
    private var charMap: Map<String, String> = emptyMap()
    private var mapEnabled: Boolean = true
    private var isShifted = false

    override fun onCreate() {
        super.onCreate()
        repo = CharMapRepository(this)
    }

    override fun onCreateInputView(): View {
        // Reutiliza el layout qwerty estándar de Android como base.
        qwertyKeyboard = Keyboard(this, android.R.xml.password_kbd_qwerty)
        keyboardView = KeyboardView(this, null)
        keyboardView.keyboard = qwertyKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        return keyboardView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Recarga el mapeo cada vez que se abre el teclado, por si el
        // usuario lo cambió en la pantalla de configuración.
        charMap = repo.loadMap()
        mapEnabled = repo.isEnabled()
    }

    private fun applyMapping(char: Char): String {
        if (!mapEnabled) return char.toString()
        val key = char.lowercaseChar().toString()
        val replacement = charMap[key] ?: return char.toString()
        return if (char.isUpperCase()) replacement.uppercase() else replacement
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic: InputConnection = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                qwertyKeyboard.isShifted = isShifted
                keyboardView.invalidateAllKeys()
            }
            10 -> ic.commitText("\n", 1) // Enter
            else -> {
                var code = primaryCode.toChar()
                if (isShifted) code = code.uppercaseChar()
                ic.commitText(applyMapping(code), 1)
            }
        }
    }

    // El resto de callbacks de OnKeyboardActionListener no se usan
    // en este prototipo, pero la interfaz los exige.
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
