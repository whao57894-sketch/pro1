package com.example.myapplication.ui.dashboard

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.time.LocalDate

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var recordDate: String = LocalDate.now().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setupForm()
        return binding.root
    }

    private fun setupForm() {
        // 设置收支类型下拉框
        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("支出", "收入")
        )

        // 监听类型切换，更新类目建议
        binding.spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCategorySuggestions()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 设置日期按钮
        binding.buttonDate.text = "📅 ${formatDate(recordDate)}"
        binding.buttonDate.setOnClickListener { pickDate() }

        // 保存按钮
        binding.buttonSaveRecord.setOnClickListener { saveRecord() }

        // 上传账单图片
        binding.buttonUploadBill.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, 1001)
        }

        // 监听备注变化，智能推荐类目
        binding.editRemark.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateCategorySuggestions()
            }
        })

        // 监听金额变化，智能推荐类目
        binding.editAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateCategorySuggestions()
            }
        })

        // 类目输入框点击显示建议
        binding.editCategory.setOnClickListener {
            showCategorySuggestionDialog()
        }
    }

    private fun updateCategorySuggestions() {
        val type = binding.spinnerType.selectedItem.toString()
        val remark = binding.editRemark.text.toString().trim()
        val amountStr = binding.editAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        // 获取智能推荐
        val suggestions = com.example.myapplication.utils.CategorySuggestion.smartSuggest(remark, amount, type)

        // 如果类目为空且有推荐，自动填充第一个
        if (binding.editCategory.text.isNullOrBlank() && suggestions.isNotEmpty()) {
            val firstSuggestion = suggestions[0]
            val emoji = com.example.myapplication.utils.CategorySuggestion.getCategoryEmoji(firstSuggestion)
            binding.editCategory.hint = "$emoji $firstSuggestion (智能推荐)"
        }
    }

    private fun showCategorySuggestionDialog() {
        val type = binding.spinnerType.selectedItem.toString()
        val remark = binding.editRemark.text.toString().trim()
        val amountStr = binding.editAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        // 获取智能推荐和全部类目
        val suggestions = com.example.myapplication.utils.CategorySuggestion.smartSuggest(remark, amount, type)
        val allCategories = com.example.myapplication.utils.CategorySuggestion.getAllCategories(type)

        // 构建显示列表：推荐 + 全部
        val displayList = mutableListOf<String>()
        if (suggestions.isNotEmpty()) {
            displayList.add("--- 💡 智能推荐 ---")
            suggestions.forEach { category ->
                val emoji = com.example.myapplication.utils.CategorySuggestion.getCategoryEmoji(category)
                displayList.add("$emoji $category")
            }
            displayList.add("--- 📋 全部类目 ---")
        }
        allCategories.forEach { category ->
            val emoji = com.example.myapplication.utils.CategorySuggestion.getCategoryEmoji(category)
            displayList.add("$emoji $category")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("选择类目")
            .setItems(displayList.toTypedArray()) { _, which ->
                val selected = displayList[which]
                // 跳过分隔行
                if (!selected.startsWith("---")) {
                    // 提取类目名称（去除 emoji）
                    val category = selected.substringAfter(" ")
                    binding.editCategory.setText(category)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatDate(date: String): String {
        return try {
            val localDate = LocalDate.parse(date)
            "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日"
        } catch (e: Exception) {
            date
        }
    }

    private fun pickDate() {
        val date = LocalDate.parse(recordDate)
        DatePickerDialog(requireContext(), { _, year, month, day ->
            recordDate = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.buttonDate.text = "📅 ${formatDate(recordDate)}"
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun saveRecord() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Snackbar.make(binding.root, "请先登录", Snackbar.LENGTH_LONG).show()
            return
        }

        val type = binding.spinnerType.selectedItem.toString()
        val category = binding.editCategory.text.toString().trim()
        val amount = binding.editAmount.text.toString().trim()
        val remark = binding.editRemark.text.toString().trim()

        // 验证输入
        if (category.isEmpty()) {
            binding.editCategory.error = "请输入类目"
            binding.editCategory.requestFocus()
            return
        }

        if (amount.isEmpty()) {
            binding.editAmount.error = "请输入金额"
            binding.editAmount.requestFocus()
            return
        }

        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            binding.editAmount.error = "请输入有效金额"
            binding.editAmount.requestFocus()
            return
        }

        // 禁用按钮防止重复提交
        binding.buttonSaveRecord.isEnabled = false
        binding.buttonSaveRecord.text = "保存中..."

        ApiClient.saveBookkeepingRecord(null, phone, type, category, amount, recordDate, remark) { result ->
            activity?.runOnUiThread {
                binding.buttonSaveRecord.isEnabled = true
                binding.buttonSaveRecord.text = "💾 保存记账"

                result
                    .onSuccess { apiResult ->
                        if (apiResult.success) {
                            Snackbar.make(binding.root, "✅ 保存成功！", Snackbar.LENGTH_LONG)
                                .setAction("查看") {
                                    // 切换到首页查看记录
                                    activity?.supportFragmentManager?.beginTransaction()?.apply {
                                        // 这里可以触发切换到首页的逻辑
                                    }
                                }
                                .show()
                            clearForm()
                        } else {
                            Snackbar.make(binding.root, "保存失败：${apiResult.message}", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    .onFailure {
                        Snackbar.make(binding.root, "网络错误：${it.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun clearForm() {
        binding.editCategory.text?.clear()
        binding.editAmount.text?.clear()
        binding.editRemark.text?.clear()
        recordDate = LocalDate.now().toString()
        binding.buttonDate.text = "📅 ${formatDate(recordDate)}"
        binding.spinnerType.setSelection(0) // 重置为支出
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode == 1001 && uri != null) {
            recognizeImage(uri)
        }
    }

    private fun recognizeImage(uri: Uri) {
        val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            Snackbar.make(binding.root, "图片读取失败", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 显示加载状态
        binding.buttonUploadBill.isEnabled = false
        binding.buttonUploadBill.text = "🔍 识别中..."

        ApiClient.recognizeBillImage("alipay_bill.jpg", bytes) { result ->
            activity?.runOnUiThread {
                binding.buttonUploadBill.isEnabled = true
                binding.buttonUploadBill.text = "📸 上传账单图片"

                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONObject) {
                            Snackbar.make(binding.root, "✅ 识别成功！已自动填充信息", Snackbar.LENGTH_LONG).show()
                            fillFromRecognize(apiResult.data)
                        } else {
                            Snackbar.make(binding.root, "识别失败：${apiResult.message}", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    .onFailure {
                        Snackbar.make(binding.root, "识别失败：${it.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun fillFromRecognize(data: JSONObject) {
        val type = data.optString("type", "支出")
        binding.spinnerType.setSelection(if (type == "收入") 1 else 0)

        val category = data.optString("category")
        if (category.isNotBlank()) {
            binding.editCategory.setText(category)
        }

        val amount = data.optString("amount")
        if (amount.isNotBlank() && amount != "0" && amount != "0.00") {
            binding.editAmount.setText(amount)
        }

        val date = data.optString("recordDate")
        if (date.isNotBlank()) {
            recordDate = date
            binding.buttonDate.text = "📅 ${formatDate(recordDate)}"
        }

        val remark = data.optString("remark")
        if (remark.isNotBlank()) {
            binding.editRemark.setText(remark)
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
