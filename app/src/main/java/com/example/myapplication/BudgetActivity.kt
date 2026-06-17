package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityBudgetBinding
import com.example.myapplication.databinding.DialogBudgetEditBinding
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.editMonth.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
        binding.buttonQuery.setOnClickListener { loadBudgets() }
        binding.fabAddBudget.setOnClickListener { showBudgetDialog(null) }

        loadBudgets()
    }

    private fun loadBudgets() {
        val phone = currentPhone() ?: return
        val month = binding.editMonth.text.toString().trim()

        if (!month.matches(Regex("^\\d{4}-\\d{2}$"))) {
            Toast.makeText(this, "月份格式应为 yyyy-MM", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.getBudgets(phone, month) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONArray) {
                            renderBudgets(apiResult.data)
                        } else {
                            Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderBudgets(budgets: JSONArray) {
        binding.budgetList.removeAllViews()

        if (budgets.length() == 0) {
            val empty = TextView(this).apply {
                text = "暂无预算设置\n点击右下角按钮添加预算"
                textSize = 16f
                setTextColor(Color.parseColor("#999999"))
                setPadding(0, 48, 0, 48)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            binding.budgetList.addView(empty)
            return
        }

        for (index in 0 until budgets.length()) {
            val budget = budgets.getJSONObject(index)
            binding.budgetList.addView(createBudgetView(budget))
        }
    }

    private fun createBudgetView(budget: JSONObject): View {
        val card = layoutInflater.inflate(R.layout.item_budget_card, binding.budgetList, false)

        val category = card.findViewById<TextView>(R.id.textCategory)
        val amount = card.findViewById<TextView>(R.id.textAmount)
        val spent = card.findViewById<TextView>(R.id.textSpent)
        val progress = card.findViewById<ProgressBar>(R.id.progressBar)
        val btnEdit = card.findViewById<MaterialButton>(R.id.buttonEdit)
        val btnDelete = card.findViewById<MaterialButton>(R.id.buttonDelete)

        val budgetAmount = budget.optDouble("amount", 0.0)
        val currentSpent = budget.optDouble("currentSpent", 0.0)
        val percentage = if (budgetAmount > 0) ((currentSpent / budgetAmount) * 100).toInt() else 0

        category.text = budget.optString("category")
        amount.text = "预算：¥%.2f".format(budgetAmount)
        spent.text = "已用：¥%.2f (${percentage}%%)".format(currentSpent)

        progress.max = 100
        progress.progress = percentage

        // 预算预警颜色
        val color = when {
            percentage >= 100 -> Color.parseColor("#F44336") // 红色：超支
            percentage >= 80 -> Color.parseColor("#FF9800") // 橙色：警告
            else -> Color.parseColor("#4CAF50") // 绿色：正常
        }
        progress.progressDrawable.setTint(color)

        btnEdit.setOnClickListener { showBudgetDialog(budget) }
        btnDelete.setOnClickListener { deleteBudget(budget.optLong("id")) }

        return card
    }

    private fun showBudgetDialog(budget: JSONObject?) {
        val dialogBinding = DialogBudgetEditBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        if (budget != null) {
            dialogBinding.editCategory.setText(budget.optString("category"))
            dialogBinding.editAmount.setText(budget.optDouble("amount").toString())
        }

        dialogBinding.buttonCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.buttonSave.setOnClickListener {
            val category = dialogBinding.editCategory.text.toString().trim()
            val amountText = dialogBinding.editAmount.text.toString().trim()

            if (category.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "请输入正确的金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveBudget(budget?.optLong("id"), category, amount)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveBudget(id: Long?, category: String, amount: Double) {
        val phone = currentPhone() ?: return
        val month = binding.editMonth.text.toString().trim()

        ApiClient.saveBudget(id, phone, category, amount, month) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) loadBudgets()
                    }
                    .onFailure {
                        Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun deleteBudget(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除预算")
            .setMessage("确定要删除这个预算设置吗？")
            .setPositiveButton("删除") { _, _ ->
                ApiClient.deleteBudget(id) { result ->
                    runOnUiThread {
                        result
                            .onSuccess { apiResult ->
                                Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                                if (apiResult.success) loadBudgets()
                            }
                            .onFailure {
                                Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun currentPhone(): String? {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)
    }
}
