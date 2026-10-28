package com.nomorereels.app.action

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import android.view.KeyEvent

/**
 * Farkındalık Kapısı (Mindful Gateway) veya koruma katmanı ekranda belirdiğinde,
 * arkada oynatılmaya devam eden Reels video ve sesini derhal duraklatan (pause)
 * ve bilinçli izin verildiğinde veya çıkıldığında odağı serbest bırakan yardımcı sınıf.
 *
 * Ekrana ASLA tıklama yapmaz; böylece sponsorlu/reklam Reels videolarında reklama
 * veya tarayıcıya yönlendirme riski %100 önlenir.
 */
object MediaPauseHelper {
    private const val TAG = "NoMoreReels"

    @Volatile
    private var hasAudioFocus = false
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    @Volatile
    private var silentAudioTrack: AudioTrack? = null

    /**
     * Medya oynatımını ve sesini geçici ses odağı ile duraklatır:
     * 1. Geçici ses odağı (AUDIOFOCUS_GAIN_TRANSIENT) talep eder (kapı kapandığında bırakılır).
     * 2. Sessiz bir AudioTrack döngüsü başlatarak Android ve Samsung AudioService'in
     *    Instagram ExoPlayer'ı derhal duraklatmasını zorunlu kılar.
     * Not: Küresel KEYCODE_MEDIA_PAUSE gönderilmez; böylece arka plandaki Spotify/YouTube Music
     * gibi medya oturumları kalıcı olarak durdurulmaz, geçici odak bırakıldığında devam edebilir.
     */
    @Synchronized
    fun pauseMedia(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        // Geçici Ses Odağı (Transient Audio Focus) Talep Et - USAGE_MEDIA ve CONTENT_TYPE_MUSIC
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "MediaPauseHelper: Ses odağı durumu değişti: $focusChange")
                    }
                    audioFocusListener = listener

                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(attrs)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener(listener)
                        .build()
                }

                audioFocusRequest?.let { req ->
                    val result = audioManager.requestAudioFocus(req)
                    hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    Log.i(TAG, "MediaPauseHelper: Geçici ses odağı talep edildi, sonuç: $result")
                }
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                Log.i(TAG, "MediaPauseHelper (Legacy): Geçici ses odağı talep edildi, sonuç: $result")
            }

            // 3. Sessiz AudioTrack başlat (Samsung One UI & Android ExoPlayer'ın sesi hemen kesmesi için)
            if (silentAudioTrack == null) {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val numSamples = sampleRate // 1 saniyelik döngü
                val bufferSize = numSamples * 2 // 16-bit = 2 bytes per sample
                val buffer = ByteArray(bufferSize) // Saf sessizlik (0 baytları)

                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                val written = track.write(buffer, 0, buffer.size)
                if (written > 0) {
                    track.setLoopPoints(0, numSamples, -1) // Sonsuz döngü
                    track.play()
                    silentAudioTrack = track
                    Log.i(TAG, "MediaPauseHelper: Sessiz AudioTrack döngüye alındı, ExoPlayer duraklatıldı.")
                } else {
                    track.release()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPauseHelper: pauseMedia çalıştırılırken hata: ${e.message}")
        }
    }

    /**
     * Alınan ses odağını ve sessiz ses parçacığını serbest bırakır.
     */
    @Synchronized
    fun releaseAudioFocus(context: Context) {
        try {
            silentAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
                Log.i(TAG, "MediaPauseHelper: Sessiz AudioTrack durduruldu.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPauseHelper: AudioTrack serbest bırakılırken hata: ${e.message}")
        } finally {
            silentAudioTrack = null
        }

        if (!hasAudioFocus && audioFocusRequest == null) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { req ->
                    audioManager.abandonAudioFocusRequest(req)
                    Log.i(TAG, "MediaPauseHelper: Ses odağı bırakıldı (abandonAudioFocusRequest).")
                }
                audioFocusRequest = null
                audioFocusListener = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
                Log.i(TAG, "MediaPauseHelper (Legacy): Ses odağı bırakıldı.")
            }
            hasAudioFocus = false
        } catch (e: Exception) {
            Log.w(TAG, "MediaPauseHelper: Ses odağı bırakılırken hata: ${e.message}")
        }
    }
}
