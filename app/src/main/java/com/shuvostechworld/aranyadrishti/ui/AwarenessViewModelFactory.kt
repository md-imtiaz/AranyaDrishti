package com.shuvostechworld.aranyadrishti.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shuvostechworld.aranyadrishti.data.repository.AwarenessRepository

class AwarenessViewModelFactory(private val repository: AwarenessRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AwarenessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AwarenessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
