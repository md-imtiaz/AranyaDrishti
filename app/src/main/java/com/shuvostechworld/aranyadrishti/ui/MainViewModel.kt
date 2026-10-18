package com.shuvostechworld.aranyadrishti.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import com.shuvostechworld.aranyadrishti.data.repository.ReportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ReportRepository) : ViewModel() {

    val allReports: StateFlow<List<ReportEntity>> = repository.allReports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.insertReport(report)
        }
    }

    suspend fun getReportById(id: Long): ReportEntity? {
        return repository.getReportById(id)
    }

    fun updateReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.updateReport(report)
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.deleteReport(report)
        }
    }
}

class MainViewModelFactory(private val repository: ReportRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
