package com.example.myapplication

import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val HOST = "http://127.0.0.1:8084/api"
    private const val USER_URL = "$HOST/user"
    private const val BOOKKEEPING_URL = "$HOST/bookkeeping"
    private const val BUDGET_URL = "$HOST/budget"
    private const val DEBT_URL = "$HOST/debt"
    private const val RECURRING_URL = "$HOST/recurring"
    private const val ACCOUNT_URL = "$HOST/account"
    private const val STATISTICS_URL = "$HOST/statistics"

    // 存储 token
    var authToken: String? = null

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

    // ===== 预算管理 API =====
    fun getBudgets(phone: String, month: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$BUDGET_URL?phone=$phone&month=$month", null, callback)
    }

    fun saveBudget(id: Long?, phone: String, category: String, amount: Double, month: String, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("category", category)
            .put("amount", amount)
            .put("month", month)
        if (id == null) {
            request("POST", BUDGET_URL, body, callback)
        } else {
            request("PUT", "$BUDGET_URL/$id", body, callback)
        }
    }

    fun deleteBudget(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("DELETE", "$BUDGET_URL/$id", null, callback)
    }

    // ===== 债务管理 API =====
    fun getDebts(phone: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$DEBT_URL?phone=$phone", null, callback)
    }

    fun saveDebt(id: Long?, phone: String, type: String, counterparty: String, amount: Double, debtDate: String, dueDate: String?, remark: String?, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("type", type)
            .put("counterparty", counterparty)
            .put("amount", amount)
            .put("debtDate", debtDate)
        if (dueDate != null) body.put("dueDate", dueDate)
        if (remark != null) body.put("remark", remark)

        if (id == null) {
            request("POST", DEBT_URL, body, callback)
        } else {
            request("PUT", "$DEBT_URL/$id", body, callback)
        }
    }

    fun repayDebt(id: Long, amount: Double, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject().put("amount", amount)
        request("POST", "$DEBT_URL/$id/repay", body, callback)
    }

    fun deleteDebt(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("DELETE", "$DEBT_URL/$id", null, callback)
    }

    // ===== 周期性账单 API =====
    fun getRecurringBills(phone: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$RECURRING_URL?phone=$phone", null, callback)
    }

    fun saveRecurringBill(id: Long?, phone: String, type: String, category: String, amount: Double, frequency: String, startDate: String, dayOfMonth: Int?, dayOfWeek: Int?, remark: String?, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("type", type)
            .put("category", category)
            .put("amount", amount)
            .put("frequency", frequency)
            .put("startDate", startDate)
        if (dayOfMonth != null) body.put("dayOfMonth", dayOfMonth)
        if (dayOfWeek != null) body.put("dayOfWeek", dayOfWeek)
        if (remark != null) body.put("remark", remark)

        if (id == null) {
            request("POST", RECURRING_URL, body, callback)
        } else {
            request("PUT", "$RECURRING_URL/$id", body, callback)
        }
    }

    fun toggleRecurringBill(id: Long, isActive: Boolean, callback: (Result<ApiResult>) -> Unit) {
        request("POST", "$RECURRING_URL/$id/toggle?active=$isActive", null, callback)
    }

    fun deleteRecurringBill(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("DELETE", "$RECURRING_URL/$id", null, callback)
    }

    // ===== 账户管理 API =====
    fun getAccounts(phone: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$ACCOUNT_URL?phone=$phone", null, callback)
    }

    fun saveAccount(id: Long?, phone: String, accountName: String, accountType: String, balance: Double, callback: (Result<ApiResult>) -> Unit) {
        val body = JSONObject()
            .put("phone", phone)
            .put("accountName", accountName)
            .put("accountType", accountType)
            .put("balance", balance)
        if (id == null) {
            request("POST", ACCOUNT_URL, body, callback)
        } else {
            request("PUT", "$ACCOUNT_URL/$id", body, callback)
        }
    }

    fun deleteAccount(id: Long, callback: (Result<ApiResult>) -> Unit) {
        request("DELETE", "$ACCOUNT_URL/$id", null, callback)
    }

    // ===== 统计分析 API =====
    fun getMonthlyTrend(phone: String, months: Int, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$STATISTICS_URL/trend?phone=$phone&months=$months", null, callback)
    }

    fun getCategoryStatistics(phone: String, month: String, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$STATISTICS_URL/category?phone=$phone&month=$month", null, callback)
    }

    fun getYearlyReport(phone: String, year: Int, callback: (Result<ApiResult>) -> Unit) {
        request("GET", "$STATISTICS_URL/yearly?phone=$phone&year=$year", null, callback)
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

            // 添加 Authorization 请求头
            authToken?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
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

            // 添加 Authorization 请求头
            authToken?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
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

        // 如果响应包含 token，保存它
        if (responseJson.has("token")) {
            authToken = responseJson.optString("token")
        }

        return ApiResult(
            success = responseJson.optBoolean("success"),
            message = responseJson.optString("message", "请求失败"),
            data = responseJson.opt("data"),
            token = responseJson.optString("token", null)
        )
    }
}

data class ApiResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val token: String? = null
)
