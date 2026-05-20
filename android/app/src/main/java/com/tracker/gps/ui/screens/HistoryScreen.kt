package com.tracker.gps.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracker.gps.model.JumpSession
import com.tracker.gps.ui.theme.NeonCyan
import com.tracker.gps.ui.viewmodel.JumpViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: JumpViewModel) {
    val history by viewModel.jumpHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "JUMP HISTORY",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No jumps recorded yet", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(history) { jump ->
                    JumpItem(jump, onDelete = { viewModel.deleteJump(jump) })
                }
            }
        }
    }
}

@Composable
fun JumpItem(jump: JumpSession, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateFormat.format(Date(jump.timestamp)),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(jump.maxHeight),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " m",
                        fontSize = 16.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = "Hangtime: %.1f s".format(jump.hangtime / 1000f),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}
