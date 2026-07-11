package com.kreadex.playerinfo

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class PlayerData(
    val nickname: String,
    val uuid: String,
    val skinBytes: ByteArray?,
    val isSlim: Boolean,
    val isOldSkin: Boolean
)

object MinecraftApi {
    private val client = HttpClient()

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getPlayerData(nickname: String): PlayerData? = withContext(Dispatchers.IO) {
        try {
            val response1 = client.get("https://api.mojang.com/users/profiles/minecraft/$nickname")
            val profileText = response1.bodyAsText()

            val uuidRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            val uuid = uuidRegex.find(profileText)?.groups?.get(1)?.value

            if (uuid == null) {
                return@withContext null
            }

            val response2 = client.get("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
            val sessionText = response2.bodyAsText()

            val valueRegex = """"value"\s*:\s*"([^"]+)"""".toRegex()
            val base64Value = valueRegex.find(sessionText)?.groups?.get(1)?.value

            if (base64Value == null) {
                return@withContext null
            }

            val decodedJson = Base64.decode(base64Value).decodeToString()

            val urlRegex = """"SKIN"\s*:\s*\{\s*"url"\s*:\s*"([^"]+)"""".toRegex()
            val rawSkinUrl = urlRegex.find(decodedJson)?.groups?.get(1)?.value

            if (rawSkinUrl == null) {
                return@withContext null
            }

            val skinUrl = rawSkinUrl.replace("http://", "https://")

            val modelRegex = """"model"\s*:\s*"([^"]+)"""".toRegex()
            val modelValue = modelRegex.find(decodedJson)?.groups?.get(1)?.value
            val isSlim = modelValue == "slim"

            val skinBytes: ByteArray = client.get(skinUrl).readBytes()

            val (width, height) = getPngSize(skinBytes) ?: (0 to 0)

            val isOldSkin = width == 64 && height == 32

            return@withContext PlayerData(nickname, uuid, skinBytes, isSlim, isOldSkin)

        } catch (e: Exception) {
            println("ERR (Ktor): ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    fun getPngSize(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 24) return null

        val signature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        )

        if (!bytes.copyOfRange(0, 8).contentEquals(signature)) {
            return null
        }

        fun readInt(offset: Int): Int =
            ((bytes[offset].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 3].toInt() and 0xFF)

        val width = readInt(16)
        val height = readInt(20)

        return width to height
    }
}