package com.example.udparents.utilidades

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CompletableDeferred
import java.io.OutputStream

/**
 * Clase de utilidades para generar y guardar PDFs a partir de Composable.
 */
object PdfUtils {

    /**
     * Genera un archivo PDF a partir de un Composable.
     * La vista se renderiza temporalmente en la actividad para evitar el error de windowRecomposer.
     * Luego se guarda y se abre el PDF con una aplicación externa.
     *
     * @param context El contexto de la aplicación.
     * @param activity La actividad actual, necesaria para la jerarquía de vistas y permisos.
     * @param fileName El nombre del archivo PDF a crear (sin la extensión .pdf).
     * @param contenido El Composable que se va a renderizar en el PDF.
     */
    suspend fun generarPdfDesdeComposable(
        context: Context,
        activity: Activity,
        fileName: String,
        contenido: @Composable () -> Unit
    ) {
        // En Android 13 (TIRAMISU) y superiores, este permiso ya no es necesario para el almacenamiento privado de la app.
        // Sin embargo, para versiones anteriores, es requerido para el almacenamiento público.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
            return
        }

        val deferredBitmap = CompletableDeferred<Bitmap>()

        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                contenido()
            }
        }

        val rootView = activity.findViewById<View>(android.R.id.content) as ViewGroup
        rootView.addView(composeView)

        composeView.post {
            try {
                composeView.measure(
                    View.MeasureSpec.makeMeasureSpec(rootView.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(rootView.height, View.MeasureSpec.EXACTLY)
                )
                composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

                val bitmap = capturarBitmapDesdeVista(composeView)
                deferredBitmap.complete(bitmap)

            } catch (e: Exception) {
                deferredBitmap.completeExceptionally(e)
            } finally {
                rootView.removeView(composeView)
            }
        }

        try {
            val bitmap = deferredBitmap.await()
            guardarBitmapComoPDF(context, bitmap, fileName)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Captura una vista Composable en un Bitmap.
     * @param view La vista Composable que se va a capturar.
     * @return El Bitmap resultante de la captura.
     */
    private fun capturarBitmapDesdeVista(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * Guarda un Bitmap como un archivo PDF en la carpeta de Descargas pública
     * y luego lo abre con un Intent.
     *
     * @param context El contexto de la aplicación.
     * @param bitmap El Bitmap a guardar en el PDF.
     * @param fileName El nombre del archivo PDF.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarBitmapComoPDF(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val finalFileName = "$fileName.pdf"

        // 💡 Uso de MediaStore para guardar en la carpeta de Descargas pública.
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        var outputStream: OutputStream? = null
        var pdfUri: Uri? = null

        try {
            pdfUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (pdfUri == null) {
                throw Exception("No se pudo crear la URI para el PDF.")
            }

            outputStream = contentResolver.openOutputStream(pdfUri)
            if (outputStream == null) {
                throw Exception("No se pudo obtener el OutputStream.")
            }

            pdfDocument.writeTo(outputStream)
            pdfDocument.close()

            Toast.makeText(context, "PDF guardado en la carpeta Descargas", Toast.LENGTH_LONG).show()

            // Abrir el PDF con un Intent para que el usuario pueda verlo inmediatamente
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(context, "Error al guardar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            outputStream?.close()
        }
    }
}
