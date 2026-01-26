package com.example.theloop

import androidx.test.core.app.ApplicationProvider
import com.example.theloop.data.repository.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        userPreferencesRepository = UserPreferencesRepository(context)
        viewModel = OnboardingViewModel(userPreferencesRepository)
    }

    @Test
    fun saveName_withBlankName_setsErrorAndReturnsFalse() {
        viewModel.onNameChange("   ")
        val result = viewModel.saveName()

        assertFalse(result)
        assertEquals(R.string.error_name_blank, viewModel.nameError.value)
    }

    @Test
    fun saveName_withValidName_clearsErrorAndReturnsTrue() {
        viewModel.onNameChange("Valid Name")
        val result = viewModel.saveName()

        assertTrue(result)
        assertNull(viewModel.nameError.value)
    }

    @Test
    fun onNameChange_clearsError() {
        // Set error first
        viewModel.onNameChange("")
        viewModel.saveName()
        assertEquals(R.string.error_name_blank, viewModel.nameError.value)

        // Change name
        viewModel.onNameChange("New Name")
        assertNull(viewModel.nameError.value)
    }
}
