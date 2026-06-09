package com.example.myapplication.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.BookkeepingEditActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.editMonth.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
        binding.buttonQuery.setOnClickListener { loadRecords() }
        binding.buttonAddRecord.setOnClickListener {
            startActivity(Intent(requireContext(), BookkeepingEditActivity::class.java))
        }

        loadRecords()
        return binding.root
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
        binding.textIncomeTotal.text = data.optString("incomeTotal", "0.00")
        binding.textExpenseTotal.text = data.optString("expenseTotal", "0.00")
        renderRecords(data.optJSONArray("records") ?: JSONArray())
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
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(18, 18, 18, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 14)
            }
        }

        val title = TextView(context).apply {
            text = "${record.optString("type")}  ${record.optString("category")}"
            textSize = 17f
            setTextColor(Color.parseColor("#222222"))
        }
        val detail = TextView(context).apply {
            text = "${record.optString("recordDate")}    ${record.optString("amount")} 元"
            textSize = 15f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 8, 0, 0)
        }
        val remark = TextView(context).apply {
            text = record.optString("remark")
            textSize = 14f
            setTextColor(Color.parseColor("#777777"))
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            setPadding(0, 8, 0, 0)
        }
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }
        val edit = Button(context).apply {
            text = "编辑"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f)
            setOnClickListener {
                startActivity(Intent(context, BookkeepingEditActivity::class.java).putExtra("record_id", record.optLong("id")))
            }
        }
        val delete = Button(context).apply {
            text = "删除"
            layoutParams = LinearLayout.LayoutParams(0, 48, 1f).apply {
                setMargins(12, 0, 0, 0)
            }
            setOnClickListener {
                deleteRecord(record.optLong("id"))
            }
        }

        actions.addView(edit)
        actions.addView(delete)
        box.addView(title)
        box.addView(detail)
        box.addView(remark)
        box.addView(actions)
        return box
    }

    private fun deleteRecord(id: Long) {
        ApiClient.deleteBookkeepingRecord(id) { result ->
            activity?.runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(requireContext(), apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) {
                            loadRecords()
                        }
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
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
