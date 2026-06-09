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

        binding.textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val username = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonLogin.isEnabled = false
        ApiClient.login(username, password) { result ->
            runOnUiThread {
                binding.buttonLogin.isEnabled = true

                result
                    .onSuccess { apiResult ->
                        Toast.makeText(this, apiResult.message, Toast.LENGTH_SHORT).show()
                        if (apiResult.success) {
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
