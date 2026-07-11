import android.content.Context
import android.media.SoundPool
import com.kreadex.playerinfo.AudioPlayer
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private val soundPool = SoundPool.Builder().setMaxStreams(5).build()

    private val callbacks = mutableMapOf<Int, (Int) -> Unit>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                callbacks[sampleId]?.invoke(sampleId)
                callbacks.remove(sampleId)
            } else {
                println("SoundPool error: $sampleId")
            }
        }
    }

    override fun loadSound(bytes: ByteArray, onLoaded: (Int) -> Unit) {
        try {
            val tempFile = File.createTempFile("sound_", null, context.cacheDir)
            tempFile.writeBytes(bytes)

            val id = soundPool.load(tempFile.absolutePath, 1)

            callbacks[id] = onLoaded
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}