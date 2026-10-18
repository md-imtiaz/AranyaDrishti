package com.shuvostechworld.aranyadrishti

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.shuvostechworld.aranyadrishti.data.local.CloudSyncManager
import com.shuvostechworld.aranyadrishti.data.local.RewardManager
import com.shuvostechworld.aranyadrishti.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigation()
        setupEmergencyFab()

        CloudSyncManager.signInAnonymouslyAndSync(this) {
            checkFirstLaunchGamification()
        }
        
        setupFirebaseMessaging()
        askNotificationPermission()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)

        // Update toolbar title based on fragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = destination.label ?: "Aranya Drishti"
        }
    }

    private fun setupEmergencyFab() {
        binding.fabEmergency.setOnClickListener {
            simulateEmergencyAlert()
        }
    }

    private fun simulateEmergencyAlert() {
        val title = "🚨 TEST: WILDLIFE EMERGENCY ALERT"
        val messageBody = "This is a test of the Bandarban emergency alert system!"

        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "emergency_alerts"
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM) ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Wildlife Emergency Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for wildlife encounters."
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
                setSound(defaultSoundUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(999, notificationBuilder.build())
        Toast.makeText(this, "Emergency Alert Broadcasted!", Toast.LENGTH_SHORT).show()
    }

    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().subscribeToTopic("bandarban_alerts")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Successfully subscribed to bandarban_alerts topic.")
                } else {
                    Log.e("FCM", "Failed to subscribe to bandarban_alerts topic.", task.exception)
                }
            }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkFirstLaunchGamification() {
        val userName = RewardManager.getUserName(this)
        if (userName.isEmpty()) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_welcome, null)
            val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
            val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveName)

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
                
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnSave.setOnClickListener {
                val name = input.text.toString().trim()
                if (name.length >= 3) {
                    RewardManager.setUserName(this@MainActivity, name)
                    
                    // Notify HomeFragment to update its UI
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                    val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
                    if (currentFragment is com.shuvostechworld.aranyadrishti.ui.HomeFragment) {
                        currentFragment.updateEcoRewardsUI()
                    }
                    
                    dialog.dismiss()
                } else {
                    input.error = "Name must be at least 3 characters"
                }
            }
            dialog.show()
        }
    }
}