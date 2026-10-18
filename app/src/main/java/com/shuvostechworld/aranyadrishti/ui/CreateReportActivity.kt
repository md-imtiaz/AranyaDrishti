package com.shuvostechworld.aranyadrishti.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import com.shuvostechworld.aranyadrishti.data.repository.ReportRepository
import com.shuvostechworld.aranyadrishti.databinding.ActivityCreateReportBinding
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.MediaPlayer
import java.io.File
import java.io.IOException
import android.graphics.Color
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.shuvostechworld.aranyadrishti.worker.ReportSyncWorker
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shuvostechworld.aranyadrishti.MainActivity
import com.shuvostechworld.aranyadrishti.R

class CreateReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateReportBinding
    private lateinit var viewModel: MainViewModel

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null
    
    private var toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
    private var mediaPlayer: MediaPlayer? = null

    private var imageFilePath: String? = null
    private var currentPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.btnTakePhoto.text = "Photo Captured"
            binding.btnTakePhoto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#388E3C"))
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            android.widget.Toast.makeText(this@CreateReportActivity, "Photo captured and saved successfully", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            imageFilePath = null
            android.widget.Toast.makeText(this@CreateReportActivity, "Photo capture cancelled or failed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchLocation()
            }
            else -> {
                android.widget.Toast.makeText(this@CreateReportActivity, "Location permission denied. Defaulting to empty location.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        if (!permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false)) {
            android.widget.Toast.makeText(this@CreateReportActivity, "Voice recording permission denied.", android.widget.Toast.LENGTH_SHORT).show()
        }
        if (!permissions.getOrDefault(Manifest.permission.CAMERA, false)) {
            android.widget.Toast.makeText(this@CreateReportActivity, "Camera permission denied.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupViewModel()
        setupUI()
        setupListeners()
        requestPermissions()
        
        checkEditMode()
    }

    private fun checkEditMode() {
        val isEditMode = intent.getBooleanExtra("edit_mode", false)
        if (isEditMode) {
            val category = intent.getStringExtra("category") ?: ""
            val urgency = intent.getStringExtra("urgency") ?: ""
            val description = intent.getStringExtra("description") ?: ""
            
            // Set category
            val adapter = binding.spinnerCategory.adapter
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == category) {
                    binding.spinnerCategory.setSelection(i)
                    break
                }
            }

            // Set Priority
            when (urgency) {
                "High" -> binding.rgPriority.check(binding.rbHigh.id)
                "Medium" -> binding.rgPriority.check(binding.rbMedium.id)
                "Low" -> binding.rgPriority.check(binding.rbLow.id)
            }

            // Set Description
            // Assuming description formatting, let's just set it directly. 
            // In saveReport we append 'Manual Location' to description if not found.
            // For editing, let's keep it simple.
            binding.etDescription.setText(description)
            
            // Set paths if needed
            imageFilePath = intent.getStringExtra("image_path")
            audioFilePath = intent.getStringExtra("audio_path")
            
            if (!imageFilePath.isNullOrEmpty()) {
                binding.btnTakePhoto.text = "Photo Saved"
            }
            if (!audioFilePath.isNullOrEmpty()) {
                binding.btnRecordVoice.text = "Voice Note Saved"
                binding.btnPreviewVoice.visibility = android.view.View.VISIBLE
            }
            
            val isAnonymous = intent.getBooleanExtra("is_anonymous", false)
            binding.switchAnonymous.isChecked = isAnonymous
            
            binding.btnSubmitReport.text = "UPDATE REPORT"
        }
    }

    private fun requestPermissions() {
        permissionsRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ))
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location : Location? ->
            currentLocation = location
            if (location != null) {
                android.widget.Toast.makeText(this@CreateReportActivity, "Location found.", android.widget.Toast.LENGTH_SHORT).show()
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val addressText = address.getAddressLine(0) ?: "${address.locality}, ${address.adminArea}"
                        binding.etManualLocation.setText(addressText)
                    } else {
                        binding.etManualLocation.setText("Lat: ${location.latitude}, Lng: ${location.longitude}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.etManualLocation.setText("Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = ReportRepository(database.reportDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupUI() {
        val categories = arrayOf(
            "Wildlife Sighting", 
            "Injured Animal", 
            "Fire Alert", 
            "Pollution", 
            "Illegal Hill Cutting", 
            "Illegal Poaching / Hunting"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRecordVoice.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this@CreateReportActivity, "Microphone permission is required to record voice notes.", android.widget.Toast.LENGTH_SHORT).show()
                requestPermissions()
                return@setOnClickListener
            }

            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        
        binding.btnPreviewVoice.setOnClickListener {
            playVoiceNote()
        }

        binding.btnTakePhoto.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this@CreateReportActivity, "Camera permission is required to capture photos.", android.widget.Toast.LENGTH_SHORT).show()
                requestPermissions()
                return@setOnClickListener
            }
            openCamera(it)
        }

        binding.btnSubmitReport.setOnClickListener {
            saveReport()
        }
    }
    private fun openCamera(view: android.view.View) {
        try {
            val fileName = "report_image_${System.currentTimeMillis()}.jpg"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "report_image_",
                ".jpg",
                storageDir
            )
            imageFilePath = file.absolutePath
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            currentPhotoUri?.let { uri ->
                android.widget.Toast.makeText(this@CreateReportActivity, "Opening camera", android.widget.Toast.LENGTH_SHORT).show()
                takePictureLauncher.launch(uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            android.widget.Toast.makeText(this@CreateReportActivity, "Error occurred while creating image file.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording() {
        val fileName = "report_audio_${System.currentTimeMillis()}.m4a"
        val file = File(externalCacheDir?.absolutePath ?: cacheDir.absolutePath, fileName)
        audioFilePath = file.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(32000)
            setAudioSamplingRate(16000)
            setOutputFile(audioFilePath)

            try {
                prepare()
                
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                
                start()
                isRecording = true

                binding.btnRecordVoice.text = "Stop Recording"
                binding.btnRecordVoice.contentDescription = "Stop Recording"
                binding.btnRecordVoice.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D47A1"))
                android.widget.Toast.makeText(this@CreateReportActivity, "Recording started", android.widget.Toast.LENGTH_SHORT).show()
                binding.btnPreviewVoice.visibility = android.view.View.GONE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            release()
        }
        
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        
        mediaRecorder = null
        isRecording = false

        binding.btnRecordVoice.text = "Voice Note Saved"
        binding.btnRecordVoice.contentDescription = "Record Voice Note"
        binding.btnRecordVoice.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#388E3C"))
        android.widget.Toast.makeText(this@CreateReportActivity, "Recording stopped and saved", android.widget.Toast.LENGTH_SHORT).show()
        binding.btnPreviewVoice.visibility = android.view.View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun playVoiceNote() {
        if (audioFilePath.isNullOrEmpty()) return
        
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()
                android.widget.Toast.makeText(this@CreateReportActivity, "Playing voice note", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun saveReport() {
        val category = binding.spinnerCategory.selectedItem.toString()
        val manualLocation = binding.etManualLocation.text.toString().trim()
        var description = binding.etDescription.text.toString().trim()
        
        val lat = if (manualLocation.isNotEmpty() && currentLocation == null) 0.0 else (currentLocation?.latitude ?: 0.0)
        val lng = if (manualLocation.isNotEmpty() && currentLocation == null) 0.0 else (currentLocation?.longitude ?: 0.0)

        val urgency = when (binding.rgPriority.checkedRadioButtonId) {
            binding.rbHigh.id -> "High"
            binding.rbMedium.id -> "Medium"
            binding.rbLow.id -> "Low"
            else -> "High"
        }
        
        if (manualLocation.isNotEmpty()) {
            val locationContext = "Manual Location: $manualLocation"
            description = if (description.isEmpty()) locationContext else "$locationContext\n$description"
        }
        
        if (description.isEmpty()) {
            description = "Report created from input screen."
        }

        val editId = intent.getLongExtra("report_id", 0L)
        val isAnonymous = binding.switchAnonymous.isChecked
        val existingRemoteId = intent.getStringExtra("remote_id")
        val finalRemoteId = existingRemoteId ?: java.util.UUID.randomUUID().toString()

        val report = ReportEntity(
            id = editId,
            remoteId = finalRemoteId,
            category = category,
            urgency = urgency,
            locationLat = lat, 
            locationLng = lng,
            descriptionText = description,
            voiceNoteLocalPath = audioFilePath ?: "",
            imageLocalPath = imageFilePath ?: "",
            timestamp = System.currentTimeMillis(),
            isSynced = false,
            isAnonymous = isAnonymous
        )
        
        viewModel.insertReport(report)

        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncWorkRequest = OneTimeWorkRequestBuilder<ReportSyncWorker>()
            .setConstraints(syncConstraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(syncWorkRequest)

        android.widget.Toast.makeText(this@CreateReportActivity, "Report saved successfully offline.", android.widget.Toast.LENGTH_SHORT).show()
        
        if (editId == 0L) {
            val oldBadge = com.shuvostechworld.aranyadrishti.data.local.RewardManager.getCurrentBadge(
                com.shuvostechworld.aranyadrishti.data.local.RewardManager.getPoints(this)
            )
            com.shuvostechworld.aranyadrishti.data.local.RewardManager.addPoints(this, 10)
            val newBadge = com.shuvostechworld.aranyadrishti.data.local.RewardManager.getCurrentBadge(
                com.shuvostechworld.aranyadrishti.data.local.RewardManager.getPoints(this)
            )
            
            if (oldBadge != newBadge) {
                // Play level up sound
                android.media.MediaPlayer.create(this, com.shuvostechworld.aranyadrishti.R.raw.badge_sound)?.apply {
                    setOnCompletionListener { it.release() }
                    start()
                }
                android.widget.Toast.makeText(this, "Level Up! \uD83C\uDF1F You are now a $newBadge!", android.widget.Toast.LENGTH_LONG).show()
            } else {
                // Play regular sound
                android.media.MediaPlayer.create(this, com.shuvostechworld.aranyadrishti.R.raw.gem_sound)?.apply {
                    setOnCompletionListener { it.release() }
                    start()
                }
                android.widget.Toast.makeText(this, "Report submitted! You earned +10 Gems! \uD83D\uDC8E", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        finish()
    }
}
