package com.tollview.airis.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    // Private mutable status to update internally
    private val _statusMessage = MutableLiveData<String>()

    // Public immutable LiveData for observing from the UI
    val statusMessage: LiveData<String> get() = _statusMessage

    // Set default status message
    init {
        _statusMessage.value = "Ready to capture!"
    }

    // Function to update status message
    fun setStatus(message: String) {
        _statusMessage.value = message
    }
}