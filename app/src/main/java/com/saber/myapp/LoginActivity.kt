package com.saber.myapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnSignIn = findViewById(R.id.btnGoogleSignIn)
        progress = findViewById(R.id.loginProgress)

        findViewById<Button>(R.id.btnLoginCancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnSignIn.setOnClickListener { startGoogleSignIn() }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignIn.isEnabled = !loading
    }

    private fun startGoogleSignIn() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(SupabaseConfig.WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = CredentialManager.create(this@LoginActivity)
                    .getCredential(this@LoginActivity, request)

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val idToken = GoogleIdTokenCredential
                        .createFrom(credential.data).idToken
                    exchangeWithSupabase(idToken)
                } else {
                    fail("unsupported credential")
                }

            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Sign-in failed", e)
                fail(e.javaClass.simpleName)
            }
        }
    }

    private suspend fun exchangeWithSupabase(idToken: String) {
        val ok = withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(SupabaseConfig.GOOGLE_TOKEN_URL)
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SupabaseConfig.API_KEY)
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.doOutput = true

                val body = JSONObject()
                    .put("provider", "google")
                    .put("id_token", idToken)
                    .toString()
                    .toByteArray(Charsets.UTF_8)

                conn.outputStream.use { it.write(body) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val text = conn.inputStream.bufferedReader()
                        .use { it.readText() }
                    val json = JSONObject(text)

                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                        .putString("sb_access_token", json.optString("access_token"))
                        .putString("sb_refresh_token", json.optString("refresh_token"))
                        .putLong(
                            "sb_expires_at",
                            System.currentTimeMillis() / 1000 +
                                json.optLong("expires_in", 3600)
                        )
                        .apply()
                    true
                } else {
                    val err = conn.errorStream?.bufferedReader()
                        ?.use { it.readText() }
                    android.util.Log.e(
                        "LoginActivity",
                        "HTTP ${conn.responseCode}: $err"
                    )
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Exchange failed", e)
                false
            } finally {
                conn?.disconnect()
            }
        }

        if (ok) {
            setResult(RESULT_OK)
            finish()
        } else {
            fail("supabase")
        }
    }

    private fun fail(reason: String) {
        setLoading(false)
        Toast.makeText(
            this,
            "Login failed ($reason)",
            Toast.LENGTH_LONG
        ).show()
    }
}
