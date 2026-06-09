package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityEditProfileBinding
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val genders = listOf("男", "女")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spinnerGender.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            genders
        )

        binding.buttonSave.setOnClickListener {
            saveProfile()
        }

        loadProfile()
    }

    private fun currentPhone(): String? {
        return getSharedPreferences("user_session", MODE_PRIVATE).getString("phone", null)
    }

    private fun loadProfile() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ApiClient.getUser(phone) { result ->
            runOnUiThread {
                result
                    .onSuccess { apiResult ->
                        if (apiResult.success && apiResult.data is JSONObject) {
                            val user = apiResult.data
                            binding.editName.setText(user.optString("name"))
                            binding.editAge.setText(user.optInt("age").toString())
                            binding.editOccupation.setText(user.optString("occupation"))
                            val genderIndex = genders.indexOf(user.optString("gender")).coerceAtLeast(0)
                            binding.spinnerGender.setSelection(genderIndex)
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

    private fun saveProfile() {
        val phone = currentPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val name = binding.editName.text.toString().trim()
        val ageText = binding.editAge.text.toString().trim()
        val occupation = binding.editOccupation.text.toString().trim()
        val gender = binding.spinnerGender.selectedItem.toString()

        if (name.isEmpty() || ageText.isEmpty() || occupation.isEmpty()) {
            Toast.makeText(this, "请完整填写个人信息", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            Toast.makeText(this, "请输入正确的年龄", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSave.isEnabled = false
        ApiClient.updateUser(phone, name, age, occupation, gender) { result ->
            runOnUiThread {
                binding.buttonSave.isEnabled = true
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
