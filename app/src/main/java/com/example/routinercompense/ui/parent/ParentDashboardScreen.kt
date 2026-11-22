package com.example.routinercompense.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay
import com.example.routinercompense.ui.theme.AppThemePalette
import com.example.routinercompense.ui.theme.DarkThemeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    childName: String,
    totalStars: Int,
    familyCode: String,
    syncStatus: String,
    routines: List<RoutineItem>,
    rewards: List<RewardItem>,
    themePalette: AppThemePalette,
    darkThemeOption: DarkThemeOption,
    onExitParentMode: () -> Unit,
    onChangeRoleClick: () -> Unit,
    onSetChildName: (String) -> Unit,
    onSetParentPin: (String) -> Unit,
    onSetTotalStars: (Int) -> Unit,
    onAddOrUpdateRoutine: (RoutineItem) -> Unit,
    onDeleteRoutine: (RoutineItem) -> Unit,
    onAddOrUpdateReward: (RewardItem) -> Unit,
    onDeleteReward: (RewardItem) -> Unit,
    onResetTodayRoutines: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectDarkOption: (DarkThemeOption) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📋 Routines", "🎁 Récompenses", "⚙️ Paramètres")

    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }
    var isAddingRoutine by remember { mutableStateOf(false) }

    var editingReward by remember { mutableStateOf<RewardItem?>(null) }
    var isAddingReward by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Espace Parent 🔒", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = syncStatus, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(text = "Code Famille : $familyCode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    TextButton(onClick = onExitParentMode) {
                        Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Quitter")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Quitter", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ParentRoutinesTab(
                    routines = routines,
                    onAddClick = { isAddingRoutine = true },
                    onEditClick = { editingRoutine = it },
                    onDeleteClick = onDeleteRoutine
                )
                1 -> ParentRewardsTab(
                    rewards = rewards,
                    onAddClick = { isAddingReward = true },
                    onEditClick = { editingReward = it },
                    onDeleteClick = onDeleteReward
                )
                2 -> ParentSettingsTab(
                    childName = childName,
                    totalStars = totalStars,
                    familyCode = familyCode,
                    themePalette = themePalette,
                    darkThemeOption = darkThemeOption,
                    onChangeRoleClick = onChangeRoleClick,
                    onSetChildName = onSetChildName,
                    onSetParentPin = onSetParentPin,
                    onSetTotalStars = onSetTotalStars,
                    onResetTodayRoutines = onResetTodayRoutines,
                    onCheckForUpdatesClick = onCheckForUpdatesClick,
                    onSelectPalette = onSelectPalette,
                    onSelectDarkOption = onSelectDarkOption
                )
            }
        }
    }

    // Routine Edit/Add Dialog
    if (isAddingRoutine || editingRoutine != null) {
        RoutineEditDialog(
            item = editingRoutine ?: RoutineItem(title = "", icon = "⭐", timeOfDay = TimeOfDay.MORNING, starsReward = 1),
            onDismiss = {
                isAddingRoutine = false
                editingRoutine = null
            },
            onSave = { updated ->
                onAddOrUpdateRoutine(updated)
                isAddingRoutine = false
                editingRoutine = null
            }
        )
    }

    // Reward Edit/Add Dialog
    if (isAddingReward || editingReward != null) {
        RewardEditDialog(
            item = editingReward ?: RewardItem(title = "", icon = "🎁", starCost = 5),
            onDismiss = {
                isAddingReward = false
                editingReward = null
            },
            onSave = { updated ->
                onAddOrUpdateReward(updated)
                isAddingReward = false
                editingReward = null
            }
        )
    }
}

@Composable
private fun ParentRoutinesTab(
    routines: List<RoutineItem>,
    onAddClick: () -> Unit,
    onEditClick: (RoutineItem) -> Unit,
    onDeleteClick: (RoutineItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(routines, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "${item.timeOfDay.displayName} • +${item.starsReward} ⭐",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { onEditClick(item) }) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteClick(item) }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter routine")
        }
    }
}

@Composable
private fun ParentRewardsTab(
    rewards: List<RewardItem>,
    onAddClick: () -> Unit,
    onEditClick: (RewardItem) -> Unit,
    onDeleteClick: (RewardItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rewards, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Coût : ${item.starCost} ⭐ • Débloqué ${item.timesClaimed} fois",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { onEditClick(item) }) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteClick(item) }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter récompense")
        }
    }
}

