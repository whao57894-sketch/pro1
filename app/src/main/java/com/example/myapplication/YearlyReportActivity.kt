package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityYearlyReportBinding
import org.json.JSONObject
import java.time.LocalDate

class YearlyReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYearlyReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYearlyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val currentYear = LocalDate.now().year
        binding.editYear.setText(currentYear.toString())
        binding.buttonQuery.setOnClickListener { loadReport() }

        loadReport()
    }

    private fun loadReport() {
        val phone = currentPhone() ?: return
        val yearText = binding.editYear.text.toString().trim()
        val year = yearText.toIntOrNull()

        if (year == null || year < 2000 || year > 2100) {
            Toast.makeText(this, "请输入正确的年份", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.getYearlyReport(phone, year) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONObject) {
                            renderReport(apiResult.data)
                        } else {
                            Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        Toast.makeText(this, "加载年度报告失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderReport(data: JSONObject) {
        val totalIncome = data.optDouble("totalIncome", 0.0)
        val totalExpense = data.optDouble("totalExpense", 0.0)
        val savingsRate = data.optDouble("savingsRate", 0.0)
        val recordCount = data.optInt("recordCount", 0)
        val topCategory = data.optString("topExpenseCategory", "无")
        val topAmount = data.optDouble("topExpenseAmount", 0.0)
        val avgMonthlyIncome = data.optDouble("avgMonthlyIncome", 0.0)
        val avgMonthlyExpense = data.optDouble("avgMonthlyExpense", 0.0)

        binding.textTotalIncome.text = "¥%.2f".format(totalIncome)
        binding.textTotalExpense.text = "¥%.2f".format(totalExpense)
        binding.textNetSavings.text = "¥%.2f".format(totalIncome - totalExpense)
        binding.textSavingsRate.text = "%.1f%%".format(savingsRate)
        binding.textRecordCount.text = "$recordCount 笔"
        binding.textTopCategory.text = "$topCategory (¥%.2f)".format(topAmount)
        binding.textAvgIncome.text = "¥%.2f".format(avgMonthlyIncome)
        binding.textAvgExpense.text = "¥%.2f".format(avgMonthlyExpense)

        // 设置储蓄率颜色
        val color = when {
            savingsRate >= 30 -> Color.parseColor("#4CAF50")
            savingsRate >= 10 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        }
        binding.textSavingsRate.setTextColor(color)
    }

    private fun currentPhone(): String? {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)
    }
}
