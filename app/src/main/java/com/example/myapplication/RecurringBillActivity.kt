package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRecurringBillBinding

class RecurringBillActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecurringBillBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecurringBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        Toast.makeText(this, "周期账单功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun currentPhone(): String? {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)
    }
}
