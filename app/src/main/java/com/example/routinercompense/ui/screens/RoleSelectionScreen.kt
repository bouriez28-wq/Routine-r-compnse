package com.example.routinercompense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routinercompense.model.DeviceRole

@Composable
fun RoleSelectionScreen(
    currentFamilyCode: String,
    onRoleSelected: (DeviceRole, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf<DeviceRole?>(null) }
    var familyCodeInput by remember { mutableStateOf(currentFamilyCode) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✨", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bienvenue sur\nRoutine & Récompense",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "À qui appartient ce téléphone ?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Option Child
        RoleCard(
            title = "👦 Téléphone de l'Enfant",
            subtitle = "Affichage des routines, gain d'étoiles et déblocage de récompenses",
            isSelected = selectedRole == DeviceRole.CHILD,
            onClick = {
                selectedRole = DeviceRole.CHILD
                errorMessage = ""
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Option Parent
        RoleCard(
            title = "👨‍👩‍👧 Téléphone du Parent",
            subtitle = "Gestion complète : créer les routines, ajouter des récompenses, suivre les progrès",
            isSelected = selectedRole == DeviceRole.PARENT,
            onClick = {
                selectedRole = DeviceRole.PARENT
                errorMessage = ""
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedRole != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔑 Code Famille (Synchronisation)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (selectedRole == DeviceRole.CHILD)
                            "Entrez le même code famille que celui affiché sur le téléphone des parents."
                        else
                            "Partagez ce code avec les autres téléphones (parents et enfant) pour tout synchroniser.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = familyCodeInput,
                        onValueChange = {
                            familyCodeInput = it.uppercase()
                            errorMessage = ""
                        },
                        label = { Text("Code Famille (ex: FAMILLE-1234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (familyCodeInput.isBlank()) {
                        errorMessage = "Veuillez indiquer un Code Famille."
                    } else {
                        onRoleSelected(selectedRole!!, familyCodeInput)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Valider et démarrer 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val titleColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}
