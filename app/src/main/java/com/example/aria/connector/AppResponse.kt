package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Sealed result type returned by every [AppConnector.execute] call.
 */
@Parcelize
sealed class AppResponse : Parcelable {

    abstract fun toJson(): String

    // ── Success ────────────────────────────────────────────────────────────────

    @Parcelize
    data class Success(
        val data: Map<String, String> = emptyMap(),
    ) : AppResponse() {
        operator fun get(key: String): String? = data[key]
        fun getOrEmpty(key: String): String = data[key] ?: ""
        fun toUserString(): String = data["message"] ?: data["track"] ?: "Done."

        override fun toJson(): String = JSONObject()
            .put("status", "success")
            .put("data", JSONObject(data))
            .toString()
    }

    // ── Error ──────────────────────────────────────────────────────────────────

    @Parcelize
    data class Error(
        val code: ErrorCode,
        val message: String,
    ) : AppResponse() {
        enum class ErrorCode {
            APP_NOT_CONNECTED,
            APP_NOT_FOUND,
            COMMAND_FAILED,
            PERMISSION_DENIED,
            TIMEOUT,
            UNKNOWN,
        }

        override fun toJson(): String = JSONObject()
            .put("status", "error")
            .put("code", code.name)
            .put("message", message)
            .toString()
    }

    // ── Not Supported ─────────────────────────────────────────────────────────

    @Parcelize
    data object NotSupported : AppResponse() {
        override fun toJson(): String = JSONObject()
            .put("status", "not_supported")
            .toString()
    }

    companion object {
        fun fromJson(json: String): AppResponse {
            return try {
                val obj = JSONObject(json)
                when (obj.getString("status")) {
                    "success" -> {
                        val dataObj = obj.optJSONObject("data") ?: JSONObject()
                        val map = buildMap<String, String> {
                            dataObj.keys().forEach { put(it, dataObj.getString(it)) }
                        }
                        Success(map)
                    }
                    "error" -> Error(
                        code = runCatching {
                            Error.ErrorCode.valueOf(obj.getString("code"))
                        }.getOrDefault(Error.ErrorCode.UNKNOWN),
                        message = obj.optString("message", "Unknown error"),
                    )
                    "not_supported" -> NotSupported
                    else -> Error(Error.ErrorCode.UNKNOWN, "Unrecognized status in response")
                }
            } catch (e: Exception) {
                Error(Error.ErrorCode.UNKNOWN, "Failed to parse response: ${e.message}")
            }
        }

        /** Convenience factory — used on the app side to return simple text results. */
        fun success(message: String, vararg extras: Pair<String, String>): AppResponse =
            Success(mapOf("message" to message, *extras))

        fun error(message: String, code: Error.ErrorCode = Error.ErrorCode.COMMAND_FAILED): AppResponse =
            Error(code, message)

        val notSupported: AppResponse = NotSupported
    }
}
