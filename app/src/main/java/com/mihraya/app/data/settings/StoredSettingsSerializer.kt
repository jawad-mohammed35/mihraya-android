package com.mihraya.app.data.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/** Missing script fields in an existing file meant Uthmani before 1.5.0.
 * New installations use DataStore's IndoPak default without decoding a file.
 * Explicit choices, including all other reader preferences, pass through unchanged.
 */
internal object StoredSettingsSerializer : JsonTransformingSerializer<AppSettings>(AppSettings.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val settings = element as? JsonObject ?: return element
        val reader = settings["reader"] as? JsonObject ?: JsonObject(emptyMap())
        if ("script" in reader) return element
        return JsonObject(settings + ("reader" to JsonObject(reader + ("script" to JsonPrimitive("Uthmani")))))
    }
}
