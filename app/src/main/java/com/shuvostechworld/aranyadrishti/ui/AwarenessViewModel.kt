package com.shuvostechworld.aranyadrishti.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuvostechworld.aranyadrishti.data.model.EduCard
import com.shuvostechworld.aranyadrishti.data.repository.AwarenessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AwarenessViewModel(private val repository: AwarenessRepository) : ViewModel() {

    private val _awarenessCards = MutableStateFlow<List<EduCard>>(emptyList())
    val awarenessCards: StateFlow<List<EduCard>> = _awarenessCards

    fun loadAwarenessData() {
        viewModelScope.launch {
            val cards = repository.fetchAwarenessData()
            _awarenessCards.value = cards
        }
    }
}
