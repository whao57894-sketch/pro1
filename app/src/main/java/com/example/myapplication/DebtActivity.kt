package com.example.myapplication

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityDebtBinding
import com.example.myapplication.databinding.DialogDebtEditBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class DebtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebtBinding
    private var currentType = "借出"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebtBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabAddDebt.setOnClickListener { showDebtDialog(null) }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentType = if (tab?.position == 0) "借出" else "借入"
                loadDebts()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        loadDebts()
    }

    private fun loadDebts() {
        val phone = currentPhone() ?: return

        ApiClient.getDebts(phone) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONArray) {
                            renderDebts(apiResult.data)
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

    private fun renderDebts(debts: JSONArray) {
        binding.debtList.removeAllViews()

        val filteredDebts = mutableListOf<JSONObject>()
        for (i in 0 until debts.length()) {
            val debt = debts.getJSONObject(i)
            if (debt.optString("type") == currentType) {
                filteredDebts.add(debt)
            }
        }

        if (filteredDebts.isEmpty()) {
            val empty = TextView(this).apply {
                text = "暂无${currentType}记录\n点击右下角按钮添加"
                textSize = 16f
                setTextColor(Color.parseColor("#999999"))
                setPadding(0, 48, 0, 48)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            binding.debtList.addView(empty)
            return
        }

        filteredDebts.forEach { debt ->
            binding.debtList.addView(createDebtView(debt))
        }
    }

    private fun createDebtView(debt: JSONObject): View {
        val card = layoutInflater.inflate(R.layout.item_debt_card, binding.debtList, false)

        val counterparty = card.findViewById<TextView>(R.id.textCounterparty)
        val status = card.findViewById<TextView>(R.id.textStatus)
        val amount = card.findViewById<TextView>(R.id.textAmount)
        val remaining = card.findViewById<TextView>(R.id.textRemaining)
        val date = card.findViewById<TextView>(R.id.textDate)
        val dueDate = card.findViewById<TextView>(R.id.textDueDate)
        val btnRepay = card.findViewById<MaterialButton>(R.id.buttonRepay)
        val btnDelete = card.findViewById<MaterialButton>(R.id.buttonDelete)

        val totalAmount = debt.optDouble("amount", 0.0)
        val repaidAmount = debt.optDouble("repaidAmount", 0.0)
        val remainingAmount = totalAmount - repaidAmount
        val isPaid = remainingAmount <= 0.0

        counterparty.text = debt.optString("counterparty")
        amount.text = "金额：¥%.2f".format(totalAmount)
        remaining.text = "剩余：¥%.2f".format(remainingAmount)
        date.text = "借款日期：${debt.optString("debtDate")}"

        val dueDateStr = debt.optString("dueDate", "")
        if (dueDateStr.isNotEmpty()) {
            dueDate.text = "到期日期：$dueDateStr"
            dueDate.visibility = View.VISIBLE
        } else {
            dueDate.visibility = View.GONE
        }

        if (isPaid) {
            status.text = "已还清"
            status.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnRepay.isEnabled = false
        } else {
            status.text = "未还清"
            status.setBackgroundColor(Color.parseColor("#FF9800"))
        }

        btnRepay.setOnClickListener { showRepayDialog(debt) }
        btnDelete.setOnClickListener { deleteDebt(debt.optLong("id")) }

        return card
    }

    private fun showDebtDialog(debt: JSONObject?) {
        val dialogBinding = DialogDebtEditBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("借出", "借入")
        )

        var debtDate = LocalDate.now().toString()
        var dueDate: String? = null

        dialogBinding.editDebtDate.setText(debtDate)
        dialogBinding.editDebtDate.setOnClickListener {
            showDatePicker { date ->
                debtDate = date
                dialogBinding.editDebtDate.setText(date)
            }
        }

        dialogBinding.editDueDate.setOnClickListener {
            showDatePicker { date ->
                dueDate = date
                dialogBinding.editDueDate.setText(date)
            }
        }

        if (debt != null) {
            dialogBinding.editCounterparty.setText(debt.optString("counterparty"))
            dialogBinding.spinnerType.setSelection(if (debt.optString("type") == "借入") 1 else 0)
            dialogBinding.editAmount.setText(debt.optDouble("amount").toString())
            debtDate = debt.optString("debtDate")
            dialogBinding.editDebtDate.setText(debtDate)
            dueDate = debt.optString("dueDate", "")
            if (!dueDate.isNullOrEmpty()) {
                dialogBinding.editDueDate.setText(dueDate)
            }
            dialogBinding.editRemark.setText(debt.optString("remark", ""))
        }

        dialogBinding.buttonCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.buttonSave.setOnClickListener {
            val counterparty = dialogBinding.editCounterparty.text.toString().trim()
            val type = dialogBinding.spinnerType.selectedItem.toString()
            val amountText = dialogBinding.editAmount.text.toString().trim()
            val remark = dialogBinding.editRemark.text.toString().trim()

            if (counterparty.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "请输入正确的金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveDebt(debt?.optLong("id"), type, counterparty, amount, debtDate, dueDate, remark)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRepayDialog(debt: JSONObject) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_debt_edit, null)
        val editAmount = TextInputEditText(this)
        editAmount.hint = "还款金额"
        editAmount.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val remainingAmount = debt.optDouble("amount", 0.0) - debt.optDouble("repaidAmount", 0.0)

        AlertDialog.Builder(this)
            .setTitle("还款")
            .setMessage("剩余金额：¥%.2f".format(remainingAmount))
            .setView(editAmount)
            .setPositiveButton("确认") { _, _ ->
                val amountText = editAmount.text.toString().trim()
                val amount = amountText.toDoubleOrNull()

                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "请输入正确的金额", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (amount > remainingAmount) {
                    Toast.makeText(this, "还款金额不能超过剩余金额", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                repayDebt(debt.optLong("id"), amount)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDatePicker(callback: (String) -> Unit) {
        val date = LocalDate.now()
        DatePickerDialog(this, { _, year, month, day ->
            callback("%04d-%02d-%02d".format(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun saveDebt(id: Long?, type: String, counterparty: String, amount: Double, debtDate: String, dueDate: String?, remark: String?) {
        val phone = currentPhone() ?: return

        ApiClient.saveDebt(id, phone, type, counterparty, amount, debtDate, dueDate, remark) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) loadDebts()
                    }
                    .onFailure {
                        Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun repayDebt(id: Long, amount: Double) {
        ApiClient.repayDebt(id, amount) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) loadDebts()
                    }
                    .onFailure {
                        Toast.makeText(this, "连接后端失败：${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun deleteDebt(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除债务记录")
            .setMessage("确定要删除这条债务记录吗？")
            .setPositiveButton("删除") { _, _ ->
                ApiClient.deleteDebt(id) { result ->
                    runOnUiThread {
                        result
                            .onSuccess { apiResult ->
                                Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                                if (apiResult.success) loadDebts()
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
