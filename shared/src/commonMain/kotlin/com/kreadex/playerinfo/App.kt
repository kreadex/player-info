package com.kreadex.playerinfo

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import playerinfo.shared.generated.resources.Res
import kotlin.time.Duration.Companion.milliseconds

enum class Weapon { FIST, CROWBAR }

fun generateRealDossier(nickname: String, uuid: String?, isSlim: Boolean = false, isOldSkin: Boolean = false): String {
    val displayUuid = uuid ?: "NOT_FOUND"
    val status = if (uuid != null) "FOUND" else "UNKNOWN"
    val modelType = when {
        (isOldSkin) -> "OLD"
        (isSlim) -> "SLIM"
        else -> "WIDE"
    }

    return """
        > USER: $nickname
        > UUID: $displayUuid
        > STAT: $status
        > TYPE: $modelType
    """.trimIndent()
}

@Composable
fun App(audioPlayer: AudioPlayer) {
    val terminalColors = darkColorScheme(
        background = Color(0xFF21212B),
        surface = Color(0xFF323244),
        surfaceVariant = Color(0xFF455544),
        primary = Color(0xFFA1E28A),
        onSurface = Color.White
    )

    MaterialTheme(colorScheme = terminalColors) {
        val scope = rememberCoroutineScope()

        val fistSoundIds = remember { mutableStateListOf<Int>() }
        val crowbarSoundIds = remember { mutableStateListOf<Int>() }

        var typeSoundId by remember { mutableStateOf(0) }
        var uiClickSoundId by remember { mutableStateOf(0) }

        var currentWeapon by remember { mutableStateOf(Weapon.FIST) }
        var showWeaponMenu by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            try {
                audioPlayer.loadSound(Res.readBytes("files/sounds/wpn_denyselect.wav")) { uiClickSoundId = it }
                audioPlayer.loadSound(Res.readBytes("files/sounds/wpn_select.wav")) { typeSoundId = it }
            } catch (e: Exception) { println("UI Sound error") }

            listOf("metal_solid_impact_bullet1.wav", "metal_solid_impact_bullet2.wav").forEach { file ->
                try {
                    audioPlayer.loadSound(Res.readBytes("files/sounds/$file")) { crowbarSoundIds.add(it) }
                } catch (e: Exception) {}
            }
            listOf("body_medium_impact_hard1.wav", "body_medium_impact_hard2.wav", "body_medium_impact_hard3.wav", "body_medium_impact_hard4.wav", "body_medium_impact_hard5.wav", "body_medium_impact_hard6.wav").forEach { file ->
                try {
                    audioPlayer.loadSound(Res.readBytes("files/sounds/$file")) { fistSoundIds.add(it) }
                } catch (e: Exception) {}
            }
        }

        var nickname by remember { mutableStateOf("") }
        var showContent by remember { mutableStateOf(false) }
        var isSearching by remember { mutableStateOf(false) }
        var playerModelType by remember { mutableStateOf(PlayerModelType.STEVE) }

        var fullPlayerInfo by remember { mutableStateOf("") }
        var displayedInfo by remember { mutableStateOf("") }

        var skinBytes by remember { mutableStateOf<ByteArray?>(null) }

        LaunchedEffect(fullPlayerInfo) {
            displayedInfo = ""
            if (fullPlayerInfo.isNotEmpty()) {
                for (i in fullPlayerInfo.indices) {
                    displayedInfo = fullPlayerInfo.substring(0, i + 1)
                    if (fullPlayerInfo[i] != ' ' && fullPlayerInfo[i] != '\n' && typeSoundId != 0) {
                        audioPlayer.playSound(typeSoundId)
                    }
                    delay(30.milliseconds)
                }
            }
        }

        fun playClick() { if (uiClickSoundId != 0) audioPlayer.playSound(uiClickSoundId) }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "PLAYER_DATABASE",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        playClick()
                        if (nickname.isNotBlank() && !isSearching) {
                            scope.launch {
                                showContent = false
                                showWeaponMenu = false

                                delay(400)

                                isSearching = true
                                skinBytes = null

                                val playerData = MinecraftApi.getPlayerData(nickname)

                                if (playerData != null && playerData.skinBytes != null) {
                                    skinBytes = playerData.skinBytes

                                    playerModelType = when {
                                        (playerData.isSlim) -> PlayerModelType.ALEX
                                        (playerData.isOldSkin) -> PlayerModelType.OLD
                                        else -> PlayerModelType.STEVE
                                    }

                                    fullPlayerInfo = generateRealDossier(nickname, playerData.uuid, playerData.isSlim, playerData.isOldSkin)
                                } else {
                                    fullPlayerInfo = generateRealDossier(nickname, null)
                                }

                                isSearching = false
                                showContent = true
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("\uD83D\uDD0D")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Player3DView(
                                    modifier = Modifier.fillMaxSize(),
                                    modelType = playerModelType,
                                    skinBytes = skinBytes,
                                    onHit = {
                                        val soundList =
                                            if (currentWeapon == Weapon.FIST) fistSoundIds else crowbarSoundIds
                                        soundList.randomOrNull()?.let { audioPlayer.playSound(it) }
                                    },
                                    onLongPress = {
                                        playClick()
                                        showWeaponMenu = true
                                    }
                                )

                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showWeaponMenu,
                                        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                                        exit = scaleOut(targetScale = 0.8f) + fadeOut()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    Color.Black.copy(alpha = 0.7f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "👊",
                                                fontSize = 24.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        playClick()
                                                        currentWeapon = Weapon.FIST
                                                        showWeaponMenu = false
                                                    }
                                                    .padding(8.dp)
                                                    .background(
                                                        if (currentWeapon == Weapon.FIST) Color.White.copy(
                                                            0.2f
                                                        ) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "\uD83D\uDD27",
                                                fontSize = 24.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        playClick()
                                                        currentWeapon = Weapon.CROWBAR
                                                        showWeaponMenu = false
                                                    }
                                                    .padding(8.dp)
                                                    .background(
                                                        if (currentWeapon == Weapon.CROWBAR) Color.White.copy(
                                                            0.2f
                                                        ) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (displayedInfo.length < fullPlayerInfo.length) displayedInfo + "_" else displayedInfo,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}