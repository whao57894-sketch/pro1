package com.example.myapplication.ui.statistics

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.R
import com.example.myapplication.YearlyReportActivity
import com.example.myapplication.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)

        setupTrendChart()
        setupCategoryChart()
        setupButtons()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadTrendData(6)
        loadCategoryData()
        generateInsights()
    }

    private fun generateInsights() {
        val phone = currentPhone() ?: return
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        ApiClient.getBookkeepingMonth(phone, currentMonth) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is JSONObject) {
                        val data = apiResult.data
                        val income = data.optDouble("incomeTotal", 0.0)
                        val expense = data.optDouble("expenseTotal", 0.0)
                        val balance = income - expense
                        val records = data.optJSONArray("records")
                        val recordCount = records?.length() ?: 0

                        val insights = buildString {
                            append("本月共记账 $recordCount 笔\n")

                            if (balance > 0) {
                                append("✅ 收支平衡良好，结余 ¥%.2f\n".format(balance))
                            } else if (balance < 0) {
                                append("⚠️ 支出超过收入 ¥%.2f\n".format(-balance))
                            }

                            if (expense > 0) {
                                val avgDaily = expense / LocalDate.now().dayOfMonth
                                append("📊 日均消费 ¥%.2f".format(avgDaily))
                            }
                        }

                        binding.textInsights.text = insights
                    }
                }
            }
        }
    }

    private fun setupTrendChart() {
        binding.spinnerTrendPeriod.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("近3个月", "近6个月", "近12个月")
        )
        binding.spinnerTrendPeriod.setSelection(1)
        binding.spinnerTrendPeriod.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val months = when (position) {
                    0 -> 3
                    1 -> 6
                    else -> 12
                }
                loadTrendData(months)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.chartTrend.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = Color.parseColor("#333333")
            }
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = Color.parseColor("#666666")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
            }
            xAxis.apply {
                textColor = Color.parseColor("#666666")
                setDrawGridLines(false)
            }
            animateX(800)
        }
    }

    private fun setupCategoryChart() {
        binding.editCategoryMonth.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
        binding.editCategoryMonth.setOnClickListener {
            // TODO: 添加月份选择器
            loadCategoryData()
        }

        binding.chartCategory.apply {
            description.isEnabled = false
            isRotationEnabled = true
            setDrawEntryLabels(false)
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = Color.parseColor("#333333")
            }
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 40f
            transparentCircleRadius = 45f
            animateY(1000)
        }
    }

    private fun setupButtons() {
        binding.buttonYearlyReport.setOnClickListener {
            startActivity(Intent(requireContext(), YearlyReportActivity::class.java))
        }

        binding.buttonExportData.setOnClickListener {
            showExportDialog()
        }

        binding.buttonCompareTrend.setOnClickListener {
            showCompareTrendDialog()
        }
    }

    private fun showExportDialog() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("导出为 Excel", "导出为 CSV", "导出为 TXT")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📤 导出数据")
            .setItems(options) { _, which ->
                val format = when (which) {
                    0 -> "Excel"
                    1 -> "CSV"
                    else -> "TXT"
                }
                exportCurrentMonthData(format)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportCurrentMonthData(format: String) {
        val phone = currentPhone() ?: return
        val month = binding.editCategoryMonth.text.toString().trim()

        if (!month.matches(Regex("^\\d{4}-\\d{2}$"))) {
            Toast.makeText(requireContext(), "请先选择有效月份", Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("导出中...")
            .setMessage("正在生成文件，请稍候...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        ApiClient.getBookkeepingMonth(phone, month) { result ->
            activity?.runOnUiThread {
                loadingDialog.dismiss()

                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is org.json.JSONObject) {
                        val records = apiResult.data.optJSONArray("records") ?: org.json.JSONArray()

                        if (records.length() == 0) {
                            Toast.makeText(requireContext(), "该月份没有记录", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        val filePath = when (format) {
                            "Excel" -> com.example.myapplication.utils.ExportUtil.exportToExcel(requireContext(), records, month)
                            "CSV" -> com.example.myapplication.utils.ExportUtil.exportToCSV(requireContext(), records, month)
                            else -> com.example.myapplication.utils.ExportUtil.exportToText(requireContext(), records, month)
                        }

                        if (filePath != null) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("✅ 导出成功")
                                .setMessage("文件已保存至：\n$filePath")
                                .setPositiveButton("确定", null)
                                .show()
                        } else {
                            Toast.makeText(requireContext(), "导出失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "获取数据失败", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(requireContext(), "网络错误：${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCompareTrendDialog() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val months = mutableListOf<String>()
        val currentDate = LocalDate.now()

        // 生成最近12个月的选项
        for (i in 0..11) {
            val date = currentDate.minusMonths(i.toLong())
            months.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")))
        }

        val selectedMonths = mutableListOf<String>()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📊 选择对比月份（至少选2个）")
            .setMultiChoiceItems(months.toTypedArray(), null) { _, which, isChecked ->
                if (isChecked) {
                    selectedMonths.add(months[which])
                } else {
                    selectedMonths.remove(months[which])
                }
            }
            .setPositiveButton("对比") { _, _ ->
                if (selectedMonths.size < 2) {
                    Toast.makeText(requireContext(), "请至少选择2个月份", Toast.LENGTH_SHORT).show()
                } else {
                    compareMonths(selectedMonths.sorted())
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun compareMonths(months: List<String>) {
        val phone = currentPhone() ?: return

        val message = buildString {
            append("正在对比以下月份：\n")
            months.forEach { append("• $it\n") }
        }

        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("加载中...")
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // 获取各月数据
        val results = mutableMapOf<String, Pair<Double, Double>>() // month -> (income, expense)
        var completed = 0

        months.forEach { month ->
            ApiClient.getBookkeepingMonth(phone, month) { result ->
                activity?.runOnUiThread {
                    result.onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is org.json.JSONObject) {
                            val income = apiResult.data.optDouble("incomeTotal", 0.0)
                            val expense = apiResult.data.optDouble("expenseTotal", 0.0)
                            results[month] = Pair(income, expense)
                        }
                    }

                    completed++
                    if (completed == months.size) {
                        loadingDialog.dismiss()
                        showCompareResult(results)
                    }
                }
            }
        }
    }

    private fun showCompareResult(results: Map<String, Pair<Double, Double>>) {
        val message = buildString {
            append("📊 收支对比结果\n")
            append("=".repeat(30) + "\n\n")

            results.entries.sortedBy { it.key }.forEach { (month, data) ->
                val (income, expense) = data
                val balance = income - expense
                val balanceEmoji = if (balance >= 0) "✅" else "⚠️"

                append("【$month】\n")
                append("  收入：¥%.2f\n".format(income))
                append("  支出：¥%.2f\n".format(expense))
                append("  结余：$balanceEmoji ¥%.2f\n".format(balance))
                append("\n")
            }

            // 计算平均值
            val avgIncome = results.values.map { it.first }.average()
            val avgExpense = results.values.map { it.second }.average()

            append("=".repeat(30) + "\n")
            append("平均收入：¥%.2f\n".format(avgIncome))
            append("平均支出：¥%.2f\n".format(avgExpense))
            append("平均结余：¥%.2f\n".format(avgIncome - avgExpense))
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("对比分析")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun loadTrendData(months: Int) {
        val phone = currentPhone() ?: return

        ApiClient.getMonthlyTrend(phone, months) { result ->
            activity?.runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONArray) {
                            renderTrendChart(apiResult.data)
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "加载趋势数据失败", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderTrendChart(data: JSONArray) {
        val incomeEntries = mutableListOf<Entry>()
        val expenseEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val month = item.optString("month", "")
            val income = item.optDouble("income", 0.0).toFloat()
            val expense = item.optDouble("expense", 0.0).toFloat()

            labels.add(month.substring(5))
            incomeEntries.add(Entry(i.toFloat(), income))
            expenseEntries.add(Entry(i.toFloat(), expense))
        }

        val incomeDataSet = LineDataSet(incomeEntries, "收入").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 3f
            circleRadius = 5f
            valueTextSize = 11f
            valueTextColor = Color.parseColor("#4CAF50")
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val expenseDataSet = LineDataSet(expenseEntries, "支出").apply {
            color = Color.parseColor("#F44336")
            setCircleColor(Color.parseColor("#F44336"))
            lineWidth = 3f
            circleRadius = 5f
            valueTextSize = 11f
            valueTextColor = Color.parseColor("#F44336")
            setDrawFilled(true)
            fillColor = Color.parseColor("#F44336")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.chartTrend.apply {
            this.data = LineData(incomeDataSet, expenseDataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
            }
            invalidate()
        }
    }

    private fun loadCategoryData() {
        val phone = currentPhone() ?: return
        val month = binding.editCategoryMonth.text.toString().trim()

        if (!month.matches(Regex("^\\d{4}-\\d{2}$"))) {
            return
        }

        ApiClient.getCategoryStatistics(phone, month) { result ->
            activity?.runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONArray) {
                            renderCategoryChart(apiResult.data)
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "加载分类数据失败", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderCategoryChart(data: JSONArray) {
        val entries = mutableListOf<PieEntry>()
        var total = 0.0

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val category = item.optString("category", "")
            val amount = item.optDouble("amount", 0.0).toFloat()
            total += amount
            entries.add(PieEntry(amount, category))
        }

        if (entries.isEmpty()) {
            binding.chartCategory.centerText = "暂无数据"
            binding.chartCategory.data = null
            binding.chartCategory.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "分类").apply {
            colors = listOf(
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"),
                Color.parseColor("#FFA07A"),
                Color.parseColor("#98D8C8"),
                Color.parseColor("#F7DC6F"),
                Color.parseColor("#BB8FCE"),
                Color.parseColor("#85C1E2")
            )
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
            selectionShift = 8f
        }

        binding.chartCategory.apply {
            setUsePercentValues(true)
            this.data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.chartCategory))
            }
            centerText = "总支出\n¥%.2f".format(total)
            invalidate()
        }
    }

    private fun currentPhone(): String? {
        return requireContext()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
