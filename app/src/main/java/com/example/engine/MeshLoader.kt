package com.example.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Submesh(
    val name: String,
    val texturePath: String,
    val isAlpha: Boolean,
    val vboId: Int,
    val iboId: Int,
    val numIndices: Int,
    var textureId: Int = 0
)

class MeshLoader {
    private val submeshes = ArrayList<Submesh>()
    private val textureCache = HashMap<String, Int>()
    private var defaultTextureId: Int = 0

    fun load(context: Context, assetName: String = "glencanyon.bin"): Boolean {
        try {
            defaultTextureId = createDefaultTexture()
            val inputStream: InputStream = context.assets.open(assetName)
            val bytes = inputStream.readBytes()
            inputStream.close()

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4)
            buffer.get(magic)
            if (String(magic) != "GCDR") {
                Log.e("MeshLoader", "Invalid magic header: " + String(magic))
                return false
            }

            val version = buffer.int
            val numSubmeshes = buffer.int
            Log.d("MeshLoader", "Loading $numSubmeshes submeshes from $assetName (version $version)")

            for (s in 0 until numSubmeshes) {
                val nameLen = buffer.short.toInt() and 0xFFFF
                val nameBytes = ByteArray(nameLen)
                buffer.get(nameBytes)
                val submeshName = String(nameBytes)

                val texLen = buffer.short.toInt() and 0xFFFF
                val texBytes = ByteArray(texLen)
                buffer.get(texBytes)
                val texturePath = String(texBytes)

                val r = buffer.float
                val g = buffer.float
                val b = buffer.float
                val a = buffer.float
                val isAlpha = buffer.get().toInt() != 0

                val numVertices = buffer.int
                val vertexByteSize = numVertices * 8 * 4
                val vertexBuffer = ByteBuffer.allocateDirect(vertexByteSize).order(ByteOrder.nativeOrder())
                val vSlice = buffer.slice()
                vSlice.limit(vertexByteSize)
                vertexBuffer.put(vSlice)
                vertexBuffer.position(0)
                buffer.position(buffer.position() + vertexByteSize)

                val numIndices = buffer.int
                val indexByteSize = numIndices * 2
                val indexBuffer = ByteBuffer.allocateDirect(indexByteSize).order(ByteOrder.nativeOrder())
                val iSlice = buffer.slice()
                iSlice.limit(indexByteSize)
                indexBuffer.put(iSlice)
                indexBuffer.position(0)
                buffer.position(buffer.position() + indexByteSize)

                // Generate VBO and IBO
                val buffers = IntArray(2)
                GLES20.glGenBuffers(2, buffers, 0)
                val vbo = buffers[0]
                val ibo = buffers[1]

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexByteSize, vertexBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
                GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexByteSize, indexBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

                var texId = defaultTextureId
                if (texturePath.isNotBlank()) {
                    texId = textureCache.getOrPut(texturePath) {
                        loadTexture(context, texturePath)
                    }
                }

                submeshes.add(
                    Submesh(
                        name = submeshName,
                        texturePath = texturePath,
                        isAlpha = isAlpha,
                        vboId = vbo,
                        iboId = ibo,
                        numIndices = numIndices,
                        textureId = texId
                    )
                )
            }
            Log.d("MeshLoader", "Loaded ${submeshes.size} submeshes successfully.")
            return true
        } catch (e: Exception) {
            Log.e("MeshLoader", "Failed to load mesh: ", e)
            return false
        }
    }

    private fun loadTexture(context: Context, path: String): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        if (textures[0] == 0) return defaultTextureId

        try {
            val stream = context.assets.open(path)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            if (bitmap != null) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
                bitmap.recycle()
                return textures[0]
            }
        } catch (e: Exception) {
            Log.w("MeshLoader", "Could not load texture $path, using default: ${e.message}")
        }
        return defaultTextureId
    }

    private fun createDefaultTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        val color = ByteBuffer.allocateDirect(4).put(byteArrayOf(-1, -1, -1, -1))
        color.position(0)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, color)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        return textures[0]
    }

    private var cachedAPosition = -1
    private var cachedANormal = -1
    private var cachedATexCoord = -1

    fun initShaderAttributes(program: Int) {
        cachedAPosition = GLES20.glGetAttribLocation(program, "aPosition")
        cachedANormal = GLES20.glGetAttribLocation(program, "aNormal")
        cachedATexCoord = GLES20.glGetAttribLocation(program, "aTexCoordinate")
    }

    fun render(
        program: Int,
        uMVPMatrixHandle: Int,
        uModelMatrixHandle: Int,
        uTextureHandle: Int,
        uAlphaCutoffHandle: Int,
        uTintHandle: Int,
        mvpMatrix: FloatArray,
        modelMatrix: FloatArray
    ) {
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uTintHandle, 1f, 1f, 1f, 1f)

        if (cachedAPosition < 0) {
            initShaderAttributes(program)
        }

        val aPosition = cachedAPosition
        val aNormal = cachedANormal
        val aTexCoord = cachedATexCoord

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glEnableVertexAttribArray(aTexCoord)

        val stride = 8 * 4 // 8 floats per vertex

        // Render opaque first
        GLES20.glUniform1f(uAlphaCutoffHandle, 0.5f)
        for (sm in submeshes) {
            if (sm.isAlpha) continue
            drawSubmesh(sm, aPosition, aNormal, aTexCoord, stride, uTextureHandle)
        }

        // Render alpha blended submeshes (trees/barriers)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUniform1f(uAlphaCutoffHandle, 0.15f)
        for (sm in submeshes) {
            if (!sm.isAlpha) continue
            drawSubmesh(sm, aPosition, aNormal, aTexCoord, stride, uTextureHandle)
        }
        GLES20.glDisable(GLES20.GL_BLEND)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
        GLES20.glDisableVertexAttribArray(aTexCoord)
    }

    private fun drawSubmesh(
        sm: Submesh,
        aPosition: Int,
        aNormal: Int,
        aTexCoord: Int,
        stride: Int,
        uTextureHandle: Int
    ) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sm.vboId)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, stride, 6 * 4)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sm.textureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sm.iboId)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sm.numIndices, GLES20.GL_UNSIGNED_SHORT, 0)
    }

    fun release() {
        for (sm in submeshes) {
            val bufs = intArrayOf(sm.vboId, sm.iboId)
            GLES20.glDeleteBuffers(2, bufs, 0)
        }
        for (texId in textureCache.values) {
            GLES20.glDeleteTextures(1, intArrayOf(texId), 0)
        }
        if (defaultTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(defaultTextureId), 0)
        }
        submeshes.clear()
        textureCache.clear()
    }
}
