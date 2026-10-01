package com.example.engine

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

data class RoadTriangle(
    val x0: Float, val y0: Float, val z0: Float,
    val x1: Float, val y1: Float, val z1: Float,
    val x2: Float, val y2: Float, val z2: Float,
    val nx: Float, val ny: Float, val nz: Float,
    val minX: Float, val maxX: Float,
    val minZ: Float, val maxZ: Float,
    val cx: Float, val cz: Float, val cy: Float
)

data class WheelContact(
    var x: Float = 0f,
    var z: Float = 0f,
    var roadY: Float = 0f,
    var isContacting: Boolean = false
)

class CarAlignment(
    var carY: Float = 0f,
    var pitchDeg: Float = 0f,
    var rollDeg: Float = 0f,
    val wheelContacts: Array<WheelContact> = Array(4) { WheelContact() },
    var isOnRoad: Boolean = true
)

class RoadSurfaceResult {
    var y: Float = 0f
    var nx: Float = 0f
    var ny: Float = 1f
    var nz: Float = 0f
    var isValid: Boolean = false
}

class RoadCollisionSystem {
    private val triangles = ArrayList<RoadTriangle>(4500)
    // 2D Spatial Hash Grid: cell size 15 meters
    private val cellSize = 15f
    private val grid = HashMap<Long, ArrayList<Int>>(1024)

    // Pre-allocated reusable result objects for ZERO allocations per frame
    private val cachedResult = RoadSurfaceResult()
    private val tempAlignment = CarAlignment()

    var isLoaded = false
        private set

