package com.example.conjuntoresidencial.ui.comun

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Difuminado progresivo de los bordes de arriba y de abajo (Android 13+): el contenido se
 * ve nítido en el centro y se desenfoca poco a poco al acercarse al borde, sin ningún color
 * encima. Se aplica como RenderEffect a la vista que contiene todas las pantallas.
 *
 * Se superponen [NIVELES] copias del contenido con el desenfoque nativo (gaussiano) de radio
 * creciente; cada una se hace visible solo en su franja de distancia al borde mediante un
 * shader AGSL que le pone transparencia. Así el desenfoque crece de forma continua y suave.
 */
@RequiresApi(33)
class DifuminadoProgresivo(private val vista: View, private val radioMaximo: Float) {

    // Un shader de máscara por nivel (cada RenderEffect guarda su propia copia de uniformes)
    private val mascaras = List(NIVELES) { RuntimeShader(MASCARA) }

    /** Alto (px) de la zona difuminada de arriba y de abajo; 0 = sin difuminado en ese borde. */
    private var arriba = 0f
    private var abajo = 0f

    init {
        vista.addOnLayoutChangeListener { _, _, top, _, bottom, _, viejoTop, _, viejoBottom ->
            if (bottom - top != viejoBottom - viejoTop) aplicar()
        }
    }

    fun ponerZonas(arriba: Float, abajo: Float) {
        if (arriba == this.arriba && abajo == this.abajo) return
        this.arriba = arriba
        this.abajo = abajo
        aplicar()
    }

    private fun aplicar() {
        if (vista.width == 0 || vista.height == 0) return
        if (arriba <= 0f && abajo <= 0f) {
            vista.setRenderEffect(null)
            return
        }
        // Base: el contenido sin cambios. Encima, cada nivel más desenfocado y más cerca del borde.
        var efecto = RenderEffect.createOffsetEffect(0f, 0f)
        mascaras.forEachIndexed { i, mascara ->
            mascara.setFloatUniform("tamano", vista.width.toFloat(), vista.height.toFloat())
            mascara.setFloatUniform("arriba", arriba)
            mascara.setFloatUniform("abajo", abajo)
            mascara.setFloatUniform("nivel", i.toFloat())
            mascara.setFloatUniform("niveles", NIVELES.toFloat())
            val radio = radioMaximo * (i + 1) / NIVELES
            val desenfocado = RenderEffect.createBlurEffect(radio, radio, Shader.TileMode.CLAMP)
            val nivel = RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(mascara, "contenido"), desenfocado
            )
            efecto = RenderEffect.createBlendModeEffect(efecto, nivel, BlendMode.SRC_OVER)
        }
        vista.setRenderEffect(efecto)
    }

    private companion object {
        const val NIVELES = 4

        /** Deja ver el nivel solo donde le toca: transparente en el centro, opaco hacia el borde. */
        const val MASCARA = """
            uniform shader contenido;
            uniform float2 tamano;
            uniform float arriba;
            uniform float abajo;
            uniform float nivel;
            uniform float niveles;

            half4 main(float2 p) {
                // Cercanía al borde: 0 fuera de las zonas, 1 justo en el borde
                float t = 0.0;
                if (arriba > 0.0 && p.y < arriba) t = 1.0 - p.y / arriba;
                if (abajo > 0.0 && p.y > tamano.y - abajo) t = max(t, (p.y - (tamano.y - abajo)) / abajo);
                // Cada nivel aparece en su tramo; suavizado para que no se noten los saltos
                float alfa = smoothstep(0.0, 1.0, clamp(t * niveles - nivel, 0.0, 1.0));
                if (alfa <= 0.0) return half4(0.0);
                return contenido.eval(p) * alfa;
            }
        """
    }
}
