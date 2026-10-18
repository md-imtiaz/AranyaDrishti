package com.shuvostechworld.aranyadrishti.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.functions.FirebaseFunctions
import com.shuvostechworld.aranyadrishti.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = FirebaseFunctions.getInstance()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSendAlert.setOnClickListener {
            val title = binding.etAlertTitle.text.toString().trim()
            val message = binding.etAlertMessage.text.toString().trim()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Title and message cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendEmergencyAlert(title, message)
        }
    }

    private fun sendEmergencyAlert(title: String, message: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendAlert.isEnabled = false

        val data = hashMapOf(
            "title" to title,
            "message" to message
        )

        functions
            .getHttpsCallable("sendEmergencyAlert")
            .call(data)
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                binding.btnSendAlert.isEnabled = true
                Toast.makeText(this, "Alert broadcasted successfully!", Toast.LENGTH_LONG).show()
                binding.etAlertTitle.text?.clear()
                binding.etAlertMessage.text?.clear()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSendAlert.isEnabled = true
                Toast.makeText(this, "Failed to send alert: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
