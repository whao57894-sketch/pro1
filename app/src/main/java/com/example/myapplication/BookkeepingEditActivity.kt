package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityBookkeepingEditBinding
import org.json.JSONObject
import java.time.LocalDate

class BookkeepingEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookkeepingEditBinding
    private var recordId: Long = -1
    private var recordDate: String = LocalDate.now().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookkeepingEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordId = intent.getLongExtra("record_id", -1)
        setupForm()
        if (recordId > 0) {
            loadRecord()
        }
    }

    private fun setupForm() {
        binding.spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("支出", "收入")
        )
        binding.buttonDate.text = recordDate
        binding.buttonDate.setOnClickListener { pickDate() }
        binding.buttonSaveRecord.setOnClickListener { saveRecord() }
    }

    private fun loadRecord() {
        ApiClient.getBookkeepingRecord(recordId) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONObject) {
                            fillForm(apiResult.data)
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

    private fun fillForm(record: JSONObject) {
        binding.spinnerType.setSelection(if (record.optString("type") == "收入") 1 else 0)
        binding.editCategory.setText(record.optString("category"))
        binding.editAmount.setText(record.optString("amount"))
        recordDate = record.optString("recordDate", LocalDate.now().toString())
        binding.buttonDate.text = recordDate
        binding.editRemark.setText(record.optString("remark"))
    }

    private fun pickDate() {
        val date = LocalDate.parse(recordDate)
        DatePickerDialog(this, { _, year, month, day ->
            recordDate = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.buttonDate.text = recordDate
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun saveRecord() {
        val phone = getSharedPreferences("user_session", MODE_PRIVATE).getString("phone", null)
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val type = binding.spinnerType.selectedItem.toString()
        val category = binding.editCategory.text.toString().trim()
        val amount = binding.editAmount.text.toString().trim()
        val remark = binding.editRemark.text.toString().trim()
        if (category.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "请填写类目和金额", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSaveRecord.isEnabled = false
        ApiClient.saveBookkeepingRecord(recordId.takeIf { it > 0 }, phone, type, category, amount, recordDate, remark) { result ->
            runOnUiThread {
                binding.buttonSaveRecord.isEnabled = true
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) {
                            finish()
                        }
                    }
                    .onFailure {
                        Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
