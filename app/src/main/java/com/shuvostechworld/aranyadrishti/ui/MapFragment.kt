package com.shuvostechworld.aranyadrishti.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.shuvostechworld.aranyadrishti.R
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.repository.ReportRepository
import com.shuvostechworld.aranyadrishti.databinding.FragmentMapBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private var mMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ReportRepository(database.reportDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allReports.collectLatest { reports ->
                mMap?.clear()
                val filteredReports = reports.filter { 
                    it.locationLat != null && it.locationLng != null && 
                    it.locationLat != 0.0 && it.locationLng != 0.0 
                }

                if (filteredReports.isEmpty()) {
                    return@collectLatest
                }

                val builder = LatLngBounds.Builder()
                val list = ArrayList<LatLng>()
                for (report in filteredReports) {
                    val position = LatLng(report.locationLat!!, report.locationLng!!)
                    list.add(position)
                    builder.include(position)
                }

                if (list.isNotEmpty()) {
                    val provider = HeatmapTileProvider.Builder()
                        .data(list)
                        .build()
                    mMap?.addTileOverlay(TileOverlayOptions().tileProvider(provider))
                }

                if (filteredReports.size == 1) {
                    val report = filteredReports.first()
                    val pos = LatLng(report.locationLat!!, report.locationLng!!)
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                } else {
                    try {
                        val bounds = builder.build()
                        mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val latest = filteredReports.firstOrNull()
                        if (latest != null) {
                            val pos = LatLng(latest.locationLat!!, latest.locationLng!!)
                            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                        }
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
