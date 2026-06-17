package com.example.myapplication.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.LoginActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentNotificationsBinding
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        setupClickListeners()
        loadProfile()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadProfile()
        }
    }

    private fun setupClickListeners() {
        // 设置按钮
        binding.buttonSettings.setOnClickListener {
            Toast.makeText(requireContext(), "设置功能开发中...", Toast.LENGTH_SHORT).show()
        }

        // 数据管理
        binding.itemBackup.setOnClickListener {
            showBackupDialog()
        }

        binding.itemExport.setOnClickListener {
            showExportDialog()
        }

        // 个性化 - 深色模式
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("dark_mode", isChecked).apply()

            if (isChecked) {
                Toast.makeText(requireContext(), "深色模式已开启，重启应用后生效", Toast.LENGTH_LONG).show()
                // 动态切换主题
                requireActivity().setTheme(R.style.Theme_MyApplication)
                requireActivity().recreate()
            } else {
                Toast.makeText(requireContext(), "深色模式已关闭，重启应用后生效", Toast.LENGTH_LONG).show()
                requireActivity().recreate()
            }
        }

        binding.itemNotification.setOnClickListener {
            Toast.makeText(requireContext(), "消息通知设置开发中...", Toast.LENGTH_SHORT).show()
        }

        // 关于
        binding.itemAbout.setOnClickListener {
            showAboutDialog()
        }

        // 退出登录
        binding.buttonLogout.setOnClickListener {
            showLogoutDialog()
        }

        // 用户信息点击 - 可以跳转到编辑资料
        binding.textName.setOnClickListener {
            val phone = currentPhone()
            if (phone.isNullOrBlank()) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "个人资料编辑功能开发中...", Toast.LENGTH_SHORT).show()
            }
        }

        // 加载深色模式设置
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        binding.switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
    }

    private fun loadProfile() {
        val phone = currentPhone()

        if (phone.isNullOrBlank()) {
            binding.textName.text = "未登录"
            binding.textPhone.text = "点击登录"
            binding.textRecordCount.text = "0"
            binding.textDaysCount.text = "0"
            return
        }

        binding.textPhone.text = phone

        // 加载用户信息
        ApiClient.getUser(phone) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is JSONObject) {
                        binding.textName.text = apiResult.data.optString("name", "用户")
                    }
                }
            }
        }

        // 加载记账统计
        loadStatistics(phone)
    }

    private fun loadStatistics(phone: String) {
        // 获取总记账笔数
        ApiClient.getBookkeepingCount(phone) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success) {
                        binding.textRecordCount.text = apiResult.data?.toString() ?: "0"
                    }
                }
            }
        }

        // 计算使用天数 - 从第一笔记账到现在
        ApiClient.getMonthlyTrend(phone, 12) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is JSONArray) {
                        val data = apiResult.data
                        if (data.length() > 0) {
                            val firstMonth = data.getJSONObject(0).optString("month")
                            try {
                                val firstDate = LocalDate.parse("$firstMonth-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                val today = LocalDate.now()
                                val days = ChronoUnit.DAYS.between(firstDate, today)
                                binding.textDaysCount.text = days.toString()
                            } catch (e: Exception) {
                                binding.textDaysCount.text = "0"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("☁️ 云端备份")
            .setMessage("是否将数据备份到云端？\n\n备份内容包括：\n• 所有记账记录\n• 预算设置\n• 债务信息\n• 个人设置")
            .setPositiveButton("立即备份") { _, _ ->
                Toast.makeText(requireContext(), "备份功能开发中...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExportDialog() {
        val options = arrayOf("导出为 Excel", "导出为 CSV", "导出为 TXT")
        AlertDialog.Builder(requireContext())
            .setTitle("📤 导出数据")
            .setItems(options) { _, which ->
                val format = when (which) {
                    0 -> "Excel"
                    1 -> "CSV"
                    else -> "TXT"
                }
                showMonthPickerForExport(format)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMonthPickerForExport(format: String) {
        val months = mutableListOf<String>()
        val currentDate = java.time.LocalDate.now()

        // 生成最近12个月的选项
        for (i in 0..11) {
            val date = currentDate.minusMonths(i.toLong())
            months.add(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")))
        }

        AlertDialog.Builder(requireContext())
            .setTitle("选择月份")
            .setItems(months.toTypedArray()) { _, which ->
                exportData(format, months[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportData(format: String, month: String) {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载对话框
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("导出中...")
            .setMessage("正在生成文件，请稍候...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // 获取月度数据
        ApiClient.getBookkeepingMonth(phone, month) { result ->
            activity?.runOnUiThread {
                loadingDialog.dismiss()

                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is JSONObject) {
                        val records = apiResult.data.optJSONArray("records") ?: JSONArray()

                        if (records.length() == 0) {
                            Toast.makeText(requireContext(), "该月份没有记录", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        // 执行导出
                        val filePath = when (format) {
                            "Excel" -> com.example.myapplication.utils.ExportUtil.exportToExcel(requireContext(), records, month)
                            "CSV" -> com.example.myapplication.utils.ExportUtil.exportToCSV(requireContext(), records, month)
                            else -> com.example.myapplication.utils.ExportUtil.exportToText(requireContext(), records, month)
                        }

                        if (filePath != null) {
                            showExportSuccessDialog(filePath, format)
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

    private fun showExportSuccessDialog(filePath: String, format: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("✅ 导出成功")
            .setMessage("文件已保存至：\n$filePath\n\n您可以在文件管理器中的 Documents 目录下找到该文件。")
            .setPositiveButton("确定", null)
            .setNeutralButton("分享") { _, _ ->
                shareFile(filePath)
            }
            .show()
    }

    private fun shareFile(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    filePath.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    filePath.endsWith(".csv") -> "text/csv"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "分享文件"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ℹ️ 关于应用")
            .setMessage("智能记账助手 v1.0.0\n\n一款简洁高效的记账工具，帮助你轻松管理个人财务。\n\n功能特色：\n• 快速记账\n• 数据统计\n• 预算管理\n• 债务追踪\n• 智能分析\n\n© 2026 智能记账团队")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                // 清除用户会话和 token
                requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()

                // 清除 ApiClient 中的 token
                ApiClient.authToken = null

                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("取消", null)
            .show()
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
