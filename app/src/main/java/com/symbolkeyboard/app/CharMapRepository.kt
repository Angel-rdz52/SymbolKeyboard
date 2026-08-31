package com.symbolkeyboard.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Repositorio encargado de persistir el mapa de reemplazo de caracteres
 * (original -> reemplazo) en SharedPreferences, serializado como JSON.
 *
 * Es usado tanto por la Activity de configuración (para leer/escribir)
 * como por el IME (para leer el mapa vigente en cada sesión de tecleo).
 */
class CharMapRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Devuelve true si el reemplazo global está activado.
     */
    fun isReplacementEnabled(): Boolean =
        prefs.getBoolean(KEY_ENABLED, true)

    fun setReplacementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Si está activado, las teclas del teclado muestran directamente el
     * símbolo de reemplazo (por ejemplo "ɐ") en vez de la letra normal
     * ("a"). Si está desactivado, las teclas siempre muestran las letras
     * normales aunque el reemplazo esté activo al escribir.
     */
    fun isKeyPreviewEnabled(): Boolean =
        prefs.getBoolean(KEY_PREVIEW, false)

    fun setKeyPreviewEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREVIEW, enabled).apply()
    }

    /**
     * Carga el mapa de reemplazo guardado. Si no hay nada guardado todavía,
     * devuelve el preset "Al revés" como valor inicial por defecto.
     */
    fun loadMap(): LinkedHashMap<String, String> {
        val json = prefs.getString(KEY_MAP, null) ?: return LinkedHashMap(PRESET_UPSIDE_DOWN)
        return try {
            jsonToMap(json)
        } catch (e: Exception) {
            LinkedHashMap(PRESET_UPSIDE_DOWN)
        }
    }

    /**
     * Guarda el mapa de reemplazo completo, serializado como JSON.
     * Sobrescribe cualquier mapa anterior.
     */
    fun saveMap(map: Map<String, String>) {
        val json = mapToJson(map)
        prefs.edit().putString(KEY_MAP, json).apply()
    }

    private fun mapToJson(map: Map<String, String>): String {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    private fun jsonToMap(json: String): LinkedHashMap<String, String> {
        val obj = JSONObject(json)
        val result = LinkedHashMap<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            result[k] = obj.getString(k)
        }
        return result
    }

    companion object {
        private const val PREFS_NAME = "symbol_keyboard_prefs"
        private const val KEY_MAP = "char_map_json"
        private const val KEY_ENABLED = "replacement_enabled"
        private const val KEY_PREVIEW = "key_preview_enabled"

        /**
         * Preset "Al revés": letras minúsculas dadas vuelta usando glifos
         * Unicode que se asemejan a la letra rotada 180°.
         */
        val PRESET_UPSIDE_DOWN: LinkedHashMap<String, String> = linkedMapOf(
            "a" to "ɐ", "b" to "q", "c" to "ɔ", "d" to "p", "e" to "ǝ",
            "f" to "ɟ", "g" to "ƃ", "h" to "ɥ", "i" to "ᴉ", "j" to "ɾ",
            "k" to "ʞ", "l" to "ʃ", "m" to "ɯ", "n" to "u", "o" to "o",
            "p" to "d", "q" to "b", "r" to "ɹ", "s" to "s", "t" to "ʇ",
            "u" to "n", "v" to "ʌ", "w" to "ʍ", "x" to "x", "y" to "ʎ",
            "z" to "z"
        )

        /**
         * Preset "Leet speak": reemplazos numéricos clásicos.
         */
        val PRESET_LEET: LinkedHashMap<String, String> = linkedMapOf(
            "a" to "4", "b" to "8", "e" to "3", "g" to "9", "i" to "1",
            "l" to "1", "o" to "0", "s" to "5", "t" to "7", "z" to "2"
        )

        /**
         * Preset "Runas": letras latinas mapeadas a runas del Futhark antiguo
         * con sonido aproximado similar.
         */
        val PRESET_RUNES: LinkedHashMap<String, String> = linkedMapOf(
            "a" to "ᚨ", "b" to "ᛒ", "c" to "ᚲ", "d" to "ᛞ", "e" to "ᛖ",
            "f" to "ᚠ", "g" to "ᚷ", "h" to "ᚺ", "i" to "ᛁ", "j" to "ᛃ",
            "k" to "ᚲ", "l" to "ᛚ", "m" to "ᛗ", "n" to "ᚾ", "o" to "ᛟ",
            "p" to "ᛈ", "q" to "ᛢ", "r" to "ᚱ", "s" to "ᛋ", "t" to "ᛏ",
            "u" to "ᚢ", "v" to "ᚡ", "w" to "ᚹ", "x" to "ᚴᛋ", "y" to "ᚤ",
            "z" to "ᛉ"
        )

        /**
         * Preset "Círculos": letras encerradas en un círculo, usando el
         * bloque Unicode "Enclosed Alphanumerics" (U+24D0 en adelante para
         * minúsculas, U+24B6 en adelante para mayúsculas).
         */
        val PRESET_CIRCLES: LinkedHashMap<String, String> = run {
            val map = LinkedHashMap<String, String>()
            for (i in 0 until 26) {
                val lower = ('a' + i)
                val circledLower = (0x24D0 + i).toChar()
                map[lower.toString()] = circledLower.toString()
            }
            map
        }

        /**
         * Preset "Sin cambios": mapa vacío, efectivamente resetea el teclado
         * a su comportamiento normal sin reemplazos.
         */
        val PRESET_NONE: LinkedHashMap<String, String> = linkedMapOf()

        /**
         * Lista de presets disponibles para mostrar en la UI, junto con
         * un identificador legible.
         */
        fun availablePresets(): List<Pair<String, Map<String, String>>> = listOf(
            "upside_down" to PRESET_UPSIDE_DOWN,
            "leet" to PRESET_LEET,
            "runes" to PRESET_RUNES,
            "circles" to PRESET_CIRCLES,
            "none" to PRESET_NONE
        )
    }
}