@Composable
private fun ParentSettingsTab(
    childName: String,
    totalStars: Int,
    familyCode: String,
    themePalette: AppThemePalette,
    darkThemeOption: DarkThemeOption,
    onChangeRoleClick: () -> Unit,
    onSetChildName: (String) -> Unit,
    onSetParentPin: (String) -> Unit,
    onSetTotalStars: (Int) -> Unit,
    onResetTodayRoutines: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectDarkOption: (DarkThemeOption) -> Unit
) {
    var nameInput by remember(childName) { mutableStateOf(childName) }
    var pinInput by remember { mutableStateOf("") }
    var starsInput by remember(totalStars) { mutableStateOf(totalStars.toString()) }

    var showResetConfirmation by remember { mutableStateOf(false) }
    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var saveNotificationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveNotificationMessage) {
        if (saveNotificationMessage != null) {
            kotlinx.coroutines.delay(3000)
            saveNotificationMessage = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Family Code Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📲 Code de Synchronisation Famille", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entrez ce code sur le téléphone de l'enfant et des autres parents pour relier les 3 appareils :",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = familyCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Theme & Color Palette Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎨 Thème et Couleurs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Personnaliser l'apparence visuelle de l'application.", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Mode d'affichage :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkThemeOption.values().forEach { option ->
                            FilterChip(
                                selected = darkThemeOption == option,
                                onClick = { onSelectDarkOption(option) },
                                label = { Text(option.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Palette de couleurs :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppThemePalette.values().forEach { palette ->
                            FilterChip(
                                selected = themePalette == palette,
                                onClick = { onSelectPalette(palette) },
                                label = { Text(palette.displayName) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.primaryColor)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Child Name Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "👶 Prénom de l'enfant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Prénom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onSetChildName(nameInput)
                                saveNotificationMessage = "Le prénom $nameInput a été enregistré avec succès."
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enregistrer le prénom")
                    }
                }
            }
        }

        // Stars Balance Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⭐ Ajuster le solde d'étoiles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = starsInput,
                        onValueChange = { starsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Nombre d'étoiles") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val count = starsInput.toIntOrNull() ?: 0
                            onSetTotalStars(count)
                            saveNotificationMessage = "Modification enregistrée ($count étoiles)"
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Mettre à jour les étoiles")
                    }
                }
            }
        }

        // Parent PIN Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔑 Changer le code PIN parent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("Nouveau code PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (pinInput.isNotEmpty()) {
                                onSetParentPin(pinInput)
                                pinInput = ""
                                saveNotificationMessage = "Modification enregistrée (Nouveau PIN)"
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Changer le PIN")
                    }
                }
            }
        }

        // Change Role / Reset Device Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📱 Rôle du téléphone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Changer l'utilisation de cet appareil (Passer de Parent à Enfant ou réinitialiser le Code Famille).", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onChangeRoleClick
                    ) {
                        Text("Changer le rôle ou le Code Famille")
                    }
                }
            }
        }

        // Check For Updates
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🚀 Mise à jour de l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Vérifier si une nouvelle version de Routine & Récompense est disponible.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCheckForUpdatesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rechercher une mise à jour", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // App Version Info Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVersionInfoDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ℹ️ Version de l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Version 2.0 (Build 2) • Toucher pour afficher le numéro", fontSize = 13.sp, color = Color.Gray)
                    }
                    Text(text = "v2.0", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Reset Routines
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔄 Réinitialiser la journée", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Décocher toutes les routines accomplies aujourd'hui.", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showResetConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Réinitialiser les routines")
                    }
                }
            }
        }
    }

    if (showVersionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showVersionInfoDialog = false },
            icon = { Text(text = "✨", fontSize = 40.sp) },
            title = {
                Text(
                    text = "Routine & Récompenses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVersionInfoDialog = false }
                ) {
                    Text(
                        text = "Version 2.0 (Build 2)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Application synchronisée & à jour.\n\nToucher l'écran pour fermer.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionInfoDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    saveNotificationMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { saveNotificationMessage = null },
            icon = { Text(text = "✅", fontSize = 40.sp) },
            title = {
                Text(
                    text = "Modification effectuée",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveNotificationMessage = null }
                ) {
                    Text(
                        text = msg,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toucher l'écran pour fermer (3s).",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { saveNotificationMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Confirmer la réinitialisation") },
            text = { Text("Voulez-vous vraiment réinitialiser toutes les routines d'aujourd'hui ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTodayRoutines()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun RoutineEditDialog(
    item: RoutineItem,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var icon by remember { mutableStateOf(item.icon) }
    var timeOfDay by remember { mutableStateOf(item.timeOfDay) }
    var starsReward by remember { mutableStateOf(item.starsReward.toString()) }
    var showIconLibrary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.title.isEmpty()) "Ajouter une routine" else "Modifier la routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la tâche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Icône :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = icon, fontSize = 26.sp)
                        }
                    }
                    OutlinedButton(onClick = { showIconLibrary = true }) {
                        Text("🎨 Choisir dans la bibliothèque")
                    }
                }

                Text(text = "Moment de la journée :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeOfDay.values().forEach { tod ->
                        FilterChip(
                            selected = timeOfDay == tod,
                            onClick = { timeOfDay = tod },
                            label = { Text(tod.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = starsReward,
                    onValueChange = { starsReward = it.filter { c -> c.isDigit() } },
                    label = { Text("Récompense en étoiles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val stars = starsReward.toIntOrNull() ?: 1
                        onSave(item.copy(title = title, icon = icon, timeOfDay = timeOfDay, starsReward = stars))
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    if (showIconLibrary) {
        IconLibraryDialog(
            selectedIcon = icon,
            onDismiss = { showIconLibrary = false },
            onIconSelected = { selected ->
                icon = selected
                showIconLibrary = false
            }
        )
    }
}

@Composable
private fun RewardEditDialog(
    item: RewardItem,
    onDismiss: () -> Unit,
    onSave: (RewardItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var icon by remember { mutableStateOf(item.icon) }
    var starCost by remember { mutableStateOf(item.starCost.toString()) }
    var showIconLibrary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.title.isEmpty()) "Ajouter une récompense" else "Modifier la récompense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la récompense") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Icône :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = icon, fontSize = 26.sp)
                        }
                    }
                    OutlinedButton(onClick = { showIconLibrary = true }) {
                        Text("🎨 Choisir dans la bibliothèque")
                    }
                }

                OutlinedTextField(
                    value = starCost,
                    onValueChange = { starCost = it.filter { c -> c.isDigit() } },
                    label = { Text("Coût en étoiles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val cost = starCost.toIntOrNull() ?: 5
                        onSave(item.copy(title = title, icon = icon, starCost = cost))
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    if (showIconLibrary) {
        IconLibraryDialog(
            selectedIcon = icon,
            onDismiss = { showIconLibrary = false },
            onIconSelected = { selected ->
                icon = selected
                showIconLibrary = false
            }
        )
    }
}

@Composable
private fun IconLibraryDialog(
    selectedIcon: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val categories = mapOf(
        "🌅 Matin" to listOf("🛏️", "🪥", "🚿", "🧼", "👗", "💇", "🚽", "🧦", "👟", "🧴"),
        "🥣 Repas" to listOf("🥣", "🍎", "🥛", "🍽️", "🥦", "🧃", "🍌", "🥞", "🥪", "🥤"),
        "🎒 Devoirs" to listOf("🎒", "📚", "📖", "✏️", "💻", "🎨", "📐", "📝", "🧩", "🎹"),
        "🧹 Rangement" to listOf("🧹", "🧸", "🧺", "🐶", "🐱", "🪴", "🗑️", "🧽", "📦", "🚗"),
        "🌙 Nuit" to listOf("🌙", "🥱", "📖", "🧸", "🤫", "⏰", "🛌", "💡", "✨", "💤"),
        "⚽ Loisirs" to listOf("⚽", "🚴", "🏊", "🎮", "🛹", "🏃", "🛝", "🎯", "🎲", "🏆")
    )

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categoryNames = categories.keys.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "🎨 Bibliothèque d'icônes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(selectedTabIndex = selectedCategoryIndex) {
                    categoryNames.forEachIndexed { index, name ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val currentIcons = categories[categoryNames[selectedCategoryIndex]] ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(currentIcons) { emoji ->
                        Surface(
                            onClick = {
                                onIconSelected(emoji)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedIcon == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
