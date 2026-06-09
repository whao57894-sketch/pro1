package com.example.myapplication

import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val HOST = "http://172.24.27.137:8084/api"
    private const val USER_URL = "$HOST/user"
    private const val BOOKKEEPING_URL = "$HOST/bookkeeping"

    fun register(phone: String, password: String, confirmPassword: String, name: String, age: Int, occupation: String, gender: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("password", password)
            .put("confirmPassword", confirmPassword)
            .put("name", name)
            .put("age", age)
            .put("occupation", occupation)
            .put("gender", gender)
        request("POST", "$USER_URL/register", body, callback)
    }

    fun login(phone: String, password: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject().put("phone", phone).put("password", password)
        request("POST", "$USER_URL/login", body, callback)
    }

    fun getUser(phone: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$USER_URL/$phone", null, callback)
    }

    fun updateUser(phone: String, name: String, age: Int, occupation: String, gender: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("name", name)
            .put("age", age)
            .put("occupation", occupation)
            .put("gender", gender)
        request("PUT", "$USER_URL/$phone", body, callback)
    }

    fun getBookkeepingCount(phone: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$USER_URL/$phone/bookkeeping-count", null, callback)
    }

    fun getBookkeepingMonth(phone: String, month: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$BOOKKEEPING_URL?phone=$phone&month=$month", null, callback)
    }

    fun getBookkeepingRecord(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$BOOKKEEPING_URL/$id", null, callback)
    }

    fun saveBookkeepingRecord(id: Long?, phone: String, type: String, category: String, amount: String, recordDate: String, remark: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("type", type)
            .put("category", category)
            .put("amount", amount)
            .put("recordDate", recordDate)
            .put("remark", remark)
        if (id == null) {
            request("POST", BOOKKEEPING_URL, body, callback)
        } else {
            request("PUT", "$BOOKKEEPING_URL/$id", body, callback)
        }
    }

    fun deleteBookkeepingRecord(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("DELETE", "$BOOKKEEPING_URL/$id", null, callback)
    }

    fun recognizeBillImage(fileName: String, bytes: ByteArray, callback: (Result<ApiResult>) -> Unit) {
        Thread {
            try {
                callback(Result.success(sendMultipart("$BOOKKEEPING_URL/recognize", fileName, bytes)))
            } catch (ex: Exception) {
                callback(Result.failure(ex))
            }
        }.start()
    }

    private fun request(method: String, url: String, body: JSONObject?, callback: (Result<ApiResult>) -> Unit) {
        Thread {
            try {
                callback(Result.success(sendRequest(method, url, body)))
            } catch (ex: Exception) {
                callback(Result.failure(ex))
            }
        }.start()
    }

    private fun sendRequest(method: String, url: String, body: JSONObject?): ApiResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = body != null
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }
            return parseResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun sendMultipart(url: String, fileName: String, bytes: ByteArray): ApiResult {
        val boundary = "----BookkeepingBoundary${System.currentTimeMillis()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(connection.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                out.writeBytes("Content-Type: image/*\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            return parseResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(connection: HttpURLConnection): ApiResult {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        val responseJson = JSONObject(responseText)
        return ApiResult(
            success = responseJson.optBoolean("success"),
            message = responseJson.optString("message", "请求失败"),
            data = responseJson.opt("data")
        )
    }
}

data class ApiResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
