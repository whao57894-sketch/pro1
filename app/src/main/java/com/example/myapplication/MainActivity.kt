package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate 之前应用深色模式设置
        applyThemeMode()

        // 恢复 token 到 ApiClient（如果之前保存了）
        restoreAuthToken()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_statistics, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun applyThemeMode() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun restoreAuthToken() {
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("auth_token", null)

        if (!savedToken.isNullOrBlank()) {
            ApiClient.authToken = savedToken
        }
    }
}