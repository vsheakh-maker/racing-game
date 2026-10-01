package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.*
import kotlin.random.Random

class ProceduralAudio {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var audioThread: Thread? = null

    // Audio state parameters
    @Volatile var targetRpm: Float = 1000f
    @Volatile var throttle: Float = 0f
    @Volatile var isDrifting: Boolean = false
    @Volatile var isNitro: Boolean = false
    @Volatile var isOverdrive: Boolean = false
    @Volatile var tireSquealAmount: Float = 0f // 0f to 1f
    @Volatile var triggerCountdownBeep: Int = -1 // 3, 2, 1, 0 (GO!)
    @Volatile var triggerCrash: Boolean = false
    @Volatile var triggerExhaustPop: Boolean = false
    @Volatile var triggerGearShift: Boolean = false
    @Volatile var isRaining: Boolean = false
    @Volatile var isMuted: Boolean = false
    @Volatile var soundFreqMultiplier: Float = 1.0f
    @Volatile var hasSuperchargerWhine: Boolean = false
    @Volatile var hasTurboWhistle: Boolean = false

    private val sampleRate = 22050
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast( sampleRate / 10 )

    fun start() {
        if (isPlaying) return
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            audioThread = Thread {
                audioLoop()
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.w("ProceduralAudio", "AudioTrack failed to start: ${e.message}")
        }
    }

    private fun audioLoop() {
        val chunkSamples = 1024
        val buffer = ShortArray(chunkSamples)

        var phase1 = 0.0
        var phase2 = 0.0
        var superchargerPhase = 0.0
        var turboPhase = 0.0
        var smoothedRpm = 1000f
        var beepRemaining = 0
        var beepFreq = 880.0
        var crashRemaining = 0
        var exhaustPopRemaining = 0
        var gearShiftRemaining = 0

        while (isPlaying) {
            if (isMuted) {
                buffer.fill(0)
                audioTrack?.write(buffer, 0, chunkSamples)
                continue
            }

            // Check triggers
            val beepVal = triggerCountdownBeep
            if (beepVal >= 0) {
                triggerCountdownBeep = -1
                beepFreq = if (beepVal == 0) 1760.0 else 880.0
                beepRemaining = (sampleRate * (if (beepVal == 0) 0.4 else 0.15)).toInt()
            }

            if (triggerCrash) {
                triggerCrash = false
                crashRemaining = (sampleRate * 0.25).toInt()
            }

            if (triggerExhaustPop) {
                triggerExhaustPop = false
                exhaustPopRemaining = (sampleRate * 0.08).toInt()
            }

            if (triggerGearShift) {
                triggerGearShift = false
                gearShiftRemaining = (sampleRate * 0.06).toInt()
            }

            smoothedRpm += (targetRpm - smoothedRpm) * 0.18f

            // Engine fundamental frequency tailored to car cylinder count & profile
            val engineFreq = (smoothedRpm / 60.0) * 3.5 * soundFreqMultiplier
            val deltaPhase1 = (2.0 * PI * engineFreq) / sampleRate
            val deltaPhase2 = (2.0 * PI * (engineFreq * 1.5)) / sampleRate
            val deltaScPhase = (2.0 * PI * (engineFreq * 6.5)) / sampleRate
            val deltaTurboPhase = (2.0 * PI * (engineFreq * 9.2)) / sampleRate

            val driftActive = isDrifting || tireSquealAmount > 0.15f
            val nitroActive = isNitro
            val overdriveActive = isOverdrive
            val currentThrottle = throttle
            val squealIntensity = max(if (driftActive) 0.85f else 0f, tireSquealAmount)

            for (i in 0 until chunkSamples) {
                phase1 += deltaPhase1
                if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI

                phase2 += deltaPhase2
                if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI

                superchargerPhase += deltaScPhase
                if (superchargerPhase > 2.0 * PI) superchargerPhase -= 2.0 * PI

                turboPhase += deltaTurboPhase
                if (turboPhase > 2.0 * PI) turboPhase -= 2.0 * PI

                // Engine sound: multi-harmonic asymmetric pulse wave (V8 firing order rumble)
                val wave1 = sin(phase1) + 0.45 * sin(phase1 * 2.0) + 0.25 * sin(phase1 * 3.0) + 0.15 * sin(phase1 * 4.0)
                val wave2 = sin(phase2) * 0.35
                val exhaustGrit = (Random.nextDouble() - 0.5) * 0.18 * currentThrottle

                var sample = (wave1 + wave2 + exhaustGrit) * (0.35 + 0.45 * currentThrottle)

                // Supercharger high-frequency whine (HEMI blower)
                if (hasSuperchargerWhine) {
                    val scWhine = sin(superchargerPhase) * 0.22 * currentThrottle * (smoothedRpm / 5000f).coerceIn(0.1f, 1.2f)
                    sample += scWhine
                }

                // Turbocharger high-pitched jet whistle (Veyron W16)
                if (hasTurboWhistle) {
                    val turboWhistle = sin(turboPhase) * 0.20 * currentThrottle * (smoothedRpm / 4500f).coerceIn(0.2f, 1.3f)
                    sample += turboWhistle
                }

                // Tire Screech: Band-limited friction noise
                if (driftActive) {
                    val noise = (Random.nextDouble() - 0.5) * 0.40 * squealIntensity
                    sample += noise
                }

                // Nitro Booster Burner: Roaring pressurized jet whoosh
                if (nitroActive) {
                    val nitroNoise = (Random.nextDouble() - 0.5) * 0.32
                    val nitroTone = sin(phase1 * 3.0) * 0.20
                    sample += nitroNoise + nitroTone
                }

                // Stage 2 Overdrive Hyperboost: Sonic rocket roar
                if (overdriveActive) {
                    val odNoise = (Random.nextDouble() - 0.5) * 0.45
                    val odScream = sin(phase1 * 4.5) * 0.28
                    sample += odNoise + odScream
                }

                // Rain white-noise drizzle
                if (isRaining) {
                    sample += (Random.nextDouble() - 0.5) * 0.08
                }

                // Countdown Beeps
                if (beepRemaining > 0) {
                    beepRemaining--
                    val beepSample = sin(beepRemaining * 2.0 * PI * beepFreq / sampleRate) * 0.5
                    sample = sample * 0.4 + beepSample
                }

                // Exhaust backfire pop
                if (exhaustPopRemaining > 0) {
                    exhaustPopRemaining--
                    val popNoise = (Random.nextDouble() - 0.5) * 0.85
                    sample += popNoise
                }

                // Gear shift mechanical thump
                if (gearShiftRemaining > 0) {
                    gearShiftRemaining--
                    val shiftThump = sin(gearShiftRemaining * 2.0 * PI * 120.0 / sampleRate) * 0.60
                    sample = sample * 0.5 + shiftThump
                }

                // Metal Barrier crash crunch
                if (crashRemaining > 0) {
                    crashRemaining--
                    sample = (Random.nextDouble() - 0.5) * 0.85
                }

                // Soft saturation clamp to prevent 16-bit distortion
                val clamped = sample.coerceIn(-1.0, 1.0)
                buffer[i] = (clamped * 28000.0).toInt().toShort()
            }

            audioTrack?.write(buffer, 0, chunkSamples)
        }
    }

    fun stop() {
        isPlaying = false
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
