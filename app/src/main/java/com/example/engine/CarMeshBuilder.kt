package com.example.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CustomCarMesh(
    val bodyVbo: Int,
    val bodyIbo: Int,
    val bodyIndexCount: Int,
    val glassVbo: Int,
    val glassIbo: Int,
    val glassIndexCount: Int,
    val wheelVbo: Int,
    val wheelIbo: Int,
    val wheelIndexCount: Int,
    val lightsVbo: Int,
    val lightsIbo: Int,
    val lightsIndexCount: Int,
    val chromeVbo: Int,
    val chromeIbo: Int,
    val chromeIndexCount: Int
) {
    fun release() {
        val bufs = intArrayOf(bodyVbo, bodyIbo, glassVbo, glassIbo, wheelVbo, wheelIbo, lightsVbo, lightsIbo, chromeVbo, chromeIbo)
        GLES20.glDeleteBuffers(bufs.size, bufs, 0)
    }
}

object CarMeshBuilder {

    fun buildCar(carId: String): CustomCarMesh {
        val bodyVerts = ArrayList<Float>()
        val bodyInds = ArrayList<Short>()
        val glassVerts = ArrayList<Float>()
        val glassInds = ArrayList<Short>()
        val lightVerts = ArrayList<Float>()
        val lightInds = ArrayList<Short>()
        val chromeVerts = ArrayList<Float>()
        val chromeInds = ArrayList<Short>()

        fun addQuad(
            targetVerts: ArrayList<Float>,
            targetInds: ArrayList<Short>,
            p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray,
            n: FloatArray
        ) {
            val base = (targetVerts.size / 8).toShort()
            val pts = listOf(p1, p2, p3, p4)
            val uvs = listOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(1f, 1f), floatArrayOf(0f, 1f))
            for (k in 0..3) {
                targetVerts.add(pts[k][0]); targetVerts.add(pts[k][1]); targetVerts.add(pts[k][2])
                targetVerts.add(n[0]); targetVerts.add(n[1]); targetVerts.add(n[2])
                targetVerts.add(uvs[k][0]); targetVerts.add(uvs[k][1])
            }
            targetInds.add(base); targetInds.add((base + 1).toShort()); targetInds.add((base + 2).toShort())
            targetInds.add(base); targetInds.add((base + 2).toShort()); targetInds.add((base + 3).toShort())
        }

        val isCharger = carId == "charger_1970"
        val isCamaro = carId == "camaro_1969"
        val isCorvette = carId == "corvette_1963"
        val isCuda = carId == "cuda_1971"
        val isBoss = carId == "boss_429"
        val isShelby = carId == "shelby_gt500"
        val isSkyline = carId == "skyline_r34"
        val isSupra = carId == "supra_mk4"
        val isMcLaren = carId == "mclaren_f1"
        val isVeyron = carId == "veyron_ss"

        // Dimensional styling per vehicle archetype
        val halfW = when {
            isVeyron || isMcLaren -> 0.98f
            isSkyline || isSupra -> 0.92f
            isCharger || isCuda -> 0.94f
            isCorvette -> 0.88f
            else -> 0.90f
        }

        val hoodLen = when {
            isCharger || isBoss || isCuda -> 2.30f
            isCorvette -> 2.20f
            isMcLaren -> 1.50f
            isVeyron -> 1.65f
            isSupra || isSkyline -> 2.05f
            else -> 2.15f
        }

        val rearLen = when {
            isMcLaren -> -2.05f
            isVeyron -> -2.10f
            isCorvette -> -2.30f
            else -> -2.25f
        }

        val roofH = when {
            isMcLaren -> 1.00f
            isVeyron || isCorvette -> 1.05f
            isSupra || isSkyline -> 1.12f
            else -> 1.18f
        }