    @Synchronized
    fun load(context: Context): Boolean {
        if (isLoaded) return true
        try {
            val stream: InputStream = context.assets.open("road_collision.bin")
            val bytes = stream.readBytes()
            stream.close()

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4)
            buffer.get(magic)
            if (String(magic) != "RCOL") {
                Log.e("RoadCollisionSystem", "Invalid collision magic: ${String(magic)}")
                return false
            }

            val numTriangles = buffer.int
            triangles.clear()
            grid.clear()

            for (i in 0 until numTriangles) {
                val x0 = buffer.float; val y0 = buffer.float; val z0 = buffer.float
                val x1 = buffer.float; val y1 = buffer.float; val z1 = buffer.float
                val x2 = buffer.float; val y2 = buffer.float; val z2 = buffer.float
                val nx = buffer.float; val ny = buffer.float; val nz = buffer.float

                val minX = min(x0, min(x1, x2))
                val maxX = max(x0, max(x1, x2))
                val minZ = min(z0, min(z1, z2))
                val maxZ = max(z0, max(z1, z2))
                val cx = (x0 + x1 + x2) / 3f
                val cy = (y0 + y1 + y2) / 3f
                val cz = (z0 + z1 + z2) / 3f

                val tri = RoadTriangle(x0, y0, z0, x1, y1, z1, x2, y2, z2, nx, ny, nz, minX, maxX, minZ, maxZ, cx, cz, cy)
                val triIdx = triangles.size
                triangles.add(tri)

                // Add to spatial hash grid cells
                val minGx = floor(minX / cellSize).toInt()
                val maxGx = floor(maxX / cellSize).toInt()
                val minGz = floor(minZ / cellSize).toInt()
                val maxGz = floor(maxZ / cellSize).toInt()

                for (gx in minGx..maxGx) {
                    for (gz in minGz..maxGz) {
                        val key = (gx.toLong() and 0xFFFFFFFFL) or ((gz.toLong() and 0xFFFFFFFFL) shl 32)
                        grid.getOrPut(key) { ArrayList(8) }.add(triIdx)
                    }
                }
            }

            isLoaded = true
            Log.d("RoadCollisionSystem", "Loaded $numTriangles road triangles into ${grid.size} spatial cells.")
            return true
        } catch (e: Exception) {
            Log.e("RoadCollisionSystem", "Failed to load road collision: ${e.message}")
            return false
        }
    }

    /**
     * Fast zero-allocation road surface query
     */
    fun queryRoadSurface(px: Float, pz: Float, out: RoadSurfaceResult): Boolean {
        out.isValid = false
        if (!isLoaded || triangles.isEmpty()) return false

        val gx = floor(px / cellSize).toInt()
        val gz = floor(pz / cellSize).toInt()
        val key = (gx.toLong() and 0xFFFFFFFFL) or ((gz.toLong() and 0xFFFFFFFFL) shl 32)

        val cellTris = grid[key]
        if (cellTris != null) {
            for (i in 0 until cellTris.size) {
                val tri = triangles[cellTris[i]]
                if (testPointInTriangle(px, pz, tri, out)) {
                    out.nx = tri.nx
                    out.ny = tri.ny
                    out.nz = tri.nz
                    out.isValid = true
                    return true
                }
            }
        }

        // Check 8 neighboring cells for edge proximity
        var bestDistSq = Float.MAX_VALUE
        var bestY = 0f
        var bestNx = 0f
        var bestNy = 1f
        var bestNz = 0f
        var foundNear = false

        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val nKey = ((gx + dx).toLong() and 0xFFFFFFFFL) or (((gz + dz).toLong() and 0xFFFFFFFFL) shl 32)
                val nTris = grid[nKey] ?: continue
                for (i in 0 until nTris.size) {
                    val tri = triangles[nTris[i]]
                    if (testPointInTriangle(px, pz, tri, out)) {
                        out.nx = tri.nx
                        out.ny = tri.ny
                        out.nz = tri.nz
                        out.isValid = true
                        return true
                    }
                    val distSq = (tri.cx - px) * (tri.cx - px) + (tri.cz - pz) * (tri.cz - pz)
                    if (distSq < bestDistSq && distSq < 36f) { // within 6m
                        bestDistSq = distSq
                        bestY = tri.cy
                        bestNx = tri.nx
                        bestNy = tri.ny
                        bestNz = tri.nz
                        foundNear = true
                    }
                }
            }
        }

        if (foundNear) {
            out.y = bestY
            out.nx = bestNx
            out.ny = bestNy
            out.nz = bestNz
            out.isValid = true
            return true
        }

        return false
    }

    /**
     * Backwards-compatible query returning Pair (used only for non-hot-path tests)
     */
    fun getRoadSurface(px: Float, pz: Float): Pair<Float, FloatArray>? {
        val res = RoadSurfaceResult()
        return if (queryRoadSurface(px, pz, res)) {
            Pair(res.y, floatArrayOf(res.nx, res.ny, res.nz))
        } else {
            null
        }
    }

    private fun testPointInTriangle(px: Float, pz: Float, tri: RoadTriangle, out: RoadSurfaceResult): Boolean {
        val x0 = tri.x0; val z0 = tri.z0
        val x1 = tri.x1; val z1 = tri.z1
        val x2 = tri.x2; val z2 = tri.z2

        val denom = (z1 - z2) * (x0 - x2) + (x2 - x1) * (z0 - z2)
        if (abs(denom) < 1e-7f) return false

        val w0 = ((z1 - z2) * (px - x2) + (x2 - x1) * (pz - z2)) / denom
        val w1 = ((z2 - z0) * (px - x2) + (x0 - x2) * (pz - z2)) / denom
        val w2 = 1.0f - w0 - w1

        val eps = -0.06f
        if (w0 >= eps && w1 >= eps && w2 >= eps) {
            out.y = w0 * tri.y0 + w1 * tri.y1 + w2 * tri.y2
            return true
        }
        return false
    }

    /**
     * Calculates the exact car chassis elevation, pitch, roll, and all 4 wheel contacts.
     * Uses zero allocations in hot path.
     */
    fun alignCarOnRoad(
        posX: Float,
        posZ: Float,
        headingDeg: Float,
        wheelbase: Float = 2.7f,
        trackWidth: Float = 1.84f,
        wheelRadius: Float = 0.35f,
        userHeightOffset: Float = 0.0f,
        targetAlignment: CarAlignment? = null
    ): CarAlignment {
        val align = targetAlignment ?: CarAlignment()

        val hRad = Math.toRadians(headingDeg.toDouble())
        val fwdX = sin(hRad).toFloat()
        val fwdZ = cos(hRad).toFloat()
        val rightX = cos(hRad).toFloat()
        val rightZ = -sin(hRad).toFloat()

        val halfWb = wheelbase * 0.5f
        val halfTw = trackWidth * 0.5f

        // 4 wheel positions
        // 0: Front-Left
        val flX = posX + fwdX * halfWb - rightX * halfTw
        val flZ = posZ + fwdZ * halfWb - rightZ * halfTw
        // 1: Front-Right
        val frX = posX + fwdX * halfWb + rightX * halfTw
        val frZ = posZ + fwdZ * halfWb + rightZ * halfTw
        // 2: Rear-Left
        val rlX = posX - fwdX * halfWb - rightX * halfTw
        val rlZ = posZ - fwdZ * halfWb - rightZ * halfTw
        // 3: Rear-Right
        val rrX = posX - fwdX * halfWb + rightX * halfTw
        val rrZ = posZ - fwdZ * halfWb + rightZ * halfTw

        val res = cachedResult

        var defaultY = 40.0f
        if (queryRoadSurface(posX, posZ, res)) {
            defaultY = res.y
        }

        // Front-Left
        val flOk = queryRoadSurface(flX, flZ, res)
        val flY = if (flOk) res.y else defaultY
        align.wheelContacts[0].apply { x = flX; z = flZ; roadY = flY; isContacting = flOk }

        // Front-Right
        val frOk = queryRoadSurface(frX, frZ, res)
        val frY = if (frOk) res.y else defaultY
        align.wheelContacts[1].apply { x = frX; z = frZ; roadY = frY; isContacting = frOk }

        // Rear-Left
        val rlOk = queryRoadSurface(rlX, rlZ, res)
        val rlY = if (rlOk) res.y else defaultY
        align.wheelContacts[2].apply { x = rlX; z = rlZ; roadY = rlY; isContacting = rlOk }

        // Rear-Right
        val rrOk = queryRoadSurface(rrX, rrZ, res)
        val rrY = if (rrOk) res.y else defaultY
        align.wheelContacts[3].apply { x = rrX; z = rrZ; roadY = rrY; isContacting = rrOk }

        val avgWheelGroundY = (flY + frY + rlY + rrY) * 0.25f
        align.carY = avgWheelGroundY + wheelRadius + userHeightOffset

        // Longitudinal pitch (front to rear slope)
        val frontAxleY = (flY + frY) * 0.5f
        val rearAxleY = (rlY + rrY) * 0.5f
        val pitchRad = atan2((rearAxleY - frontAxleY).toDouble(), wheelbase.toDouble())
        align.pitchDeg = Math.toDegrees(pitchRad).toFloat()

        // Lateral roll (left to right camber / banking)
        val leftY = (flY + rlY) * 0.5f
        val rightY = (frY + rrY) * 0.5f
        val rollRad = atan2((rightY - leftY).toDouble(), trackWidth.toDouble())
        align.rollDeg = Math.toDegrees(rollRad).toFloat()

        align.isOnRoad = flOk || frOk || rlOk || rrOk

        return align
    }

    fun getDebugWireframe(): FloatArray {
        val maxTris = min(400, triangles.size)
        val lines = FloatArray(maxTris * 18)
        var ptr = 0
        for (i in 0 until maxTris) {
            val t = triangles[i]
            // Edge 1
            lines[ptr++] = t.x0; lines[ptr++] = t.y0 + 0.05f; lines[ptr++] = t.z0
            lines[ptr++] = t.x1; lines[ptr++] = t.y1 + 0.05f; lines[ptr++] = t.z1
            // Edge 2
            lines[ptr++] = t.x1; lines[ptr++] = t.y1 + 0.05f; lines[ptr++] = t.z1
            lines[ptr++] = t.x2; lines[ptr++] = t.y2 + 0.05f; lines[ptr++] = t.z2
            // Edge 3
            lines[ptr++] = t.x2; lines[ptr++] = t.y2 + 0.05f; lines[ptr++] = t.z2
            lines[ptr++] = t.x0; lines[ptr++] = t.y0 + 0.05f; lines[ptr++] = t.z0
        }
        return lines
    }
}
