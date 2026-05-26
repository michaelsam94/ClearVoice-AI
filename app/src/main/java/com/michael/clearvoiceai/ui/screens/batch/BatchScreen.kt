package com.michael.clearvoiceai.ui.screens.batch

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.clearvoiceai.R
import com.michael.clearvoiceai.domain.model.BatchJob
import com.michael.clearvoiceai.domain.model.BatchStatus

@Composable
fun BatchScreen(
    viewModel: BatchViewModel,
    modifier: Modifier = Modifier
) {
    val jobs by viewModel.batchJobs.collectAsState()
    val context = LocalContext.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val uriStrings = uris.map { it.toString() }
            viewModel.addFilesToBatch(uriStrings)
        }
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

    val activeCount = jobs.count { it.status == BatchStatus.PROCESSING }
    val queuedCount = jobs.count { it.status == BatchStatus.QUEUED }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch("audio/*") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_batch_files_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import files to batch queue")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Files")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Batch Processing Queue",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "$activeCount active · $queuedCount queued",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "No items in queue",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.empty_batch_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("batch_jobs_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(jobs, key = { it.id }) { job ->
                        BatchJobItem(
                            job = job,
                            currentlyPlayingUri = currentlyPlayingUri,
                            onCancelClick = { viewModel.deleteJob(job.id) },
                            onItemClick = { job.voiceResultUri?.let { togglePlayback(it) } },
                            modifier = Modifier.fillMaxWidth().testTag("batch_job_item_${job.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchJobItem(
    job: BatchJob,
    currentlyPlayingUri: String?,
    onCancelClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (job.status) {
        BatchStatus.PROCESSING -> MaterialTheme.colorScheme.primary
        BatchStatus.DONE -> MaterialTheme.colorScheme.tertiary
        BatchStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isPlaying = job.voiceResultUri != null && currentlyPlayingUri == job.voiceResultUri

    Card(
        modifier = modifier
            .clickable(enabled = job.status == BatchStatus.DONE && job.voiceResultUri != null) {
                onItemClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPlaying) "🔊 Playing Clean Voice..." else "Status: ${job.status.name}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else statusColor
                    )
                }
                
                if (job.status == BatchStatus.PROCESSING || job.status == BatchStatus.QUEUED) {
                    IconButton(
                        onClick = onCancelClick,
                        modifier = Modifier.size(36.dp).testTag("cancel_job_button_${job.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancel background execution",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (job.status == BatchStatus.DONE) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                }
            }

            if (job.status == BatchStatus.PROCESSING || job.progress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { job.progress / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${job.progress.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
