package com.example.engine

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.sqrt

class RoadBarrierSystem {
    private var vbo = 0
    private var ibo = 0
    private var indexCount = 0
    var isInitialized = false
        private set

    private var barrierProgram = 0
    private var aPos = -1
    private var aNorm = -1
    private var aColor = -1
    private var uMVP = -1
    private var uSunDir = -1
    private var uSunColor = -1
    private var uAmbient = -1

    companion object {
        private const val BARRIER_VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform vec3 uSunDirection;
            uniform vec3 uSunColor;
            uniform vec3 uAmbientColor;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;

            varying vec4 vColor;

            void main() {
                vec3 norm = normalize(aNormal);
                float diff = max(dot(norm, uSunDirection), 0.0);
                vec3 lighting = uAmbientColor + uSunColor * diff * 0.75;
                vColor = vec4(aColor.rgb * lighting, aColor.a);
                gl_Position = uMVPMatrix * aPosition;
            }
        """

        private const val BARRIER_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 vColor;

            void main() {
                gl_FragColor = vColor;
            }
        """
    }

    fun init(trackData: TrackData): Boolean {
        if (isInitialized) return true
        val checkpoints = trackData.checkpoints
        if (checkpoints.size < 3) return false

        barrierProgram = ShaderHelper.createProgram(BARRIER_VERTEX_SHADER, BARRIER_FRAGMENT_SHADER)
        if (barrierProgram == 0) return false

        aPos = GLES20.glGetAttribLocation(barrierProgram, "aPosition")
        aNorm = GLES20.glGetAttribLocation(barrierProgram, "aNormal")
        aColor = GLES20.glGetAttribLocation(barrierProgram, "aColor")
        uMVP = GLES20.glGetUniformLocation(barrierProgram, "uMVPMatrix")
        uSunDir = GLES20.glGetUniformLocation(barrierProgram, "uSunDirection")
        uSunColor = GLES20.glGetUniformLocation(barrierProgram, "uSunColor")
        uAmbient = GLES20.glGetUniformLocation(barrierProgram, "uAmbientColor")

        val n = checkpoints.size
        // Each checkpoint has:
        // Left barrier: 2 vertices (top, bottom)
        // Right barrier: 2 vertices (top, bottom)
        // Left post: 4 vertices
        // Right post: 4 vertices
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, a: Float) {
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(a)
        }

        val barrierHeight = 0.85f
        val barrierGroundClearance = 0.12f

        // 1. Build Left & Right Continuous Guardrails
        for (side in 0..1) { // 0 = Left, 1 = Right
            val sign = if (side == 0) -1.0f else 1.0f
            val baseVertex = (vertices.size / 10).toShort()

            for (i in 0 until n) {
                val cp = checkpoints[i]
                val halfW = cp.width * 0.5f + 0.35f
                val bx = cp.x + cp.nx * (sign * halfW)
                val by = cp.y
                val bz = cp.z + cp.nz * (sign * halfW)

                // Outward normal facing road inwards towards the car
                val inNx = -sign * cp.nx
                val inNy = 0.1f
                val inNz = -sign * cp.nz
                val len = sqrt(inNx * inNx + inNy * inNy + inNz * inNz).coerceAtLeast(0.001f)
                val nxx = inNx / len
                val nyy = inNy / len
                val nzz = inNz / len

                // Alternating safety pattern (Red & White sections)
                val isRed = (i / 4) % 2 == 0
                val (cr, cg, cb) = if (isRed) {
                    Triple(0.88f, 0.18f, 0.14f) // Racing Safety Red
                } else {
                    Triple(0.92f, 0.94f, 0.96f) // High-visibility Safety White / Silver
                }

                // Top rail vertex
                addVertex(bx, by + barrierHeight, bz, nxx, nyy, nzz, cr, cg, cb, 1.0f)
                // Bottom rail vertex
                addVertex(bx, by + barrierGroundClearance, bz, nxx, nyy, nzz, cr * 0.8f, cg * 0.8f, cb * 0.8f, 1.0f)
            }

            // Connect quad strips around the loop
            for (i in 0 until n) {
                val nextI = (i + 1) % n
                val i0 = (baseVertex + i * 2).toShort()
                val i1 = (baseVertex + i * 2 + 1).toShort()
                val i2 = (baseVertex + nextI * 2).toShort()
                val i3 = (baseVertex + nextI * 2 + 1).toShort()

                if (side == 0) {
                    indices.add(i0); indices.add(i1); indices.add(i2)
                    indices.add(i2); indices.add(i1); indices.add(i3)
                } else {
                    indices.add(i0); indices.add(i2); indices.add(i1)
                    indices.add(i1); indices.add(i2); indices.add(i3)
                }
            }
        }

