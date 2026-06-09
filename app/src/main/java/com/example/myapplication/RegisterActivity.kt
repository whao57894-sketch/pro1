package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spinnerGender.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("男", "女")
        )

        binding.buttonRegister.setOnClickListener {
            register()
        }

        binding.textLogin.setOnClickListener {
            finish()
        }
    }

    private fun register() {
        val phone = binding.editPhone.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()
        val name = binding.editName.text.toString().trim()
        val ageText = binding.editAge.text.toString().trim()
        val occupation = binding.editOccupation.text.toString().trim()
        val gender = binding.spinnerGender.selectedItem.toString()

        if (phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty() || ageText.isEmpty() || occupation.isEmpty()) {
            Toast.makeText(this, "请完整填写注册信息", Toast.LENGTH_SHORT).show()
            return
        }

        if (!phone.matches(Regex("^1\\d{10}$"))) {
            Toast.makeText(this, "请输入正确的手机号码", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            Toast.makeText(this, "请输入正确的年龄", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonRegister.isEnabled = false
        ApiClient.register(phone, password, confirmPassword, name, age, occupation, gender) { result ->
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