        // 1. Main Hood Surface
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW, 0.48f, hoodLen), floatArrayOf(halfW, 0.48f, hoodLen),
            floatArrayOf(halfW * 0.92f, 0.68f, 0.75f), floatArrayOf(-halfW * 0.92f, 0.68f, 0.75f),
            floatArrayOf(0f, 0.95f, 0.2f)
        )

        // 2. Distinctive Hood Elements
        if (isCharger) {
            // Chrome Roots Blower protruding through hood cutout
            val bw = 0.28f; val bh = 0.96f; val bz0 = 1.05f; val bz1 = 1.75f
            addQuad(chromeVerts, chromeInds, floatArrayOf(-bw, bh, bz1), floatArrayOf(bw, bh, bz1), floatArrayOf(bw, bh, bz0), floatArrayOf(-bw, bh, bz0), floatArrayOf(0f, 1f, 0f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(-bw, 0.65f, bz1), floatArrayOf(bw, 0.65f, bz1), floatArrayOf(bw, bh, bz1), floatArrayOf(-bw, bh, bz1), floatArrayOf(0f, 0.2f, 1f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(-bw, 0.65f, bz1), floatArrayOf(-bw, bh, bz1), floatArrayOf(-bw, bh, bz0), floatArrayOf(-bw, 0.65f, bz0), floatArrayOf(-1f, 0f, 0f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(bw, 0.65f, bz0), floatArrayOf(bw, bh, bz0), floatArrayOf(bw, bh, bz1), floatArrayOf(bw, 0.65f, bz1), floatArrayOf(1f, 0f, 0f))
        } else if (isCuda) {
            // Shaker Hood Scoop in contrasting matte black / chrome
            val sw = 0.26f; val sh = 0.82f; val sz0 = 1.15f; val sz1 = 1.65f
            addQuad(chromeVerts, chromeInds, floatArrayOf(-sw, sh, sz1), floatArrayOf(sw, sh, sz1), floatArrayOf(sw, sh, sz0), floatArrayOf(-sw, sh, sz0), floatArrayOf(0f, 1f, 0f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(-sw, 0.68f, sz1), floatArrayOf(sw, 0.68f, sz1), floatArrayOf(sw, sh, sz1), floatArrayOf(-sw, sh, sz1), floatArrayOf(0f, 0.3f, 1f))
        } else if (isBoss) {
            // Massive Ram Air Hood Scoop
            val bw = 0.36f; val bh = 0.84f; val bz0 = 0.95f; val bz1 = 1.75f
            addQuad(bodyVerts, bodyInds, floatArrayOf(-bw, bh, bz1), floatArrayOf(bw, bh, bz1), floatArrayOf(bw, bh, bz0), floatArrayOf(-bw, bh, bz0), floatArrayOf(0f, 1f, 0f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(-bw, 0.68f, bz1), floatArrayOf(bw, 0.68f, bz1), floatArrayOf(bw, bh, bz1), floatArrayOf(-bw, bh, bz1), floatArrayOf(0f, 0.2f, 1f))
        } else if (isCamaro || isShelby) {
            // Cowl induction scoop
            val sw = 0.32f; val sh = 0.76f
            addQuad(bodyVerts, bodyInds, floatArrayOf(-sw, sh, 1.6f), floatArrayOf(sw, sh, 1.6f), floatArrayOf(sw, sh, 0.85f), floatArrayOf(-sw, sh, 0.85f), floatArrayOf(0f, 1f, 0f))
            addQuad(bodyVerts, bodyInds, floatArrayOf(sw, 0.68f, 0.85f), floatArrayOf(sw, sh, 0.85f), floatArrayOf(-sw, sh, 0.85f), floatArrayOf(-sw, 0.68f, 0.85f), floatArrayOf(0f, 0f, -1f))
        } else if (isMcLaren) {
            // Central Roof Snorkel Scoop (Air Intake for V12)
            val mw = 0.16f
            addQuad(chromeVerts, chromeInds, floatArrayOf(-mw, roofH + 0.14f, 0.25f), floatArrayOf(mw, roofH + 0.14f, 0.25f), floatArrayOf(mw, roofH + 0.10f, -0.55f), floatArrayOf(-mw, roofH + 0.10f, -0.55f), floatArrayOf(0f, 1f, 0f))
            addQuad(chromeVerts, chromeInds, floatArrayOf(-mw, roofH, 0.25f), floatArrayOf(mw, roofH, 0.25f), floatArrayOf(mw, roofH + 0.14f, 0.25f), floatArrayOf(-mw, roofH + 0.14f, 0.25f), floatArrayOf(0f, 0f, 1f))
        } else if (isVeyron) {
            // Twin Chrome Roof Snorkel Scoops
            val rw = 0.18f
            for (sign in listOf(-0.45f, 0.45f)) {
                addQuad(chromeVerts, chromeInds, floatArrayOf(sign - rw, roofH + 0.10f, 0.15f), floatArrayOf(sign + rw, roofH + 0.10f, 0.15f), floatArrayOf(sign + rw, roofH + 0.08f, -0.65f), floatArrayOf(sign - rw, roofH + 0.08f, -0.65f), floatArrayOf(0f, 1f, 0f))
            }
        }

        // 3. Roof Structure
        val roofStart = if (isVeyron || isMcLaren) 0.05f else 0.20f
        val roofEnd = if (isVeyron || isMcLaren) -0.75f else -0.85f
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW * 0.75f, roofH, roofStart), floatArrayOf(halfW * 0.75f, roofH, roofStart),
            floatArrayOf(halfW * 0.75f, roofH * 0.98f, roofEnd), floatArrayOf(-halfW * 0.75f, roofH * 0.98f, roofEnd),
            floatArrayOf(0f, 1f, 0f)
        )

        // 4. Trunk / Fastback Rear Deck
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW * 0.88f, 0.74f, -1.45f), floatArrayOf(halfW * 0.88f, 0.74f, -1.45f),
            floatArrayOf(halfW * 0.90f, 0.70f, rearLen), floatArrayOf(-halfW * 0.90f, 0.70f, rearLen),
            floatArrayOf(0f, 0.95f, -0.2f)
        )

        // 5. Rear Diffuser & Bumper
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW * 0.90f, 0.70f, rearLen), floatArrayOf(halfW * 0.90f, 0.70f, rearLen),
            floatArrayOf(halfW * 0.85f, 0.20f, rearLen), floatArrayOf(-halfW * 0.85f, 0.20f, rearLen),
            floatArrayOf(0f, 0f, -1f)
        )

        // 6. Front Bumper / Air Dam
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW * 0.88f, 0.20f, hoodLen + 0.05f), floatArrayOf(halfW * 0.88f, 0.20f, hoodLen + 0.05f),
            floatArrayOf(halfW, 0.48f, hoodLen), floatArrayOf(-halfW, 0.48f, hoodLen),
            floatArrayOf(0f, 0.1f, 1f)
        )

        // 7. Left & Right Body Flanks
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(-halfW, 0.48f, hoodLen), floatArrayOf(-halfW * 0.90f, 0.70f, rearLen),
            floatArrayOf(-halfW * 0.85f, 0.20f, rearLen), floatArrayOf(-halfW * 0.88f, 0.20f, hoodLen + 0.05f),
            floatArrayOf(-1f, 0f, 0f)
        )
        addQuad(
            bodyVerts, bodyInds,
            floatArrayOf(halfW * 0.90f, 0.70f, rearLen), floatArrayOf(halfW, 0.48f, hoodLen),
            floatArrayOf(halfW * 0.88f, 0.20f, hoodLen + 0.05f), floatArrayOf(halfW * 0.85f, 0.20f, rearLen),
            floatArrayOf(1f, 0f, 0f)
        )

        // Front Chin Splitter (Boss 429, Skyline, Supra, McLaren, Veyron)
        if (isBoss || isSkyline || isSupra || isMcLaren || isVeyron) {
            val splitLen = hoodLen + 0.14f
            addQuad(
                chromeVerts, chromeInds,
                floatArrayOf(-halfW * 0.90f, 0.18f, splitLen), floatArrayOf(halfW * 0.90f, 0.18f, splitLen),
                floatArrayOf(halfW * 0.88f, 0.20f, hoodLen), floatArrayOf(-halfW * 0.88f, 0.20f, hoodLen),
                floatArrayOf(0f, 1f, 0f)
            )
        }

        // 8. Rear Spoiler / Wing
        if (isSupra) {
            // Iconic Curved Hoop Rear Wing
            val wingH = 1.05f
            addQuad(chromeVerts, chromeInds, floatArrayOf(-halfW * 0.82f, wingH, rearLen + 0.35f), floatArrayOf(halfW * 0.82f, wingH, rearLen + 0.35f), floatArrayOf(halfW * 0.82f, wingH, rearLen - 0.05f), floatArrayOf(-halfW * 0.82f, wingH, rearLen - 0.05f), floatArrayOf(0f, 1f, 0f))
            // Wing Uprights
            for (sign in listOf(-halfW * 0.78f, halfW * 0.78f)) {
                addQuad(chromeVerts, chromeInds, floatArrayOf(sign - 0.03f, 0.72f, rearLen + 0.15f), floatArrayOf(sign + 0.03f, 0.72f, rearLen + 0.15f), floatArrayOf(sign + 0.03f, wingH, rearLen + 0.15f), floatArrayOf(sign - 0.03f, wingH, rearLen + 0.15f), floatArrayOf(0f, 0f, 1f))
            }
        } else if (isSkyline || isMcLaren) {
            // Tall Carbon GT Racing Wing
            val wingH = 1.08f
            addQuad(chromeVerts, chromeInds, floatArrayOf(-halfW * 0.88f, wingH, rearLen + 0.20f), floatArrayOf(halfW * 0.88f, wingH, rearLen + 0.20f), floatArrayOf(halfW * 0.88f, wingH, rearLen - 0.10f), floatArrayOf(-halfW * 0.88f, wingH, rearLen - 0.10f), floatArrayOf(0f, 1f, 0f))
            // Dual Uprights
            for (sign in listOf(-0.35f, 0.35f)) {
                addQuad(chromeVerts, chromeInds, floatArrayOf(sign - 0.03f, 0.72f, rearLen + 0.05f), floatArrayOf(sign + 0.03f, 0.72f, rearLen + 0.05f), floatArrayOf(sign + 0.03f, wingH, rearLen + 0.05f), floatArrayOf(sign - 0.03f, wingH, rearLen + 0.05f), floatArrayOf(0f, 0f, 1f))
            }
        } else if (isVeyron || isShelby || isCamaro || isBoss) {
            val wingH = if (isVeyron) 0.95f else 0.86f
            addQuad(
                chromeVerts, chromeInds,
                floatArrayOf(-halfW * 0.85f, wingH, rearLen + 0.25f), floatArrayOf(halfW * 0.85f, wingH, rearLen + 0.25f),
                floatArrayOf(halfW * 0.85f, wingH, rearLen - 0.08f), floatArrayOf(-halfW * 0.85f, wingH, rearLen - 0.08f),
                floatArrayOf(0f, 1f, 0f)
            )
        }

        // 9. Glass & Windshield
        // Front Windshield
        addQuad(
            glassVerts, glassInds,
            floatArrayOf(-halfW * 0.86f, 0.69f, 0.74f), floatArrayOf(halfW * 0.86f, 0.69f, 0.74f),
            floatArrayOf(halfW * 0.74f, roofH - 0.02f, roofStart - 0.02f), floatArrayOf(-halfW * 0.74f, roofH - 0.02f, roofStart - 0.02f),
            floatArrayOf(0f, 0.8f, 0.6f)
        )

        // Rear Window (Split Window for 1963 Corvette!)
        if (isCorvette) {
            // Left pane
            addQuad(glassVerts, glassInds, floatArrayOf(-halfW * 0.74f, roofH - 0.02f, roofEnd + 0.02f), floatArrayOf(-0.04f, roofH - 0.02f, roofEnd + 0.02f), floatArrayOf(-0.04f, 0.75f, -1.43f), floatArrayOf(-halfW * 0.86f, 0.75f, -1.43f), floatArrayOf(0f, 0.7f, -0.7f))
            // Right pane
            addQuad(glassVerts, glassInds, floatArrayOf(0.04f, roofH - 0.02f, roofEnd + 0.02f), floatArrayOf(halfW * 0.74f, roofH - 0.02f, roofEnd + 0.02f), floatArrayOf(halfW * 0.86f, 0.75f, -1.43f), floatArrayOf(0.04f, 0.75f, -1.43f), floatArrayOf(0f, 0.7f, -0.7f))
            // Iconic Split Spine Divider
            addQuad(bodyVerts, bodyInds, floatArrayOf(-0.04f, roofH, roofEnd + 0.02f), floatArrayOf(0.04f, roofH, roofEnd + 0.02f), floatArrayOf(0.04f, 0.76f, -1.43f), floatArrayOf(-0.04f, 0.76f, -1.43f), floatArrayOf(0f, 0.8f, -0.6f))
        } else {
            addQuad(
                glassVerts, glassInds,
                floatArrayOf(-halfW * 0.74f, roofH - 0.02f, roofEnd + 0.02f), floatArrayOf(halfW * 0.74f, roofH - 0.02f, roofEnd + 0.02f),
                floatArrayOf(halfW * 0.86f, 0.75f, -1.43f), floatArrayOf(-halfW * 0.86f, 0.75f, -1.43f),
                floatArrayOf(0f, 0.7f, -0.7f)
            )
        }

        // 10. Headlights & Taillights
        // Headlights
        addQuad(lightVerts, lightInds, floatArrayOf(-halfW * 0.85f, 0.42f, hoodLen + 0.04f), floatArrayOf(-halfW * 0.55f, 0.42f, hoodLen + 0.04f), floatArrayOf(-halfW * 0.55f, 0.52f, hoodLen), floatArrayOf(-halfW * 0.85f, 0.52f, hoodLen), floatArrayOf(0f, 0f, 1f))
        addQuad(lightVerts, lightInds, floatArrayOf(halfW * 0.55f, 0.42f, hoodLen + 0.04f), floatArrayOf(halfW * 0.85f, 0.42f, hoodLen + 0.04f), floatArrayOf(halfW * 0.85f, 0.52f, hoodLen), floatArrayOf(halfW * 0.55f, 0.52f, hoodLen), floatArrayOf(0f, 0f, 1f))

        // Taillights
        addQuad(lightVerts, lightInds, floatArrayOf(-halfW * 0.88f, 0.60f, rearLen - 0.02f), floatArrayOf(-halfW * 0.35f, 0.60f, rearLen - 0.02f), floatArrayOf(-halfW * 0.35f, 0.68f, rearLen - 0.02f), floatArrayOf(-halfW * 0.88f, 0.68f, rearLen - 0.02f), floatArrayOf(0f, 0f, -1f))
        addQuad(lightVerts, lightInds, floatArrayOf(halfW * 0.35f, 0.60f, rearLen - 0.02f), floatArrayOf(halfW * 0.88f, 0.60f, rearLen - 0.02f), floatArrayOf(halfW * 0.88f, 0.68f, rearLen - 0.02f), floatArrayOf(halfW * 0.35f, 0.68f, rearLen - 0.02f), floatArrayOf(0f, 0f, -1f))

        // 11. Dual Exhaust Tips
        val exY = 0.28f
        val exZ = rearLen - 0.04f
        val exR = 0.07f
        for (sign in listOf(-0.45f, 0.45f)) {
            addQuad(chromeVerts, chromeInds, floatArrayOf(sign - exR, exY - exR, exZ), floatArrayOf(sign + exR, exY - exR, exZ), floatArrayOf(sign + exR, exY + exR, exZ), floatArrayOf(sign - exR, exY + exR, exZ), floatArrayOf(0f, 0f, -1f))
        }

        // 12. Wheel (12-segment cylinder)
        val wheelVerts = ArrayList<Float>()
        val wheelInds = ArrayList<Short>()
        val segments = 12
        val radius = 0.35f
        val width = 0.28f
        for (i in 0 until segments) {
            val a1 = (i.toDouble() / segments) * 2 * Math.PI
            val a2 = ((i + 1).toDouble() / segments) * 2 * Math.PI
            val y1 = (Math.sin(a1) * radius).toFloat()
            val z1 = (Math.cos(a1) * radius).toFloat()
            val y2 = (Math.sin(a2) * radius).toFloat()
            val z2 = (Math.cos(a2) * radius).toFloat()

            val base = (wheelVerts.size / 8).toShort()
            val ny = (Math.sin((a1 + a2) / 2)).toFloat()
            val nz = (Math.cos((a1 + a2) / 2)).toFloat()

            wheelVerts.addAll(listOf(-width / 2, y1, z1, 0f, ny, nz, 0f, 0f))
            wheelVerts.addAll(listOf(width / 2, y1, z1, 0f, ny, nz, 1f, 0f))
            wheelVerts.addAll(listOf(width / 2, y2, z2, 0f, ny, nz, 1f, 1f))
            wheelVerts.addAll(listOf(-width / 2, y2, z2, 0f, ny, nz, 0f, 1f))

            wheelInds.add(base); wheelInds.add((base + 1).toShort()); wheelInds.add((base + 2).toShort())
            wheelInds.add(base); wheelInds.add((base + 2).toShort()); wheelInds.add((base + 3).toShort())

            val rimBase = (wheelVerts.size / 8).toShort()
            wheelVerts.addAll(listOf(width / 2, 0f, 0f, 1f, 0f, 0f, 0.5f, 0.5f))
            wheelVerts.addAll(listOf(width / 2, y1, z1, 1f, 0f, 0f, 0f, 0f))
            wheelVerts.addAll(listOf(width / 2, y2, z2, 1f, 0f, 0f, 1f, 1f))
            wheelInds.add(rimBase); wheelInds.add((rimBase + 1).toShort()); wheelInds.add((rimBase + 2).toShort())
        }

        // Upload to VBOs
        fun makeVbo(verts: List<Float>): Int {
            val bb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            verts.forEach { fb.put(it) }
            fb.position(0)
            val bufs = IntArray(1)
            GLES20.glGenBuffers(1, bufs, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufs[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.size * 4, fb, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            return bufs[0]
        }

        fun makeIbo(inds: List<Short>): Int {
            val bb = ByteBuffer.allocateDirect(inds.size * 2).order(ByteOrder.nativeOrder())
            val sb = bb.asShortBuffer()
            inds.forEach { sb.put(it) }
            sb.position(0)
            val bufs = IntArray(1)
            GLES20.glGenBuffers(1, bufs, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufs[0])
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, inds.size * 2, sb, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            return bufs[0]
        }

        return CustomCarMesh(
            bodyVbo = makeVbo(bodyVerts),
            bodyIbo = makeIbo(bodyInds),
            bodyIndexCount = bodyInds.size,
            glassVbo = makeVbo(glassVerts),
            glassIbo = makeIbo(glassInds),
            glassIndexCount = glassInds.size,
            wheelVbo = makeVbo(wheelVerts),
            wheelIbo = makeIbo(wheelInds),
            wheelIndexCount = wheelInds.size,
            lightsVbo = makeVbo(lightVerts),
            lightsIbo = makeIbo(lightInds),
            lightsIndexCount = lightInds.size,
            chromeVbo = makeVbo(chromeVerts),
            chromeIbo = makeIbo(chromeInds),
            chromeIndexCount = chromeInds.size
        )
    }
}