        // 2. Build Vertical Support Posts every 2 checkpoints
        for (i in 0 until n step 2) {
            val cp = checkpoints[i]
            for (side in 0..1) {
                val sign = if (side == 0) -1.0f else 1.0f
                val halfW = cp.width * 0.5f + 0.38f
                val px = cp.x + cp.nx * (sign * halfW)
                val py = cp.y
                val pz = cp.z + cp.nz * (sign * halfW)
                val pw = 0.08f

                val postBase = (vertices.size / 10).toShort()
                // 4 corners of vertical post
                addVertex(px - pw, py + barrierHeight + 0.05f, pz - pw, 0f, 1f, 0f, 0.35f, 0.38f, 0.42f, 1f)
                addVertex(px + pw, py + barrierHeight + 0.05f, pz - pw, 0f, 1f, 0f, 0.35f, 0.38f, 0.42f, 1f)
                addVertex(px + pw, py, pz + pw, 0f, 0f, 1f, 0.25f, 0.27f, 0.30f, 1f)
                addVertex(px - pw, py, pz + pw, 0f, 0f, 1f, 0.25f, 0.27f, 0.30f, 1f)

                indices.add(postBase); indices.add((postBase + 1).toShort()); indices.add((postBase + 2).toShort())
                indices.add(postBase); indices.add((postBase + 2).toShort()); indices.add((postBase + 3).toShort())
            }
        }

        indexCount = indices.size

        // Upload to OpenGL Buffers
        val vByteSize = vertices.size * 4
        val vbb = ByteBuffer.allocateDirect(vByteSize).order(ByteOrder.nativeOrder())
        val vfb = vbb.asFloatBuffer()
        for (v in vertices) vfb.put(v)
        vfb.position(0)

        val iByteSize = indices.size * 2
        val ibb = ByteBuffer.allocateDirect(iByteSize).order(ByteOrder.nativeOrder())
        val isb = ibb.asShortBuffer()
        for (idx in indices) isb.put(idx)
        isb.position(0)

        val bufs = IntArray(2)
        GLES20.glGenBuffers(2, bufs, 0)
        vbo = bufs[0]
        ibo = bufs[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vByteSize, vfb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, iByteSize, isb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        isInitialized = true
        return true
    }

    fun render(
        viewProjectionMatrix: FloatArray,
        sunDirX: Float,
        sunDirY: Float,
        sunDirZ: Float,
        sunColors: FloatArray,
        ambientColors: FloatArray
    ) {
        if (!isInitialized || indexCount == 0) return

        GLES20.glUseProgram(barrierProgram)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, viewProjectionMatrix, 0)
        GLES20.glUniform3f(uSunDir, sunDirX, sunDirY, sunDirZ)
        GLES20.glUniform3f(uSunColor, sunColors[0], sunColors[1], sunColors[2])
        GLES20.glUniform3f(uAmbient, ambientColors[0] + 0.15f, ambientColors[1] + 0.15f, ambientColors[2] + 0.15f)

        val stride = 10 * 4
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, 0)

        GLES20.glEnableVertexAttribArray(aNorm)
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)

        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, 6 * 4)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNorm)
        GLES20.glDisableVertexAttribArray(aColor)
    }

    fun release() {
        if (vbo != 0) {
            GLES20.glDeleteBuffers(2, intArrayOf(vbo, ibo), 0)
            vbo = 0
            ibo = 0
        }
        if (barrierProgram != 0) {
            GLES20.glDeleteProgram(barrierProgram)
            barrierProgram = 0
        }
        isInitialized = false
    }
}
