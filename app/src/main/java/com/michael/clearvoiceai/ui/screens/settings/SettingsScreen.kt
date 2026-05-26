package com.michael.clearvoiceai.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.clearvoiceai.R

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val themePref by viewModel.themePref.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ClearVoice Preferences",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Source,
                        contentDescription = "ML Engine type selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_select_model),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateModelType("FAST") }
                            .testTag("model_fast_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedModel == "FAST") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Fast\n(RNNoise)",
                            modifier = Modifier.padding(12.dp),
                            color = if (selectedModel == "FAST") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateModelType("QUALITY") }
                            .testTag("model_quality_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedModel == "QUALITY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Quality\n(DNS-48)",
                            modifier = Modifier.padding(12.dp),
                            color = if (selectedModel == "QUALITY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Audio export codec selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_export_format),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateExportFormat("WAV") }
                            .testTag("export_wav_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportFormat == "WAV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "WAV\n(Lossless)",
                            modifier = Modifier.padding(12.dp),
                            color = if (exportFormat == "WAV") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateExportFormat("AAC") }
                            .testTag("export_aac_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportFormat == "AAC") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AAC\n(Compressed)",
                            modifier = Modifier.padding(12.dp),
                            color = if (exportFormat == "AAC") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Visual theme setting",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_theme),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SYSTEM", "DARK", "LIGHT").forEach { theme ->
                        val selected = themePref == theme
                        val label = when (theme) {
                            "DARK" -> stringResource(R.string.theme_dark)
                            "LIGHT" -> stringResource(R.string.theme_light)
                            else -> stringResource(R.string.theme_system)
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateThemePreference(theme) }
                                .testTag("theme_card_${theme.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Trust summary icon",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ClearVoice AI is fully offline",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Zero transmissions. Real-time local machine intelligence completely safe in your possession.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
