package com.example.myapplication.ui.home

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.BookkeepingEditActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var allRecords = JSONArray()
    private var displayedRecords = JSONArray()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.editMonth.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
        binding.buttonQuery.setOnClickListener { loadRecords() }

        // 使用 FAB 替代按钮
        binding.fabAddRecord.setOnClickListener {
            startActivity(Intent(requireContext(), BookkeepingEditActivity::class.java))
        }

        // 快捷功能按钮
        binding.btnBudget.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.myapplication.BudgetActivity::class.java))
        }

        binding.btnDebt.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.myapplication.DebtActivity::class.java))
        }

        binding.btnReport.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.myapplication.YearlyReportActivity::class.java))
        }

        binding.btnSearch.setOnClickListener {
            showSearchDialog()
        }

        // 设置问候语
        updateGreeting()

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            loadRecords()
        }

        loadRecords()
        return binding.root
    }

    private fun updateGreeting() {
        val hour = java.time.LocalTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "☀️ 早上好"
            in 12..13 -> "🌤️ 中午好"
            in 14..17 -> "🌞 下午好"
            in 18..23 -> "🌙 晚上好"
            else -> "🌃 夜深了"
        }
        binding.textGreeting.text = "$greeting，开始记账吧～"
    }

    private fun showSearchDialog() {
        val categories = mutableSetOf<String>()
        for (i in 0 until allRecords.length()) {
            val record = allRecords.getJSONObject(i)
            categories.add(record.optString("category"))
        }

        val categoryArray = categories.toTypedArray()
        val selectedCategories = BooleanArray(categoryArray.size)

        AlertDialog.Builder(requireContext())
            .setTitle("🔍 筛选类目")
            .setMultiChoiceItems(categoryArray, selectedCategories) { _, which, isChecked ->
                selectedCategories[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ ->
                val selected = categoryArray.filterIndexed { index, _ -> selectedCategories[index] }
                filterRecords(selected)
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除筛选") { _, _ ->
                displayedRecords = JSONArray(allRecords.toString())
                renderRecords(displayedRecords)
            }
            .show()
    }

    private fun filterRecords(categories: List<String>) {
        if (categories.isEmpty()) {
            displayedRecords = JSONArray(allRecords.toString())
        } else {
            displayedRecords = JSONArray()
            for (i in 0 until allRecords.length()) {
                val record = allRecords.getJSONObject(i)
                if (categories.contains(record.optString("category"))) {
                    displayedRecords.put(record)
                }
            }
        }
        renderRecords(displayedRecords)

        if (displayedRecords.length() == 0) {
            Snackbar.make(binding.root, "没有符合条件的记录", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "筛选出 ${displayedRecords.length()} 条记录", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadRecords()
        }
    }

    private fun loadRecords() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            binding.listRecords.removeAllViews()
            binding.textIncomeTotal.text = "0.00"
            binding.textExpenseTotal.text = "0.00"
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val month = binding.editMonth.text.toString().trim()
        if (!month.matches(Regex("^\\d{4}-\\d{2}$"))) {
            Toast.makeText(requireContext(), "月份格式应为 yyyy-MM", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.getBookkeepingMonth(phone, month) { result ->
            activity?.runOnUiThread {
                binding.swipeRefresh.isRefreshing = false
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONObject) {
                            renderMonth(apiResult.data)
                        } else {
                            Toast.makeText(requireContext(), apiResult.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderMonth(data: JSONObject) {
        val income = data.optString("incomeTotal", "0.00")
        val expense = data.optString("expenseTotal", "0.00")

        binding.textIncomeTotal.text = "¥ $income"
        binding.textExpenseTotal.text = "¥ $expense"

        // 添加数字动画
        animateValue(binding.textIncomeTotal, income.toDoubleOrNull() ?: 0.0)
        animateValue(binding.textExpenseTotal, expense.toDoubleOrNull() ?: 0.0)

        allRecords = data.optJSONArray("records") ?: JSONArray()
        displayedRecords = JSONArray(allRecords.toString())
        renderRecords(displayedRecords)
    }

    private fun animateValue(textView: TextView, targetValue: Double) {
        val animator = ObjectAnimator.ofFloat(0f, targetValue.toFloat())
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            textView.text = "¥ %.2f".format(value)
        }
        animator.start()
    }

    private fun renderRecords(records: JSONArray) {
        binding.listRecords.removeAllViews()
        if (records.length() == 0) {
            val empty = TextView(requireContext()).apply {
                text = "暂无记账记录"
                textSize = 16f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, 24, 0, 24)
            }
            binding.listRecords.addView(empty)
            return
        }

        for (index in 0 until records.length()) {
            val record = records.getJSONObject(index)
            binding.listRecords.addView(createRecordView(record))
        }
    }

    private fun createRecordView(record: JSONObject): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)
        }

        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val type = record.optString("type")
        val isIncome = type == "收入"
        val emoji = if (isIncome) "💰" else "💸"
        val amountColor = if (isIncome) "#4CAF50" else "#FF5252"

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val title = TextView(context).apply {
            text = "$emoji ${record.optString("category")}"
            textSize = 18f
            setTextColor(Color.parseColor("#1A1A1A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val amount = TextView(context).apply {
            text = "${if (isIncome) "+" else "-"}¥${record.optString("amount")}"
            textSize = 20f
            setTextColor(Color.parseColor(amountColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        header.addView(title)
        header.addView(amount)

        val detail = TextView(context).apply {
            text = "📅 ${record.optString("recordDate")}"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 12, 0, 0)
        }

        val remark = TextView(context).apply {
            val remarkText = record.optString("remark")
            text = if (remarkText.isNotBlank()) "📝 $remarkText" else ""
            textSize = 14f
            setTextColor(Color.parseColor("#777777"))
            visibility = if (remarkText.isNullOrBlank()) View.GONE else View.VISIBLE
            setPadding(0, 8, 0, 0)
        }

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val edit = Button(context).apply {
            text = "✏️ 编辑"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            setTextColor(Color.parseColor("#1976D2"))
            setOnClickListener {
                startActivity(Intent(context, BookkeepingEditActivity::class.java).putExtra("record_id", record.optLong("id")))
            }
        }

        val delete = Button(context).apply {
            text = "🗑️ 删除"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f).apply {
                setMargins(12, 0, 0, 0)
            }
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            setTextColor(Color.parseColor("#D32F2F"))
            setOnClickListener {
                showDeleteDialog(record.optLong("id"))
            }
        }

        actions.addView(edit)
        actions.addView(delete)
        box.addView(header)
        box.addView(detail)
        box.addView(remark)
        box.addView(actions)
        card.addView(box)

        // 添加淡入动画
        card.alpha = 0f
        card.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 添加长按提示
        card.setOnLongClickListener {
            Snackbar.make(binding.root, "💡 提示：向左滑动可快速删除", Snackbar.LENGTH_SHORT).show()
            true
        }

        return card
    }

    private fun showDeleteDialog(id: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteRecord(id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteRecord(id: Long) {
        ApiClient.deleteBookkeepingRecord(id) { result ->
            activity?.runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success) {
                            Snackbar.make(binding.root, "✅ 删除成功", Snackbar.LENGTH_SHORT).show()
                            loadRecords()
                        } else {
                            Snackbar.make(binding.root, "删除失败：${apiResult.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        Snackbar.make(binding.root, "网络错误：${it.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
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
