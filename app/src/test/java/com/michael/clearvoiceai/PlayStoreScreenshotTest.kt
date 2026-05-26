package com.michael.clearvoiceai

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.michael.clearvoiceai.di.DependencyProvider
import com.michael.clearvoiceai.domain.model.BatchJob
import com.michael.clearvoiceai.domain.model.BatchStatus
import com.michael.clearvoiceai.domain.repository.AudioRepository
import com.michael.clearvoiceai.domain.repository.BatchRepository
import com.michael.clearvoiceai.domain.repository.ModelRepository
import com.michael.clearvoiceai.ui.screens.batch.BatchScreen
import com.michael.clearvoiceai.ui.screens.batch.BatchViewModel
import com.michael.clearvoiceai.ui.screens.home.ProcessScreen
import com.michael.clearvoiceai.ui.screens.home.ProcessViewModel
import com.michael.clearvoiceai.ui.screens.livemic.LiveMicScreen
import com.michael.clearvoiceai.ui.screens.livemic.LiveMicViewModel
import com.michael.clearvoiceai.ui.screens.settings.SettingsScreen
import com.michael.clearvoiceai.ui.screens.settings.SettingsViewModel
import com.michael.clearvoiceai.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayStoreScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val jobsFlow = MutableStateFlow<List<BatchJob>>(emptyList())

    private val mockBatchRepository = object : BatchRepository {
        override fun getAllJobs(): Flow<List<BatchJob>> = jobsFlow
        override suspend fun getJobById(jobId: Long): BatchJob? = jobsFlow.value.find { it.id == jobId }
        override suspend fun insertJob(job: BatchJob): Long = 1
        override suspend fun updateStatus(jobId: Long, status: String) {}
        override suspend fun updateProgress(jobId: Long, progress: Float) {}
        override suspend fun updateResults(jobId: Long, voiceResultUri: String, noiseResultUri: String) {}
        override suspend fun deleteJob(jobId: Long) {}
    }

    private val mockModelRepository = object : ModelRepository {
        override fun reset() {}
        override suspend fun runInference(
            inputFrame: FloatArray,
            cleanOutput: FloatArray,
            noiseOutput: FloatArray,
            modelType: String
        ) {}
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        
        try {
            val config = androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            androidx.work.WorkManager.initialize(context, config)
        } catch (e: Exception) {
            // suppressed
        }

        DependencyProvider.setBatchRepository(mockBatchRepository)
        DependencyProvider.setModelRepository(mockModelRepository)
    }

    @After
    fun tearDown() {
        DependencyProvider.setBatchRepository(null)
        DependencyProvider.setModelRepository(null)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi", sdk = [35])
    fun capture_01_dashboard() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ProcessViewModel(app)
        
        vm.setSelectedFile(Uri.parse("content://media/external/audio/media/1"))
        
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ProcessScreen(viewModel = vm)
            }
        }
        
        composeTestRule.onRoot().captureRoboImage("../play-store/phone/01_dashboard.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi", sdk = [35])
    fun capture_02_livemic() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LiveMicViewModel(app)
        
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                LiveMicScreen(viewModel = vm)
            }
        }
        
        composeTestRule.onRoot().captureRoboImage("../play-store/phone/02_livemic.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi", sdk = [35])
    fun capture_03_batch() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        
        jobsFlow.value = listOf(
            BatchJob(1, "interview_clip.mp3", "content://1", BatchStatus.DONE, 1f, "content://voice1", "content://noise1", System.currentTimeMillis() - 60000),
            BatchJob(2, "street_recording.m4a", "content://2", BatchStatus.PROCESSING, 0.45f, null, null, System.currentTimeMillis() - 30000),
            BatchJob(3, "lecture_05.wav", "content://3", BatchStatus.QUEUED, 0f, null, null, System.currentTimeMillis() - 10000)
        )
        
        val vm = BatchViewModel(app)
        
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                BatchScreen(viewModel = vm)
            }
        }
        
        composeTestRule.onRoot().captureRoboImage("../play-store/phone/03_batch.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi", sdk = [35])
    fun capture_04_settings() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(app)
        
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                SettingsScreen(viewModel = vm)
            }
        }
        
        composeTestRule.onRoot().captureRoboImage("../play-store/phone/04_settings.png")
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi", sdk = [35])
    fun capture_tablet_01_dashboard() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ProcessViewModel(app)
        vm.setSelectedFile(Uri.parse("content://media/external/audio/media/1"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ProcessScreen(viewModel = vm)
            }
        }
        composeTestRule.onRoot().captureRoboImage("../play-store/tablet/01_dashboard.png")
    }
    
    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi", sdk = [35])
    fun capture_tablet_02_livemic() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LiveMicViewModel(app)
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                LiveMicScreen(viewModel = vm)
            }
        }
        composeTestRule.onRoot().captureRoboImage("../play-store/tablet/02_livemic.png")
    }
}
