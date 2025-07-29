// MainActivity.kt
package com.example.jarvis

import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Audio.Media
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import androidx.core.net.toUri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var lastHeardText: TextView
    private var wakeWordHeard = false
    private lateinit var wakeSound: MediaPlayer
    private var commandReceived = false
    private val handler = Handler(Looper.getMainLooper())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }


        lastHeardText = findViewById(R.id.lastHeardText)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val spokenText = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.lowercase(Locale.ROOT)
                    if (spokenText != null) {
                        if (wakeWordHeard) {
                            lastHeardText.text = getString(R.string.command_heard, spokenText)
                            wakeWordHeard = false
                            commandReceived = true
                        } else if (spokenText.contains("jarvis")) {
                            lastHeardText.text = getString(R.string.wake_word_detected)
                            wakeWordHeard = true
                            commandReceived = false

                            wakeSound = MediaPlayer.create(this@MainActivity, R.raw.jarvis_wake)
                            wakeSound.setOnCompletionListener {
                                if (!commandReceived) {
                                    wakeWordHeard = false
                                    lastHeardText.text = getString(R.string.listening_for_wake_word)
                                }
                                wakeSound.release()
                            }
                            wakeSound.start()
                        } else {
                            lastHeardText.text = getString(R.string.listening_for_wake_word)
                        }
                    }
                    speechRecognizer.startListening(intent) // restart listening
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    speechRecognizer.startListening(intent) // restart even on error
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }


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

    private fun playSpotifyTrack(uri: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri.toUri()
            intent.putExtra(Intent.EXTRA_REFERRER, ("android-app://" + this.packageName).toUri())
            intent.setPackage("com.spotify.music")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Spotify not installed or failed to open.", Toast.LENGTH_SHORT).show()
            Log.e("SpotifyLaunch", "Error launching Spotify", e)
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
        speechRecognizer.destroy()
        if (::wakeSound.isInitialized) wakeSound.release()
        super.onDestroy()
    }
}
