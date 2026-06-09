package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener {
            register()
        }

        binding.textLogin.setOnClickListener {
            finish()
        }
    }

    private fun register() {
        val username = binding.editUsername.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "请完整填写注册信息", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonRegister.isEnabled = false
        ApiClient.register(username, email, password, confirmPassword) { result ->
            runOnUiThread {
                binding.buttonRegister.isEnabled = true

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
