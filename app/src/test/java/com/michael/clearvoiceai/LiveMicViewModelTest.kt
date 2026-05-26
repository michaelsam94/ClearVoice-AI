package com.michael.clearvoiceai

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.michael.clearvoiceai.di.DependencyProvider
import com.michael.clearvoiceai.domain.repository.ModelRepository
import com.michael.clearvoiceai.ui.screens.livemic.LiveMicViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveMicViewModelTest {

    private val mockModelRepository = object : ModelRepository {
        override fun reset() {}
        override suspend fun runInference(
            inputFrame: FloatArray,
            cleanOutput: FloatArray,
            noiseOutput: FloatArray,
            modelType: String
        ) {
            inputFrame.copyInto(cleanOutput)
            noiseOutput.fill(0f)
        }
    }

    @Before
    fun setUp() {
        DependencyProvider.setModelRepository(mockModelRepository)
    }

    @After
    fun tearDown() {
        DependencyProvider.setModelRepository(null)
    }

    @Test
    fun testInitialState() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LiveMicViewModel(application)

        assertFalse(viewModel.isRecording.value)
        assertEquals(emptyList<com.michael.clearvoiceai.ui.screens.livemic.LiveRecording>(), viewModel.recordingList.value)
        assertEquals(emptyList<Float>(), viewModel.liveSamples.value)
    }
}
