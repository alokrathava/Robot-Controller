package me.alokrathava.robotcontroller

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale

// Speaker no longer implements TextToSpeech.OnInitListener directly on the class
// to avoid ClassNotFoundException in Layoutlib/Compose Preview environments where
// android.speech.tts classes are not available.
class Speaker(private val context: Context, private val onInitCompleted: (() -> Unit)? = null) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        try {
            tts = TextToSpeech(context) { status ->
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
        } catch (e: Throwable) {
            Log.e("Speaker", "TTS initialization failed or not supported in current environment", e)
        }
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Throwable) {
            Log.e("Speaker", "Error stopping TTS", e)
        }
    }

    fun speak(text: String?) {
        if (!text.isNullOrBlank() && isTtsInitialized) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: Throwable) {
                Log.e("Speaker", "Error speaking text", e)
            }
        } else {
            Log.w("Speaker", "TTS not initialized, cannot speak!")
            Toast.makeText(context, "TTS is not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Throwable) {
            Log.e("Speaker", "Error shutting down TTS", e)
        }
    }
}
