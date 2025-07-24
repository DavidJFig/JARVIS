// MainActivity.kt
package com.example.jarvis

import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private var availableVoices: List<Voice> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.testVoiceButton).setOnClickListener {
            speak("Welcome back")
            speak("")
            speak("Sir.")
        }

        findViewById<Button>(R.id.testVolumeButton).setOnClickListener {
            setMediaVolume(0.6f)
            statusText.text = getString(R.string.volume_set)
        }
    }

    private fun speak(text: String) {
        tts.voice = Voice("en-gb-x-rjs-local", Locale.UK, 1, 1, false, emptySet())
        tts.setPitch(1.0f)
        tts.setSpeechRate(1.0f)

        if (text.isEmpty()) {
            tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, null)
        }
        else {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    private fun setMediaVolume(level: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (level * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            statusText.text = getString(R.string.tts_ready)
        } else {
            statusText.text = getString(R.string.tts_failed)
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
