package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否已有有效的登录状态
        checkAutoLogin()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            login()
        }

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkAutoLogin() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val savedPhone = prefs.getString("phone", null)

        // 只要本地保存了手机号就尝试自动登录（后端暂未提供 token 校验）
        if (!savedPhone.isNullOrBlank()) {
            // 恢复 token 到 ApiClient（若有）
            prefs.getString("auth_token", null)?.takeIf { it.isNotBlank() }?.let {
                ApiClient.authToken = it
            }

            // 验证账号是否仍然有效
            ApiClient.getUser(savedPhone) { result ->
                runOnUiThread {
                    result.onSuccess { apiResult ->
                        if (apiResult.success) {
                            // 账号有效，直接跳转到主页
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // 账号无效，清除保存的信息
                            clearSession()
                        }
                    }.onFailure {
                        // 网络错误，清除 session
                        clearSession()
                    }
                }
            }
        }
    }

    private fun clearSession() {
        getSharedPreferences("user_session", MODE_PRIVATE)
            .edit()
            .remove("auth_token")
            .remove("phone")
            .apply()
        ApiClient.authToken = null
    }

    private fun login() {
        val phone = binding.editPhone.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入手机号码和密码", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonLogin.isEnabled = false
        ApiClient.login(phone, password) { result ->
            runOnUiThread {
                binding.buttonLogin.isEnabled = true

                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) {
                            // 保存 token 和 phone
                            getSharedPreferences("user_session", MODE_PRIVATE)
                                .edit()
                                .putString("phone", phone)
                                .putString("auth_token", apiResult.token ?: "")
                                .apply()

                            // 设置到 ApiClient
                            ApiClient.authToken = apiResult.token

                            startActivity(Intent(this, MainActivity::class.java))
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
