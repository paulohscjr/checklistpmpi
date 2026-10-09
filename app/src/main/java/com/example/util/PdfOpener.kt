package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object PdfOpener {

    fun getPdfUri(context: Context, pdfFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }

    fun sharePdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Arquivo PDF não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = getPdfUri(context, pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Checklist Viatura PMPI - ${pdfFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório PMPI"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun viewPdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Arquivo PDF não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = getPdfUri(context, pdfFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Abrir Relatório PMPI"))
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum leitor de PDF encontrado no dispositivo.", Toast.LENGTH_LONG).show()
        }
    }
}
