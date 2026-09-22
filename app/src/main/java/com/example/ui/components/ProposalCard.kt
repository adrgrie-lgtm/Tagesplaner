package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentType
import com.example.data.model.EventAdjustment
import com.example.data.model.PlanChangeProposal

@Composable
fun ProposalCard(
    proposal: PlanChangeProposal,
    onApply: () -> Unit,
    onChange: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("proposal_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F3FF)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Neuer Plan-Vorschlag",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Neuer Planungsvorschlag",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = proposal.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = proposal.reasonExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Adjustments Bullet List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                proposal.adjustments.forEach { adj ->
                    AdjustmentRow(adjustment = adj)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons: [Übernehmen], [Ändern], [Ignorieren]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("proposal_apply_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Übernehmen",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("proposal_change_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Ändern")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("proposal_dismiss_button")
                ) {
                    Text(
                        text = "Ignorieren",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustmentRow(adjustment: EventAdjustment) {
    val (bullet, color) = when (adjustment.type) {
        AdjustmentType.PROTECTED -> "🔒" to Color(0xFF283593)
        AdjustmentType.SHIFTED -> "⏱️" to Color(0xFFE65100)
        AdjustmentType.KEPT -> "✔️" to Color(0xFF2E7D32)
        AdjustmentType.BUFFER_USED -> "☕" to Color(0xFF00695C)
        AdjustmentType.CANCELLED -> "❌" to Color(0xFFC62828)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$bullet ",
            fontSize = 14.sp
        )
        Column {
            Text(
                text = adjustment.eventTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = adjustment.detail,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}
