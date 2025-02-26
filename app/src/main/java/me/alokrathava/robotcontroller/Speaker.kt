package me.alokrathava.robotcontroller

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale

class Speaker(private val context: Context, private val onInitCompleted: (() -> Unit)? = null) :
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "English TTS not supported", Toast.LENGTH_SHORT).show()
            } else {
                isTtsInitialized = true
                Log.d("Speaker", "TTS initialized successfully")
                onInitCompleted?.invoke()
            }

            tts?.setSpeechRate(1.0f)  // Normal speed
            tts?.setPitch(1.0f)       // Normal pitch

        } else {
            Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    fun speak(text: String?) {
        if (!text.isNullOrBlank() && isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("Speaker", "TTS not initialized, cannot speak!")
            Toast.makeText(context, "TTS is not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
