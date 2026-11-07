package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import coil.load
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * P10 bloque 7: la portería toma con la cámara la foto de la cara del visitante o del paquete.
 * La cámara del celular la guarda en la caché (FileProvider, sin permiso de cámara para la app);
 * aquí se endereza, se reduce a 1280 px y se sube como JPEG. Registrar en onCreate o como
 * propiedad del fragment (registerForActivityResult).
 */
class TomadorFoto(private val fragment: Fragment, private val alTomar: (ByteArray) -> Unit) {

    private var archivo: File? = null

    private val camara = fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = archivo ?: return@registerForActivityResult
        if (!ok || !f.exists() || f.length() == 0L) return@registerForActivityResult
        val datos = try {
            FotosPorteria.comprimir(f)
        } catch (_: Exception) {
            null
        } finally {
            f.delete()
        }
        if (datos != null) alTomar(datos) else fragment.aviso(R.string.foto_error).show()
    }

    fun tomar() {
        val ctx = fragment.requireContext()
        val carpeta = File(ctx.cacheDir, "fotos").apply { mkdirs() }
        val f = File(carpeta, "foto-${System.currentTimeMillis()}.jpg")
        archivo = f
        val uri: Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".archivos", f)
        try {
            camara.launch(uri)
        } catch (_: android.content.ActivityNotFoundException) {
            fragment.aviso(R.string.foto_sin_camara).show()
        }
    }
}

object FotosPorteria {

    private const val LADO_MAXIMO = 1280

    /** La foto enderezada (EXIF), de máximo 1280 px por lado, en JPEG. */
    fun comprimir(f: File): ByteArray {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, limites)
        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val original = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = muestra })
            ?: error("Foto ilegible")
        val grados = when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(original.width, original.height))
        val m = Matrix().apply { postScale(escala, escala); postRotate(grados) }
        val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
        return ByteArrayOutputStream().use { s ->
            final.compress(Bitmap.CompressFormat.JPEG, 82, s)
            s.toByteArray()
        }
    }

    /** Sube la foto y devuelve su fotoId para registrar la visita o el paquete (null y el error si falla). */
    suspend fun subir(datos: ByteArray, porDefecto: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val parte = MultipartBody.Part.createFormData("archivo", "foto.jpg", datos.toRequestBody("image/jpeg".toMediaTypeOrNull()))
            val r = RetrofitClient.comun.subirFotoPorteria(parte)
            val id = r.body()?.fotoId
            if (r.isSuccessful && id != null) id to null else null to RetrofitClient.mensajeDeError(r, porDefecto)
        } catch (e: Exception) {
            null to RetrofitClient.mensajeDeExcepcion(e)
        }
    }

    /** La foto grande en un diálogo (tocar la miniatura). */
    fun verGrande(ctx: Context, url: String?, titulo: CharSequence?) {
        if (url.isNullOrBlank()) return
        val imagen = ImageView(ctx).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = titulo
            load(url) { crossfade(true); placeholder(R.drawable.fondo_imagen_cargando) }
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(titulo)
            .setView(imagen)
            .setPositiveButton(R.string.btn_cerrar, null)
            .show()
    }

    fun verGrande(ctx: Context, datos: ByteArray, titulo: CharSequence?) {
        val imagen = ImageView(ctx).apply {
            adjustViewBounds = true
            setImageBitmap(BitmapFactory.decodeByteArray(datos, 0, datos.size))
        }
        MaterialAlertDialogBuilder(ctx).setTitle(titulo).setView(imagen).setPositiveButton(R.string.btn_cerrar, null).show()
    }
}
