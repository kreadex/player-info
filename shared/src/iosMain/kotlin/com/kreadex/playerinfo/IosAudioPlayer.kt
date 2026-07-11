package com.kreadex.playerinfo

import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer

class IosAudioPlayer : AudioPlayer {
    private val players = mutableMapOf<Int, AVAudioPlayer>()
    private var nextId = 1

    @OptIn(ExperimentalForeignApi::class)
    override fun loadSound(bytes: ByteArray, onLoaded: (Int) -> Unit) {
        if (bytes.isEmpty()) return

        val nsData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }

        val player = AVAudioPlayer(data = nsData, error = null)
        player.prepareToPlay()

        val id = nextId++
        players[id] = player
        onLoaded(id)
    }

    override fun playSound(soundId: Int) {
        players[soundId]?.apply {
            if (playing) {
                stop()
                currentTime = 0.0
            }
            play()
        }
    }
}