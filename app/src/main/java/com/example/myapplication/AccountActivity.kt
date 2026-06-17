package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAccountBinding

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        Toast.makeText(this, "账户管理功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun currentPhone(): String? {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)
    }
}
