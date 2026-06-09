package com.example.myapplication

import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val BASE_URL = "http://172.24.27.137:8084/api/user"

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        callback: (Result<ApiResult>) -> Unit
    ) {
        val body = JSONObject()
            .put("username", username)
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword)

        postJson("$BASE_URL/register", body, callback)
    }

    fun login(username: String, password: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)

        postJson("$BASE_URL/login", body, callback)
    }

    private fun postJson(url: String, body: JSONObject, callback: (Result<ApiResult>) -> Unit) {
        Thread {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(body.toString())
                }

                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val responseJson = JSONObject(responseText)
                callback(
                    Result.success(
                        ApiResult(
                            success = responseJson.optBoolean("success"),
                            message = responseJson.optString("message", "请求失败")
                        )
                    )
                )
                connection.disconnect()
            } catch (ex: Exception) {
                callback(Result.failure(ex))
            }
        }.start()
    }
}

data class ApiResult(
    val success: Boolean,
    val message: String
)
