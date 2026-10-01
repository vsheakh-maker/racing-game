package com.example.engine

import android.opengl.GLES20
import android.util.Log

object ShaderHelper {
    private const val TAG = "ShaderHelper"

    const val TRACK_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec2 aTexCoordinate;

        varying vec2 vTexCoordinate;
        varying vec3 vNormal;
        varying vec3 vWorldPos;
        varying float vDistance;

        void main() {
            vec4 worldPos = uModelMatrix * aPosition;
            vWorldPos = worldPos.xyz;
            vNormal = aNormal;
            vTexCoordinate = aTexCoordinate;
            vec4 clipPos = uMVPMatrix * aPosition;
            vDistance = clipPos.z;
            gl_Position = clipPos;
        }
    """

    const val TRACK_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D uTexture;
        uniform vec3 uSunDirection;
        uniform vec3 uSunColor;
        uniform vec3 uAmbientColor;
        uniform vec3 uFogColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform float uAlphaCutoff;
        uniform vec4 uTint;

        varying vec2 vTexCoordinate;
        varying vec3 vNormal;
        varying vec3 vWorldPos;
        varying float vDistance;

        void main() {
            vec4 texColor = texture2D(uTexture, vTexCoordinate);
            if (texColor.a < uAlphaCutoff) {
                discard;
            }
            vec3 norm = normalize(vNormal);
            float diff = max(dot(norm, uSunDirection), 0.0);
            vec3 lighting = uAmbientColor + uSunColor * diff;
            vec3 finalRgb = texColor.rgb * lighting * uTint.rgb;

            // Canyon distance atmospheric haze/fog
            float fogFactor = clamp((vDistance - uFogStart) / (uFogEnd - uFogStart), 0.0, 0.85);
            finalRgb = mix(finalRgb, uFogColor, fogFactor);

            gl_FragColor = vec4(finalRgb, texColor.a * uTint.a);
        }
    """

    const val CAR_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec3 uCameraPos;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec2 aTexCoord;

        varying vec3 vNormal;
        varying vec3 vViewDir;
        varying vec2 vTexCoord;
        varying vec3 vWorldPos;

        void main() {
            vec4 worldPos = uModelMatrix * aPosition;
            vWorldPos = worldPos.xyz;
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
            vViewDir = normalize(uCameraPos - worldPos.xyz);
            vTexCoord = aTexCoord;
            gl_Position = uMVPMatrix * aPosition;
        }
    """

    const val CAR_FRAGMENT_SHADER = """
        precision mediump float;
        uniform vec4 uCarColor;
        uniform vec3 uSunDirection;
        uniform vec3 uSunColor;
        uniform vec3 uAmbientColor;
        uniform int uIsGlass;
        uniform int uIsWheel;
        uniform int uIsLight;

        varying vec3 vNormal;
        varying vec3 vViewDir;
        varying vec2 vTexCoord;
        varying vec3 vWorldPos;

        void main() {
            vec3 norm = normalize(vNormal);
            vec3 view = normalize(vViewDir);
            float diff = max(dot(norm, uSunDirection), 0.0);
            vec3 halfVec = normalize(uSunDirection + view);
            float spec = pow(max(dot(norm, halfVec), 0.0), 32.0);
            float fresnel = pow(1.0 - max(dot(norm, view), 0.0), 3.0);

            if (uIsLight == 1) {
                // Headlight or taillight glow
                gl_FragColor = uCarColor;
                return;
            }

            if (uIsGlass == 1) {
                // Tinted reflective windshield
                vec3 glassColor = vec3(0.08, 0.12, 0.18) + uSunColor * spec * 0.8 + vec3(0.5, 0.7, 0.9) * fresnel * 0.5;
                gl_FragColor = vec4(glassColor, 0.85);
                return;
            }

            if (uIsWheel == 1) {
                // Matte rubber and metallic rim
                vec3 wheelColor = vec3(0.12, 0.12, 0.12) * (uAmbientColor + diff * uSunColor) + vec3(0.8) * spec * 0.4;
                gl_FragColor = vec4(wheelColor, 1.0);
                return;
            }

            // High gloss car paint with clearcoat specular and fresnel sheen
            vec3 baseColor = uCarColor.rgb;
            vec3 litColor = baseColor * (uAmbientColor + diff * uSunColor * 0.75);
            vec3 finalColor = litColor + uSunColor * spec * 0.6 + baseColor * fresnel * 0.35;
            gl_FragColor = vec4(finalColor, 1.0);
        }
    """

    const val SHADOW_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uMVPMatrix * aPosition;
        }
    """

    const val SHADOW_FRAGMENT_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        void main() {
            float dist = length(vTexCoord - vec2(0.5, 0.5)) * 2.0;
            float alpha = smoothstep(1.0, 0.0, dist) * 0.55;
            gl_FragColor = vec4(0.0, 0.0, 0.0, alpha);
        }
    """

    const val DEBUG_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
        }
    """

    const val DEBUG_FRAGMENT_SHADER = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """

    fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Shader compilation error: $error")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }
}
