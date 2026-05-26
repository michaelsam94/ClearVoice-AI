package com.michael.clearvoiceai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.clearvoiceai.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayStoreFeatureGraphicTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi", sdk = [35])
    fun capture_feature_graphic() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F111A),
                                    Color(0xFF1E213A),
                                    Color(0xFF0B0D12)
                                )
                            )
                        )
                        .padding(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ClearVoice AI",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "High-Fidelity On-Device Speech Enhancer",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "Isolate speech and eliminate background noise in real-time, completely offline with high-fidelity multiband isolation.",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0C0E12))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🎙️",
                                        fontSize = 54.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "STUDIO CLEAN",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("../play-store/feature-graphic.png")
    }
}
