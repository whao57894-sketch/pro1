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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            login()
        }

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
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
                            getSharedPreferences("user_session", MODE_PRIVATE)
                                .edit()
                                .putString("phone", phone)
                                .apply()
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
