package com.example.theloop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theloop.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _nameError = MutableStateFlow<Int?>(null)
    val nameError: StateFlow<Int?> = _nameError.asStateFlow()

    fun onNameChange(newName: String) {
        _name.value = newName
        _nameError.value = null
    }

    fun saveName(): Boolean {
        val isNameValid = name.value.isNotBlank()
        if (isNameValid) {
            _nameError.value = null
            viewModelScope.launch {
                userPreferencesRepository.saveUserName(name.value)
            }
        } else {
            _nameError.value = R.string.error_name_blank
        }
        return isNameValid
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.completeOnboarding()
        }
    }
}
