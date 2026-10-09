package com.example.ubicasure.models

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** El contrato interno de analysis queda pendiente; se conserva sin convertirlo. */
data class AlertResponse(
    val message: String? = null,
    val alertId: Long? = null,
    val images: List<String> = emptyList(),
    val analysis: JsonElement? = null,
    val imageWasAttached: Boolean = false,
    val responseWasReadable: Boolean = true
) {
    val hasAnalysis: Boolean
        get() = analysis.hasContent()
}

private fun JsonElement?.hasContent(): Boolean = when {
    this == null || isJsonNull -> false
    isJsonArray -> asJsonArray.any { it.hasContent() }
    isJsonObject -> asJsonObject.entrySet().isNotEmpty()
    isJsonPrimitive && asJsonPrimitive.isString -> asString.isNotBlank()
    else -> true
}

object AlertResponseParser {
    private val gson = Gson()

    fun parse(body: String, imageWasAttached: Boolean): AlertResponse {
        val root = runCatching {
            gson.fromJson(body, JsonElement::class.java)
                ?.takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull()

        // Un 2xx confirma el registro aunque la respuesta no sea JSON válido.
        // No convertir ese caso en un error que invite a duplicar la alerta.
        if (root == null) {
            return AlertResponse(
                imageWasAttached = imageWasAttached,
                responseWasReadable = false
            )
        }

        return AlertResponse(
            message = root.stringValue("message"),
            alertId = root.stringValue("alertId")?.toLongOrNull(),
            images = root.get("images")?.takeIf { it.isJsonArray }
                ?.asJsonArray?.mapNotNull {
                    it.takeIf { value ->
                        value.isJsonPrimitive && value.asJsonPrimitive.isString
                    }?.asString
                }.orEmpty(),
            analysis = root.get("analysis")?.takeUnless { it.isJsonNull },
            imageWasAttached = imageWasAttached
        )
    }

    private fun JsonObject.stringValue(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString
}
