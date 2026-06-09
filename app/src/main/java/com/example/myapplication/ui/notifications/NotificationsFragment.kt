package com.example.myapplication.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.ApiClient
import com.example.myapplication.EditProfileActivity
import com.example.myapplication.LoginActivity
import com.example.myapplication.databinding.FragmentNotificationsBinding
import org.json.JSONObject

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.buttonEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.buttonLogout.setOnClickListener {
            requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        loadProfile()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadProfile()
        }
    }

    private fun loadProfile() {
        val phone = requireContext()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("phone", null)

        if (phone.isNullOrBlank()) {
            binding.textName.text = "我的"
            binding.textPhone.text = "未登录"
            binding.textBookkeepingCount.text = "0"
            return
        }

        binding.textPhone.text = phone
        ApiClient.getUser(phone) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success && apiResult.data is JSONObject) {
                        binding.textName.text = apiResult.data.optString("name", "我的")
                        binding.textPhone.text = apiResult.data.optString("phone", phone)
                    } else {
                        Toast.makeText(requireContext(), apiResult.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        ApiClient.getBookkeepingCount(phone) { result ->
            activity?.runOnUiThread {
                result.onSuccess { apiResult ->
                    if (apiResult.success) {
                        binding.textBookkeepingCount.text = apiResult.data?.toString() ?: "0"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
