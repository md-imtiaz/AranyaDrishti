package com.shuvostechworld.aranyadrishti.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuvostechworld.aranyadrishti.R
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.local.RewardManager
import com.shuvostechworld.aranyadrishti.data.repository.ReportRepository
import com.shuvostechworld.aranyadrishti.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var reportAdapter: ReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkAdminStatus()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ReportRepository(database.reportDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter { reportId ->
            val intent = Intent(requireContext(), ReportDetailsActivity::class.java)
            intent.putExtra("report_id", reportId)
            startActivity(intent)
        }
        binding.rvReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportAdapter
        }
    }

    private fun setupListeners() {
        binding.btnCreateReport.setOnClickListener {
            val intent = Intent(requireContext(), CreateReportActivity::class.java)
            startActivity(intent)
        }
        binding.btnAdminPanel.setOnClickListener {
            val intent = Intent(requireContext(), AdminDashboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allReports.collectLatest { reports ->
                reportAdapter.submitList(reports)
                if (reports.isEmpty()) {
                    binding.rvReports.visibility = View.GONE
                    binding.llEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvReports.visibility = View.VISIBLE
                    binding.llEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun checkAdminStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("admins").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (_binding == null) return@addOnSuccessListener
                    if (document.exists()) {
                        binding.btnAdminPanel.visibility = View.VISIBLE
                    } else {
                        binding.btnAdminPanel.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    if (_binding != null) {
                        binding.btnAdminPanel.visibility = View.GONE
                    }
                }
        } else {
            binding.btnAdminPanel.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateEcoRewardsUI()
    }

    fun updateEcoRewardsUI() {
        if (_binding == null) return
        val name = RewardManager.getUserName(requireContext())
        if (name.isEmpty()) return

        val points = RewardManager.getPoints(requireContext())
        val badge = RewardManager.getCurrentBadge(points)

        binding.tvRewardName.text = "Welcome, $name"
        binding.tvRewardPoints.text = "Gems \uD83D\uDC8E: $points"
        binding.tvRewardBadge.text = "Rank: $badge"
        binding.cvEcoRewards.contentDescription = "Gamification profile for $name. You have $points Gems and your rank is $badge."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
