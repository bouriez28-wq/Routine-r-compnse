package com.example.routinercompense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.routinercompense.model.RewardItem

@Composable
fun ChildRewardsScreen(
    rewards: List<RewardItem>,
    totalStars: Int,
    onClaimReward: (RewardItem) -> Boolean,
    modifier: Modifier = Modifier
) {
    var claimedSuccessReward by remember { mutableStateOf<RewardItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Star Balance Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⭐", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ton trésor d'étoiles",
                        fontSize = 14.sp,
                        color = Color(0xFFF57F17),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalStars étoiles disponibles",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // Rewards Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(rewards, key = { it.id }) { reward ->
                RewardCard(
                    reward = reward,
                    totalStars = totalStars,
                    onClaim = {
                        val success = onClaimReward(reward)
                        if (success) {
                            claimedSuccessReward = reward
                        }
                    }
                )
            }
        }
    }

    // Success Dialog
    claimedSuccessReward?.let { reward ->
        AlertDialog(
            onDismissRequest = { claimedSuccessReward = null },
            icon = { Text(text = "🎉 ${reward.icon} 🎉", fontSize = 48.sp) },
            title = {
                Text(
                    text = "Bravo !!!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Tu as débloqué : ${reward.title} !\nMontre cet écran à maman ou papa pour profiter de ta récompense !",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { claimedSuccessReward = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Super ! 😃")
                }
            }
        )
    }
}

@Composable
private fun RewardCard(
    reward: RewardItem,
    totalStars: Int,
    onClaim: () -> Unit
) {
    val canAfford = totalStars >= reward.starCost
    val starsNeeded = reward.starCost - totalStars

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canAfford) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = reward.icon, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = reward.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cost tag
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${reward.starCost} ⭐",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFF57F17),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Claim button
            if (canAfford) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Débloquer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = { },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Encore $starsNeeded ⭐",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
