package com.shuvostechworld.aranyadrishti.ui

import android.media.MediaPlayer
import android.net.Uri
import coil.load
import java.io.File
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.repository.ReportRepository
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import com.shuvostechworld.aranyadrishti.databinding.ActivityReportDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportDetailsBinding
    private lateinit var viewModel: MainViewModel
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false
    private var voiceNotePath: String? = null
    private var currentReport: ReportEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        
        binding.btnBack.setOnClickListener {
            finish()
        }

        val reportId = intent.getLongExtra("report_id", -1L)
        if (reportId != -1L) {
            loadReport(reportId)
        } else {
            binding.tvCategory.text = "Error"
        }

        setupActionButtons()
    }

    private fun setupActionButtons() {
        binding.btnDeleteReport.setOnClickListener {
            currentReport?.let { report ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        // Schedule cloud deletion worker
                        val deleteConstraints = androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                        val deleteWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.shuvostechworld.aranyadrishti.worker.ReportDeleteWorker>()
                            .setConstraints(deleteConstraints)
                            .setInputData(androidx.work.workDataOf("remoteId" to report.remoteId))
                            .build()
                        androidx.work.WorkManager.getInstance(applicationContext).enqueue(deleteWorkRequest)

                        viewModel.deleteReport(report)
                        android.widget.Toast.makeText(this@ReportDetailsActivity, "Report deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .show()
            }
        }

        binding.btnEditReport.setOnClickListener {
            currentReport?.let { report ->
                val intent = Intent(this, CreateReportActivity::class.java).apply {
                    putExtra("edit_mode", true)
                    putExtra("report_id", report.id)
                    putExtra("remote_id", report.remoteId)
                    putExtra("category", report.category)
                    putExtra("urgency", report.urgency)
                    putExtra("description", report.descriptionText)
                    putExtra("location_lat", report.locationLat)
                    putExtra("location_lng", report.locationLng)
                    putExtra("image_path", report.imageLocalPath)
                    putExtra("audio_path", report.voiceNoteLocalPath)
                }
                startActivity(intent)
                finish() // Optionally finish details view when editing
            }
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = ReportRepository(database.reportDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }

    private fun loadReport(id: Long) {
        lifecycleScope.launch {
            val report = viewModel.getReportById(id)
            if (report != null) {
                currentReport = report
                binding.tvCategory.text = report.category
                binding.tvPriority.text = report.urgency
                
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(report.timestamp))
                binding.tvTimestamp.text = dateStr
                
                binding.tvLocation.text = "Lat: ${report.locationLat}\nLng: ${report.locationLng}"
                
                // Image
                if (!report.imageLocalPath.isNullOrEmpty()) {
                    binding.cvPhotoContainer.visibility = View.VISIBLE
                    binding.ivReportPhoto.load(File(report.imageLocalPath)) {
                        crossfade(true)
                    }
                } else {
                    binding.cvPhotoContainer.visibility = View.GONE
                }

                // Description
                if (!report.descriptionText.isNullOrEmpty()) {
                    binding.tvDescriptionLabel.visibility = View.VISIBLE
                    binding.tvDescription.visibility = View.VISIBLE
                    binding.tvDescription.text = report.descriptionText
                } else {
                    binding.tvDescriptionLabel.visibility = View.GONE
                    binding.tvDescription.visibility = View.GONE
                }

                // Audio
                if (!report.voiceNoteLocalPath.isNullOrEmpty()) {
                    voiceNotePath = report.voiceNoteLocalPath
                    binding.btnPlayAudio.visibility = View.VISIBLE
                    setupAudioPlayer()
                } else {
                    binding.btnPlayAudio.visibility = View.GONE
                }
            } else {
                // Error loading report
            }
        }
    }

    private fun setupAudioPlayer() {
        binding.btnPlayAudio.setOnClickListener {
            if (isPlayingAudio) {
                stopAudio()
            } else {
                playAudio()
            }
        }
    }

    private fun playAudio() {
        if (voiceNotePath.isNullOrEmpty()) return
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(voiceNotePath)
                prepare()
                start()
                isPlayingAudio = true
                binding.btnPlayAudio.text = "Stop Audio"
                binding.btnPlayAudio.setIconResource(android.R.drawable.ic_media_pause)
                binding.btnPlayAudio.contentDescription = "Stop Voice Note"
                
                setOnCompletionListener {
                    stopAudio()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlayingAudio = false
        binding.btnPlayAudio.text = "Play Voice Note"
        binding.btnPlayAudio.setIconResource(android.R.drawable.ic_media_play)
        binding.btnPlayAudio.contentDescription = "Play Voice Note"
    }
}
