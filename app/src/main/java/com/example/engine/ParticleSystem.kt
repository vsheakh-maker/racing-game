package com.example.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 1f,
    var r: Float = 1f,
    var g: Float = 1f,
    var b: Float = 1f,
    var a: Float = 1f
)

class ParticleSystem {
    private val maxParticles = 600
    private val particles = Array(maxParticles) { Particle() }
    private var activeCount = 0

    // Particle vertex buffer: each particle is a point (x, y, z, size, r, g, b, a) = 8 floats
    private val stride = 8 * 4
    private val vertexData = FloatArray(maxParticles * 8)
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxParticles * 8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var program = 0
    private var uMVP = -1
    private var aPosition = -1
    private var aSize = -1
    private var aColor = -1

    var isInitialized = false
        private set

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec3 aPosition;
            attribute float aSize;
            attribute vec4 aColor;

            varying vec4 vColor;

            void main() {
                vColor = aColor;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                gl_PointSize = clamp(aSize * (450.0 / gl_Position.w), 2.0, 64.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 vColor;

            void main() {
                // Soft circular gaussian falloff for smooth smoke and glowing flame sparks
                vec2 coord = gl_PointCoord - vec2(0.5);
                float distSq = dot(coord, coord);
                if (distSq > 0.25) {
                    discard;
                }
                float alpha = smoothstep(0.25, 0.0, distSq) * vColor.a;
                gl_FragColor = vec4(vColor.rgb, alpha);
            }
        """
    }

    fun init(): Boolean {
        if (isInitialized) return true
        program = ShaderHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return false

        uMVP = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aSize = GLES20.glGetAttribLocation(program, "aSize")
        aColor = GLES20.glGetAttribLocation(program, "aColor")

        isInitialized = true
        return true
    }

    fun spawnExhaustFlame(x: Float, y: Float, z: Float, dirX: Float, dirZ: Float, isOverdrive: Boolean, isBackfire: Boolean) {
        if (activeCount >= maxParticles - 8) return
        val p = particles[activeCount++]
        val spread = 0.08f
        p.x = x + (Random.nextFloat() - 0.5f) * spread
        p.y = y + (Random.nextFloat() - 0.5f) * spread
        p.z = z + (Random.nextFloat() - 0.5f) * spread

        val speed = if (isOverdrive) 18f else 12f
        p.vx = -dirX * speed + (Random.nextFloat() - 0.5f) * 1.5f
        p.vy = 0.2f + Random.nextFloat() * 0.8f
        p.vz = -dirZ * speed + (Random.nextFloat() - 0.5f) * 1.5f

        p.maxLife = if (isOverdrive) 0.22f else 0.16f
        p.life = p.maxLife
        p.size = if (isOverdrive) 0.65f else 0.45f

        when {
            isOverdrive -> {
                // Supersonic Plasma Cyan/Violet
                p.r = 0.20f
                p.g = 0.85f
                p.b = 1.0f
                p.a = 0.95f
            }
            isBackfire -> {
                // Fireball Orange/Red
                p.r = 1.0f
                p.g = 0.45f
                p.b = 0.08f
                p.a = 0.95f
            }
            else -> {
                // Nitrous Blue Flame
                p.r = 0.10f
                p.g = 0.70f
                p.b = 1.0f
                p.a = 0.90f
            }
        }
    }

    fun spawnTireSmoke(x: Float, y: Float, z: Float, carSpeedKmh: Float) {
        if (activeCount >= maxParticles - 4) return
        val p = particles[activeCount++]
        p.x = x + (Random.nextFloat() - 0.5f) * 0.25f
        p.y = y + 0.08f
        p.z = z + (Random.nextFloat() - 0.5f) * 0.25f

        p.vx = (Random.nextFloat() - 0.5f) * 1.8f
        p.vy = 0.8f + Random.nextFloat() * 1.2f
        p.vz = (Random.nextFloat() - 0.5f) * 1.8f

        p.maxLife = 0.55f + Random.nextFloat() * 0.35f
        p.life = p.maxLife
        p.size = 0.6f + (carSpeedKmh / 100f) * 0.4f

        p.r = 0.92f
        p.g = 0.93f
        p.b = 0.95f
        p.a = 0.45f
    }

    fun spawnWaterSpray(x: Float, y: Float, z: Float, forwardX: Float, forwardZ: Float) {
        if (activeCount >= maxParticles - 4) return
        val p = particles[activeCount++]
        p.x = x + (Random.nextFloat() - 0.5f) * 0.3f
        p.y = y + 0.12f
        p.z = z + (Random.nextFloat() - 0.5f) * 0.3f

        p.vx = -forwardX * 5f + (Random.nextFloat() - 0.5f) * 2f
        p.vy = 1.2f + Random.nextFloat() * 1.8f
        p.vz = -forwardZ * 5f + (Random.nextFloat() - 0.5f) * 2f

        p.maxLife = 0.40f
        p.life = p.maxLife
        p.size = 0.55f

        p.r = 0.80f
        p.g = 0.88f
        p.b = 0.95f
        p.a = 0.35f
    }

    fun update(dt: Float) {
        var writeIdx = 0
        var ptr = 0

        for (i in 0 until activeCount) {
            val p = particles[i]
            p.life -= dt
            if (p.life > 0f) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt

                val lifeRatio = p.life / p.maxLife
                val currentAlpha = p.a * lifeRatio
                val currentSize = p.size * (1.0f + (1.0f - lifeRatio) * 1.5f)

                vertexData[ptr++] = p.x
                vertexData[ptr++] = p.y
                vertexData[ptr++] = p.z
                vertexData[ptr++] = currentSize
                vertexData[ptr++] = p.r
                vertexData[ptr++] = p.g
                vertexData[ptr++] = p.b
                vertexData[ptr++] = currentAlpha

                if (writeIdx != i) {
                    particles[writeIdx] = p
                }
                writeIdx++
            }
        }
        activeCount = writeIdx

        if (activeCount > 0) {
            vertexBuffer.position(0)
            vertexBuffer.put(vertexData, 0, activeCount * 8)
            vertexBuffer.position(0)
        }
    }

    fun render(viewProjectionMatrix: FloatArray) {
        if (!isInitialized || activeCount == 0) return

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, viewProjectionMatrix, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE) // Additive glowing blend
        GLES20.glDepthMask(false)

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aSize)
        GLES20.glVertexAttribPointer(aSize, 1, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(4)
        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(0)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, activeCount)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aSize)
        GLES20.glDisableVertexAttribArray(aColor)

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        isInitialized = false
        activeCount = 0
    }
}
