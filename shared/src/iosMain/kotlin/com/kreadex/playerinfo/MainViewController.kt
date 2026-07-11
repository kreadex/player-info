package com.kreadex.playerinfo

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App(audioPlayer = IosAudioPlayer()) }