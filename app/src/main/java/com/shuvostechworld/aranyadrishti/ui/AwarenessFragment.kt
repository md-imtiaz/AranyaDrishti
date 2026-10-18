package com.shuvostechworld.aranyadrishti.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.media.MediaPlayer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shuvostechworld.aranyadrishti.R
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.local.RewardManager
import com.shuvostechworld.aranyadrishti.data.repository.AwarenessRepository
import com.shuvostechworld.aranyadrishti.databinding.FragmentAwarenessBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AwarenessFragment : Fragment() {
    private var _binding: FragmentAwarenessBinding? = null
    private val binding get() = _binding!!

    private lateinit var eduCardAdapter: EduCardAdapter
    private lateinit var awarenessViewModel: AwarenessViewModel

    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val context = requireContext()
        val oldBadge = RewardManager.getCurrentBadge(RewardManager.getPoints(context))
        RewardManager.addPoints(context, 15)
        val newBadge = RewardManager.getCurrentBadge(RewardManager.getPoints(context))

        if (oldBadge != newBadge) {
            MediaPlayer.create(context, R.raw.badge_sound)?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
            Toast.makeText(context, "Level Up! \uD83C\uDF1F You are now a $newBadge!", Toast.LENGTH_LONG).show()
        } else {
            MediaPlayer.create(context, R.raw.gem_sound)?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
            Toast.makeText(context, "Thanks for sharing! You earned +15 Gems! \uD83D\uDC8E", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAwarenessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAwarenessHub()
    }

    private fun setupAwarenessHub() {
        eduCardAdapter = EduCardAdapter { shareIntent ->
            shareLauncher.launch(shareIntent)
        }
        binding.rvAwarenessHub.apply {
            // Using VERTICAL layout manager for the dedicated tab
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = eduCardAdapter
        }

        val database = AppDatabase.getDatabase(requireContext())
        val repository = AwarenessRepository(database.eduCardDao())
        val factory = AwarenessViewModelFactory(repository)
        awarenessViewModel = ViewModelProvider(this, factory)[AwarenessViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            awarenessViewModel.awarenessCards.collectLatest { cards ->
                if (_binding == null) return@collectLatest
                if (cards.isNotEmpty()) {
                    binding.shimmerAwarenessHub.stopShimmer()
                    binding.shimmerAwarenessHub.visibility = View.GONE
                    binding.rvAwarenessHub.visibility = View.VISIBLE
                    eduCardAdapter.submitList(cards)
                } else {
                    binding.shimmerAwarenessHub.startShimmer()
                    binding.shimmerAwarenessHub.visibility = View.VISIBLE
                    binding.rvAwarenessHub.visibility = View.GONE
                }
            }
        }

        awarenessViewModel.loadAwarenessData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
