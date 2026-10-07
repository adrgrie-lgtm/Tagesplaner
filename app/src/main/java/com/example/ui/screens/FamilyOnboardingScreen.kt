package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FamilyOnboardingScreen(
    onCreateFamily: (familyName: String, myName: String, role: String, emoji: String) -> Unit,
    onJoinFamily: (inviteCode: String, myName: String, role: String, emoji: String) -> Unit,
    onContinueOffline: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Erstellen, 1: Beitreten

    // Form inputs
    var familyNameInput by remember { mutableStateOf("Familie Sonnenschein") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var myNameInput by remember { mutableStateOf("Papa") }
    var selectedRole by remember { mutableStateOf("Papa") }
    var selectedEmoji by remember { mutableStateOf("👨") }

    val rolesWithEmoji = listOf(
        Triple("Papa", "👨", "Elternteil"),
        Triple("Mama", "👩", "Elternteil"),
        Triple("Tochter", "👧", "Kind"),
        Triple("Sohn", "👦", "Kind"),
        Triple("Sonstiges", "👤", "Familienmitglied")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("family_onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App / Family Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Familie",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Familienplaner Synchronisation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Nutzt denselben Plan gemeinsam auf 3–4 Android-Geräten. Vollständig offline-fähig.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Familie gründen", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Beitreten", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // NEUE FAMILIE ERSTELLEN
                    OutlinedTextField(
                        value = familyNameInput,
                        onValueChange = { familyNameInput = it },
                        label = { Text("Familienname") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_family_name"),
                        singleLine = true
                    )
                } else {
                    // FAMILIE BEITRETEN
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.uppercase() },
                        label = { Text("Einladungs-Code (z.B. ABCD-1234)") },
                        placeholder = { Text("XXXX-XXXX") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_invite_code"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dein Name auf diesem Gerät
                OutlinedTextField(
                    value = myNameInput,
                    onValueChange = { myNameInput = it },
                    label = { Text("Dein Name auf diesem Gerät") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Deine Rolle:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rolesWithEmoji.take(4).forEach { (r, e, _) ->
                        val isSelected = selectedRole == r
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedRole = r
                                    selectedEmoji = e
                                    if (myNameInput.isBlank() || myNameInput in rolesWithEmoji.map { it.first }) {
                                        myNameInput = r
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = e, fontSize = 22.sp)
                                Text(
                                    text = r,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            onCreateFamily(
                                familyNameInput.trim().ifBlank { "Meine Familie" },
                                myNameInput.trim().ifBlank { selectedRole },
                                selectedRole,
                                selectedEmoji
                            )
                        } else {
                            onJoinFamily(
                                inviteCodeInput.trim(),
                                myNameInput.trim().ifBlank { selectedRole },
                                selectedRole,
                                selectedEmoji
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_onboarding_button"),
                    enabled = !isLoading && (selectedTab == 0 || inviteCodeInput.isNotBlank())
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        val label = if (selectedTab == 0) "Familie erstellen & loslegen" else "Familie beitreten"
                        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onContinueOffline,
            modifier = Modifier.testTag("continue_offline_button")
        ) {
            Text("Vorerst rein lokal / offline fortfahren")
        }
    }
}
