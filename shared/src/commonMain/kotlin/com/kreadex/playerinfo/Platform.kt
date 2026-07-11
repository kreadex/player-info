package com.kreadex.playerinfo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform