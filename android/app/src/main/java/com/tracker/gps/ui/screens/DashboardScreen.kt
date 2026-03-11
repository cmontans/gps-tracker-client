package com.tracker.gps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracker.gps.ui.theme.ElectricBlue
import com.tracker.gps.ui.theme.NeonCyan
import com.tracker.gps.ui.viewmodel.JumpViewModel
import com.tracker.gps.util.JumpSensitivity

@Composable
fun DashboardScreen(viewModel: JumpViewModel) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val isCurrentlyJumping by viewModel.isCurrentlyJumping.collectAsState()
    val currentAltitude by viewModel.currentAltitude.collectAsState()
    val lastJumpHeight by viewModel.lastJumpHeight.collectAsState()
    val currentSensitivity by viewModel.currentSensitivity.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Text(
            text = if (isCurrentlyJumping) "JUMPING!" else if (isTracking) "WAITING FOR JUMP" else "JUMP TRACKER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentlyJumping) Color.Green else NeonCyan,
            letterSpacing = 4.sp
        )

        // Altitude Display (Big Central Meter)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ALTITUDE",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "%.1f".format(currentAltitude),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "METERS",
                fontSize = 16.sp,
                color = NeonCyan
            )
        }

        // Last Jump Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "LAST JUMP", fontSize = 12.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.1f m".format(lastJumpHeight),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
            }
        }

        // Sensitivity Selector
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SENSITIVITY", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                JumpSensitivity.values().forEach { sensitivity ->
                    OutlinedButton(
                        onClick = { viewModel.setSensitivity(sensitivity) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (currentSensitivity == sensitivity) ElectricBlue else Color.Transparent,
                            contentColor = if (currentSensitivity == sensitivity) Color.Black else Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp).weight(1f)
                    ) {
                        Text(sensitivity.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Start/Stop Button
        Button(
            onClick = { viewModel.toggleTracking(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color.Red else ElectricBlue
            )
        ) {
            Text(
                text = if (isTracking) "STOP SESSION" else "START SESSION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
