package com.example.engine

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class Checkpoint(
    val index: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val tx: Float,
    val tz: Float,
    val nx: Float,
    val nz: Float,
    val width: Float
)

data class GridSlot(
    val position: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val heading: Float
)

class TrackData(
    val trackName: String,
    val location: String,
    val circuitLengthMeters: Float,
    val checkpoints: List<Checkpoint>,
    val gridSlots: List<GridSlot>,
    val startLineIndex: Int = 1,
    val sector1Index: Int = 40,
    val sector2Index: Int = 80
) {
    companion object {
        fun loadFromAssets(context: Context): TrackData {
            val jsonString = context.assets.open("circuit.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonString)
            val name = obj.getString("trackName")
            val loc = obj.getString("location")
            val length = obj.getDouble("circuitLengthMeters").toFloat()
            val startIdx = obj.optInt("startLineIndex", 1)
            val s1Idx = obj.optInt("sector1Index", 40)
            val s2Idx = obj.optInt("sector2Index", 80)

            val cpArray = obj.getJSONArray("checkpoints")
            val cpList = ArrayList<Checkpoint>(cpArray.length())
            for (i in 0 until cpArray.length()) {
                val cp = cpArray.getJSONObject(i)
                cpList.add(
                    Checkpoint(
                        index = cp.getInt("index"),
                        x = cp.getDouble("x").toFloat(),
                        y = cp.getDouble("y").toFloat(),
                        z = cp.getDouble("z").toFloat(),
                        tx = cp.optDouble("tx", 1.0).toFloat(),
                        tz = cp.optDouble("tz", 0.0).toFloat(),
                        nx = cp.optDouble("nx", 0.0).toFloat(),
                        nz = cp.optDouble("nz", 1.0).toFloat(),
                        width = cp.optDouble("width", 14.0).toFloat()
                    )
                )
            }

            val gridArray = obj.getJSONArray("gridSlots")
            val gridList = ArrayList<GridSlot>(gridArray.length())
            for (i in 0 until gridArray.length()) {
                val g = gridArray.getJSONObject(i)
                gridList.add(
                    GridSlot(
                        position = g.getInt("position"),
                        x = g.getDouble("x").toFloat(),
                        y = g.getDouble("y").toFloat(),
                        z = g.getDouble("z").toFloat(),
                        heading = g.getDouble("heading").toFloat()
                    )
                )
            }

            return TrackData(name, loc, length, cpList, gridList, startIdx, s1Idx, s2Idx)
        }
    }

    fun findNearestCheckpointIndex(x: Float, z: Float, startSearchAround: Int = -1): Int {
        if (checkpoints.isEmpty()) return 0
        val n = checkpoints.size
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE

        // Quick window check if a valid hint is provided
        if (startSearchAround in 0 until n) {
            val window = 20
            for (offset in -window..window) {
                val i = (startSearchAround + offset + n) % n
                val cp = checkpoints[i]
                val dx = cp.x - x
                val dz = cp.z - z
                val distSq = dx * dx + dz * dz
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestIdx = i
                }
            }
            // If the local window found a close match (< 20m), return it immediately
            if (bestDistSq < 400f) {
                return bestIdx
            }
        }

        // Full global search across all 120 checkpoints to guarantee exact closest checkpoint
        bestDistSq = Float.MAX_VALUE
        for (i in 0 until n) {
            val cp = checkpoints[i]
            val dx = cp.x - x
            val dz = cp.z - z
            val distSq = dx * dx + dz * dz
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestIdx = i
            }
        }
        return bestIdx
    }

    fun getInterpolatedElevation(x: Float, z: Float, nearestIdx: Int): Float {
        if (checkpoints.isEmpty()) return 40f
        val cp = checkpoints[nearestIdx]
        val nextCp = checkpoints[(nearestIdx + 1) % checkpoints.size]
        val dx = nextCp.x - cp.x
        val dz = nextCp.z - cp.z
        val lenSq = dx * dx + dz * dz
        if (lenSq < 0.001f) return cp.y
        val t = (((x - cp.x) * dx + (z - cp.z) * dz) / lenSq).coerceIn(0f, 1f)
        return cp.y + (nextCp.y - cp.y) * t
    }

    fun getDistanceFromCenterline(x: Float, z: Float, nearestIdx: Int): Float {
        if (checkpoints.isEmpty()) return 0f
        val cp = checkpoints[nearestIdx]
        val dx = x - cp.x
        val dz = z - cp.z
        // Lateral cross distance perpendicular to track tangent
        return dz * cp.tx - dx * cp.tz
    }
}
