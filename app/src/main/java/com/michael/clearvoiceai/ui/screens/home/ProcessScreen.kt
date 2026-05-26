package com.michael.clearvoiceai.ui.screens.home

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.clearvoiceai.R
import com.michael.clearvoiceai.ui.components.WaveformCanvas

@Composable
fun ProcessScreen(
    viewModel: ProcessViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedUri by viewModel.selectedUri.collectAsState()
    val voiceWaveform by viewModel.voiceWaveform.collectAsState()
    val noiseWaveform by viewModel.noiseWaveform.collectAsState()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            currentlyPlayingUri = null
        }
        viewModel.setSelectedFile(uri)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlayback(uriStr: String) {
        try {
            if (currentlyPlayingUri == uriStr && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                currentlyPlayingUri = null
            } else {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                
                mediaPlayer = MediaPlayer.create(context, Uri.parse(uriStr)).apply {
                    start()
                }
                currentlyPlayingUri = uriStr
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ClearVoice Dual-Track",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { filePickerLauncher.launch("audio/*") }
                .testTag("file_picker_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio File Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedUri == null) {
                    Text(
                        text = stringResource(R.string.import_card_title),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.import_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "File Chosen!",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = selectedUri?.path?.substringAfterLast("/") ?: "Selected Audio Source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(visible = selectedUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isPlaying = currentlyPlayingUri == selectedUri.toString() && mediaPlayer?.isPlaying == true
                Button(
                    onClick = { togglePlayback(selectedUri.toString()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("play_original_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play Original"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlaying) "Stop Original" else "Listen Original")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                    mediaPlayer?.reset()
                    currentlyPlayingUri = null
                }
                viewModel.startProcessing()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_processing_button"),
            enabled = selectedUri != null && uiState !is ProcessState.Loading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.btn_start_processing),
                fontWeight = FontWeight.Bold
            )
        }

        when (val state = uiState) {
            is ProcessState.Loading -> {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("process_progress_bar"),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("Completed: %.1f%%", state.progress * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            is ProcessState.Error -> {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = state.errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }

            is ProcessState.Success -> {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dual-Track Results",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "🎤 Voice (Clean Speech)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        WaveformCanvas(samples = voiceWaveform, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = { togglePlayback(state.voiceUri) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("play_voice_stem_button")
                            ) {
                                val isPlaying = currentlyPlayingUri == state.voiceUri && mediaPlayer?.isPlaying == true
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Playback Action"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Listen Voice")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Ambient noise context (Isolated Noise)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        WaveformCanvas(samples = noiseWaveform, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = { togglePlayback(state.noiseUri) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("play_noise_stem_button")
                            ) {
                                val isPlaying = currentlyPlayingUri == state.noiseUri && mediaPlayer?.isPlaying == true
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Playback Action"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Listen Ambient")
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
