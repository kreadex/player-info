package com.kreadex.playerinfo

interface AudioPlayer {
    fun loadSound(bytes: ByteArray, onLoaded: (Int) -> Unit)
    fun playSound(soundId: Int)
}