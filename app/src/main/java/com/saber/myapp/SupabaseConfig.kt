package com.saber.myapp

object SupabaseConfig {

    const val PROJECT_URL =
        "https://gmelnyxzsbwbsmhgrhhu.supabase.co"

    const val EXTRACT_DATE_URL =
        "$PROJECT_URL/functions/v1/extract-date"

    const val GOOGLE_TOKEN_URL =
        "$PROJECT_URL/auth/v1/token?grant_type=id_token"

    // مفتاح Supabase العام (publishable)
    const val API_KEY =
        "sb_publishable_6W2yXX7rchLZncnfc3Qbog_3rDsz3GE"

    // Web Client ID من Google Cloud
    const val WEB_CLIENT_ID =
        "846044207958-tdk2v0fq1bu25p75kjksakmm8dcaf4h1.apps.googleusercontent.com"

    // تجديد التوكن بصمت إذا قارب على الانتهاء
    fun refreshIfNeeded(context: android.content.Context) {
        val prefs = context.getSharedPreferences(
            "app_prefs",
            android.content.Context.MODE_PRIVATE
        )
        val refresh = prefs.getString("sb_refresh_token", null)
        if (refresh.isNullOrEmpty()) return

        val expiresAt = prefs.getLong("sb_expires_at", 0L)
        val now = System.currentTimeMillis() / 1000
        if (expiresAt - now > 60) return

        var conn: java.net.HttpURLConnection? = null
        try {
            conn = java.net.URL(
                "$PROJECT_URL/auth/v1/token?grant_type=refresh_token"
            ).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", API_KEY)
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true

            val body = org.json.JSONObject()
                .put("refresh_token", refresh)
                .toString()
                .toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(body) }

            if (conn.responseCode == 200) {
                val json = org.json.JSONObject(
                    conn.inputStream.bufferedReader().use { it.readText() }
                )
                prefs.edit()
                    .putString("sb_access_token", json.optString("access_token"))
                    .putString("sb_refresh_token", json.optString("refresh_token"))
                    .putLong(
                        "sb_expires_at",
                        now + json.optLong("expires_in", 3600)
                    )
                    .apply()
            } else if (conn.responseCode == 400 || conn.responseCode == 401) {
                // الجلسة ملغاة: نمسحها ليُطلب الدخول من جديد
                prefs.edit()
                    .remove("sb_access_token")
                    .remove("sb_refresh_token")
                    .remove("sb_expires_at")
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseConfig", "refresh failed", e)
        } finally {
            conn?.disconnect()
        }
    }
}
