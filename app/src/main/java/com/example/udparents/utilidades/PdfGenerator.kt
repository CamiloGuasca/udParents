package com.example.udparents.utilidades

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class to generate and save PDF from a Composable content.
 */
class PdfGenerator {

    /**
     * Generates a PDF file from a composable and saves it to a specified directory.
     * @param context The application context.
     * @param fileName The name of the file to be saved (without extension).
     * @param content The Composable content to be drawn on the PDF.
     * @param activity The calling Activity to request permissions.
     */
    fun generatePdfFromComposable(
        context: Context,
        fileName: String,
        content: @Composable () -> Unit,
        activity: Activity
    ) {
        // Request permission if needed (only for Android < 13)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                (activity as? ComponentActivity)?.requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
                return
            }
        }

        try {
            // Crear ComposeView dentro de un Dialog para que esté atachado a ventana
            val composeView = ComposeView(context).apply {
                setContent {
                    content()
                }
            }

            // Crear y mostrar el Dialog que contendrá el ComposeView
            val dialog = Dialog(context).apply {
                setContentView(composeView)
                window?.setLayout(1, 1) // Tamaño mínimo
                show()
            }

            // Definir tamaño estándar para una hoja A4 en puntos (72 DPI)
            val width = 595
            val height = 842

            composeView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
            )
            composeView.layout(0, 0, width, height)

            // Crear bitmap a partir de la vista
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)

            // Crear documento PDF
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            // Crear nombre y ruta del archivo
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFileName = "$fileName-$timestamp.pdf"
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, outputFileName)

            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            // Cerrar el Dialog luego de generar el PDF
            dialog.dismiss()

            Toast.makeText(context, "PDF guardado en la carpeta de descargas.", Toast.LENGTH_LONG)
                .show()
            Log.d("PdfGenerator", "PDF guardado en: ${file.absolutePath}")

            // Intent para abrir el archivo PDF
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error al generar el PDF", e)
            Toast.makeText(context, "Error al guardar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
