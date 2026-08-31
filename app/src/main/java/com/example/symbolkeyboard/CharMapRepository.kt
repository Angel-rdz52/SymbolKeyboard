package com.example.symbolkeyboard

import android.content.Context
import org.json.JSONObject

/**
 * Guarda y expone el mapa "carácter original -> reemplazo".
 * Se persiste como JSON plano en SharedPreferences para que tanto
 * la pantalla de configuración (MainActivity) como el servicio de
 * teclado (SymbolKeyboardService) lean siempre la misma fuente.
 */
class CharMapRepository(context: Context) {

    private val prefs = context.getSharedPreferences("symbol_keyboard_prefs", Context.MODE_PRIVATE)

    fun loadMap(): LinkedHashMap<String, String> {
        val json = prefs.getString(KEY_MAP, null) ?: return defaultPreset().toMutableMap().let { LinkedHashMap(it) }
        val obj = JSONObject(json)
        val map = LinkedHashMap<String, String>()
        obj.keys().forEach { key -> map[key] = obj.getString(key) }
        return map
    }

    fun saveMap(map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_MAP, obj.toString()).apply()
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_MAP = "char_map_json"
        private const val KEY_ENABLED = "map_enabled"

        /** Preset por defecto: dejar todo tal cual (mapa vacío = sin reemplazos). */
        fun defaultPreset(): Map<String, String> = emptyMap()

        /** Preset "Al revés": aproxima cada letra con su glifo Unicode invertido. */
        fun upsideDownPreset(): Map<String, String> = mapOf(
            "a" to "ɐ", "b" to "q", "c" to "ɔ", "d" to "p", "e" to "ǝ",
            "f" to "ɟ", "g" to "ƃ", "h" to "ɥ", "i" to "ᴉ", "j" to "ɾ",
            "k" to "ʞ", "l" to "ʃ", "m" to "ɯ", "n" to "u", "o" to "o",
            "p" to "d", "q" to "b", "r" to "ɹ", "s" to "s", "t" to "ʇ",
            "u" to "n", "v" to "ʌ", "w" to "ʍ", "x" to "x", "y" to "ʎ",
            "z" to "z"
        )

        /** Preset "Leet speak": letras por números/símbolos parecidos. */
        fun leetPreset(): Map<String, String> = mapOf(
            "a" to "4", "e" to "3", "i" to "1", "o" to "0",
            "s" to "5", "t" to "7", "g" to "9", "b" to "8"
        )

        /** Preset "Runas": aproximación estética con runas Unicode. */
        fun runicPreset(): Map<String, String> = mapOf(
            "a" to "ᚨ", "b" to "ᛒ", "c" to "ᚲ", "d" to "ᛞ", "e" to "ᛖ",
            "f" to "ᚠ", "g" to "ᚷ", "h" to "ᚺ", "i" to "ᛁ", "j" to "ᛃ",
            "k" to "ᚴ", "l" to "ᛚ", "m" to "ᛗ", "n" to "ᚾ", "o" to "ᛟ",
            "p" to "ᛈ", "q" to "ᚲ", "r" to "ᚱ", "s" to "ᛊ", "t" to "ᛏ",
            "u" to "ᚢ", "v" to "ᚡ", "w" to "ᚥ", "x" to "ᚲᛊ", "y" to "ᚤ", "z" to "ᛉ"
        )

        /** Preset "Círculos": letras dentro de círculos Unicode (ⓐⓑⓒ...). */
        fun circledPreset(): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            for (c in 'a'..'z') {
                val codepoint = 0x24D0 + (c - 'a') // ⓐ = U+24D0
                map[c.toString()] = String(Character.toChars(codepoint))
            }
            return map
        }

        /** Preset "Espejo": invierte el orden de las letras dentro de cada palabra. */
        const val WORD_MIRROR_MARKER = "__WORD_MIRROR__"

        fun allPresets(): List<Pair<String, Map<String, String>>> = listOf(
            "Sin cambios" to defaultPreset(),
            "Al revés (ɐqɔ)" to upsideDownPreset(),
            "Leet speak (4pr3ndiz)" to leetPreset(),
            "Runas ᚱᚢᚾᛁᚲ" to runicPreset(),
            "Círculos Ⓐ Ⓑ Ⓒ" to circledPreset()
        )
    }
}
