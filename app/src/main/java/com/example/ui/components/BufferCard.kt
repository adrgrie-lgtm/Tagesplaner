package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TimelineEntry
import com.example.ui.theme.BufferBorder
import com.example.ui.theme.BufferContainer
import com.example.ui.theme.BufferGreen

@Composable
fun BufferCard(
    buffer: TimelineEntry.BufferEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("buffer_card_${buffer.startTime}"),
        colors = CardDefaults.cardColors(
            containerColor = BufferContainer.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BufferBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = "Puffer",
                    tint = BufferGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${buffer.durationMinutes} Min. Puffer & Freiraum",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BufferGreen
                    )
                    Text(
                        text = "Fängt Verzögerungen auf – bewusst unverplant",
                        style = MaterialTheme.typography.bodySmall,
                        color = BufferGreen.copy(alpha = 0.85f)
                    )
                }
            }

            Text(
                text = buffer.timeRangeFormatted(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = BufferGreen.copy(alpha = 0.8f)
            )
        }
    }
}
