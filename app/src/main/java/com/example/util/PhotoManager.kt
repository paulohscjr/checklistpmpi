package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoManager {

    fun createTempImageFile(context: Context, slotName: String): File {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(photosDir, "VTR_${slotName}_${timeStamp}.jpg")
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Gera uma imagem de teste institucional caso o policial esteja em ambiente
     * sem sensor físico de câmera ou deseje simular rapidamente a foto.
     */
    fun createSampleCarPhoto(context: Context, slotName: String, description: String): String {
        val file = createTempImageFile(context, slotName)
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = 0xFF1C355E.toInt() }
        canvas.drawRect(0f, 0f, 640f, 480f, bgPaint)

        // Light bar
        val redLight = Paint().apply { color = 0xFFD32F2F.toInt() }
        val blueLight = Paint().apply { color = 0xFF1976D2.toInt() }
        canvas.drawRoundRect(180f, 80f, 310f, 110f, 10f, 10f, redLight)
        canvas.drawRoundRect(330f, 80f, 460f, 110f, 10f, 10f, blueLight)

        // Car body silhouette
        val carPaint = Paint().apply { color = 0xFFECEFF1.toInt() }
        canvas.drawRoundRect(80f, 160f, 560f, 380f, 25f, 25f, carPaint)

        // Police emblem circle
        val emblemPaint = Paint().apply { color = 0xFF0B3C82.toInt() }
        canvas.drawCircle(320f, 270f, 50f, emblemPaint)

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("PMPI", 320f, 278f, starPaint)

        // Text labels
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("REGISTRO FOTOGRÁFICO", 320f, 50f, textPaint)

        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B3C82.toInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(description.uppercase(), 320f, 360f, descPaint)

        val timeStamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB0BEC5.toInt()
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CAPTURA: $timeStamp", 320f, 440f, timePaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
