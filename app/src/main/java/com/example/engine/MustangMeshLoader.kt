package com.example.engine

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MustangSubmesh(
    val name: String,
    val vbo: Int,
    val ibo: Int,
    val indexCount: Int
)

class MustangMeshLoader {
    private val submeshes = HashMap<String, MustangSubmesh>()
    var isLoaded = false
        private set

    fun load(context: Context): Boolean {
        if (isLoaded) return true
        try {
            val stream: InputStream = context.assets.open("mustang1965.bin")
            val bytes = stream.readBytes()
            stream.close()

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(5)
            buffer.get(magic)
            val version = buffer.get().toInt()
            if (String(magic) != "M1965" || version != 1) {
                Log.e("MustangMeshLoader", "Invalid magic or version: ${String(magic)} v$version")
                return false
            }

            val numSubmeshes = buffer.int
            for (s in 0 until numSubmeshes) {
                val nameLen = buffer.short.toInt() and 0xFFFF
                val nameBytes = ByteArray(nameLen)
                buffer.get(nameBytes)
                val subName = String(nameBytes)

                val numVertices = buffer.int
                val vByteSize = numVertices * 8 * 4
                val vBuffer = ByteBuffer.allocateDirect(vByteSize).order(ByteOrder.nativeOrder())
                val vSlice = buffer.slice()
                vSlice.limit(vByteSize)
                vBuffer.put(vSlice)
                vBuffer.position(0)
                buffer.position(buffer.position() + vByteSize)

                val numIndices = buffer.int
                val iByteSize = numIndices * 2
                val iBuffer = ByteBuffer.allocateDirect(iByteSize).order(ByteOrder.nativeOrder())
                val iSlice = buffer.slice()
                iSlice.limit(iByteSize)
                iBuffer.put(iSlice)
                iBuffer.position(0)
                buffer.position(buffer.position() + iByteSize)

                val bufs = IntArray(2)
                GLES20.glGenBuffers(2, bufs, 0)
                val vbo = bufs[0]
                val ibo = bufs[1]

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vByteSize, vBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
                GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, iByteSize, iBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

                submeshes[subName] = MustangSubmesh(subName, vbo, ibo, numIndices)
            }

            isLoaded = true
            Log.d("MustangMeshLoader", "Loaded 1965 Mustang with ${submeshes.size} submeshes successfully.")
            return true
        } catch (e: Exception) {
            Log.e("MustangMeshLoader", "Failed to load Mustang 1965 model: ${e.message}")
            return false
        }
    }

    fun render(
        carColorHex: Long,
        aPosition: Int,
        aNormal: Int,
        aTexCoord: Int,
        uCarColor: Int,
        uIsGlass: Int,
        uIsWheel: Int,
        uIsLight: Int
    ) {
        if (!isLoaded) return
        val stride = 8 * 4

        // 1. Render Body Panels (Custom Paint)
        submeshes["body"]?.let { body ->
            val cr = ((carColorHex shr 16) and 0xFF) / 255f
            val cg = ((carColorHex shr 8) and 0xFF) / 255f
            val cb = (carColorHex and 0xFF) / 255f
            GLES20.glUniform4f(uCarColor, cr, cg, cb, 1f)
            GLES20.glUniform1i(uIsGlass, 0)
            GLES20.glUniform1i(uIsWheel, 0)
            GLES20.glUniform1i(uIsLight, 0)
            drawSubmesh(body, aPosition, aNormal, aTexCoord, stride)
        }

        // 2. Render Chrome Trim & Bumpers
        submeshes["chrome"]?.let { chrome ->
            GLES20.glUniform4f(uCarColor, 0.94f, 0.94f, 0.96f, 1f)
            GLES20.glUniform1i(uIsGlass, 0)
            GLES20.glUniform1i(uIsWheel, 1) // High metallic specular
            GLES20.glUniform1i(uIsLight, 0)
            drawSubmesh(chrome, aPosition, aNormal, aTexCoord, stride)
        }

        // 3. Render Interior, Seats & Undercarriage
        submeshes["interior"]?.let { interior ->
            GLES20.glUniform4f(uCarColor, 0.12f, 0.12f, 0.13f, 1f)
            GLES20.glUniform1i(uIsGlass, 0)
            GLES20.glUniform1i(uIsWheel, 0)
            GLES20.glUniform1i(uIsLight, 0)
            drawSubmesh(interior, aPosition, aNormal, aTexCoord, stride)
        }

        // 4. Render Headlights & Taillights
        submeshes["lights"]?.let { lights ->
            GLES20.glUniform4f(uCarColor, 1.0f, 0.95f, 0.85f, 1f)
            GLES20.glUniform1i(uIsGlass, 0)
            GLES20.glUniform1i(uIsWheel, 0)
            GLES20.glUniform1i(uIsLight, 1) // Glowing light
            drawSubmesh(lights, aPosition, aNormal, aTexCoord, stride)
        }

        // 5. Render Tires
        submeshes["tires"]?.let { tires ->
            GLES20.glUniform4f(uCarColor, 0.14f, 0.14f, 0.14f, 1f)
            GLES20.glUniform1i(uIsGlass, 0)
            GLES20.glUniform1i(uIsWheel, 1)
            GLES20.glUniform1i(uIsLight, 0)
            drawSubmesh(tires, aPosition, aNormal, aTexCoord, stride)
        }

        // 6. Render Glass (Windshield, Quarter Windows & Rear Fastback Glass)
        submeshes["glass"]?.let { glass ->
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glUniform4f(uCarColor, 0.15f, 0.22f, 0.30f, 0.82f)
            GLES20.glUniform1i(uIsGlass, 1)
            GLES20.glUniform1i(uIsWheel, 0)
            GLES20.glUniform1i(uIsLight, 0)
            drawSubmesh(glass, aPosition, aNormal, aTexCoord, stride)
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun drawSubmesh(
        sub: MustangSubmesh,
        aPosition: Int,
        aNormal: Int,
        aTexCoord: Int,
        stride: Int
    ) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sub.vbo)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, stride, 6 * 4)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sub.ibo)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sub.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
    }

    fun release() {
        for (sub in submeshes.values) {
            GLES20.glDeleteBuffers(2, intArrayOf(sub.vbo, sub.ibo), 0)
        }
        submeshes.clear()
        isLoaded = false
    }
}
