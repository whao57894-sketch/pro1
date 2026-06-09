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
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.databinding.FragmentDashboardBinding
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
        binding.spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("支出", "收入")
        )
        binding.buttonDate.text = recordDate
        binding.buttonDate.setOnClickListener { pickDate() }
        binding.buttonSaveRecord.setOnClickListener { saveRecord() }
        binding.buttonUploadBill.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, 1001)
        }
    }

    private fun pickDate() {
        val date = LocalDate.parse(recordDate)
        DatePickerDialog(requireContext(), { _, year, month, day ->
            recordDate = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.buttonDate.text = recordDate
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun saveRecord() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val type = binding.spinnerType.selectedItem.toString()
        val category = binding.editCategory.text.toString().trim()
        val amount = binding.editAmount.text.toString().trim()
        val remark = binding.editRemark.text.toString().trim()
        if (category.isEmpty() || amount.isEmpty()) {
            Toast.makeText(requireContext(), "请填写类目和金额", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSaveRecord.isEnabled = false
        ApiClient.saveBookkeepingRecord(null, phone, type, category, amount, recordDate, remark) { result ->
            activity?.runOnUiThread {
                binding.buttonSaveRecord.isEnabled = true
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(requireContext(), apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) {
                            clearForm()
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun clearForm() {
        binding.editCategory.text?.clear()
        binding.editAmount.text?.clear()
        binding.editRemark.text?.clear()
        recordDate = LocalDate.now().toString()
        binding.buttonDate.text = recordDate
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
            Toast.makeText(requireContext(), "图片读取失败", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.recognizeBillImage("alipay_bill.jpg", bytes) { result ->
            activity?.runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(requireContext(), apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success && apiResult.data is JSONObject) {
                            fillFromRecognize(apiResult.data)
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "识别失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun fillFromRecognize(data: JSONObject) {
        val type = data.optString("type", "支出")
        binding.spinnerType.setSelection(if (type == "收入") 1 else 0)
        binding.editCategory.setText(data.optString("category"))
        val amount = data.optString("amount")
        if (amount != "0" && amount != "0.00") {
            binding.editAmount.setText(amount)
        }
        recordDate = data.optString("recordDate", LocalDate.now().toString())
        binding.buttonDate.text = recordDate
        binding.editRemark.setText(data.optString("remark"))
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
