package com.example.service.cloud

import android.content.Context
import com.example.data.model.ChecklistRecordEntity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementação do provedor de nuvem para Google Drive.
 * Organiza a estrutura hierárquica automática:
 * CHECKLIST VTR -> [ANO] -> [MÊS EM PORTUGUÊS] -> [DIA]
 * e envia o PDF oficial sem alterar o arquivo gerado localmente.
 */
class GoogleDriveProvider(
    private val context: Context,
    private val restService: GoogleDriveRestService = GoogleDriveRestService()
) : CloudStorageProvider {

    override val providerId: String = "google_drive"
    override val displayName: String = "Google Drive"

    companion object {
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val ROOT_FOLDER_NAME = "CHECKLIST VTR"
    }

    override fun isConnected(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))
    }

    override fun getConnectedAccountEmail(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return if (GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
            account.email
        } else {
            null
        }
    }

    override suspend fun uploadReportPdf(
        record: ChecklistRecordEntity,
        pdfFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)?.account
                ?: return@withContext Result.failure(
                    IllegalStateException("Conecte uma conta Google Drive para enviar o relatório para a nuvem.")
                )

            // Obter token OAuth 2.0 válido através do Google Play Services
            val token = GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_SCOPE")

            // Extrair data institucional de finalização do checklist
            val dateHierarchy = DriveFolderHelper.extractDateHierarchy(record)

            // 1. Localizar ou criar pasta principal CHECKLIST VTR
            val mainFolderId = restService.getOrCreateFolder(
                token = token,
                parentId = "root",
                folderName = ROOT_FOLDER_NAME
            )

            // 2. Localizar ou criar pasta do ANO (ex: 2026)
            val yearFolderId = restService.getOrCreateFolder(
                token = token,
                parentId = mainFolderId,
                folderName = dateHierarchy.year
            )

            // 3. Localizar ou criar pasta do MÊS (ex: SETEMBRO)
            val monthFolderId = restService.getOrCreateFolder(
                token = token,
                parentId = yearFolderId,
                folderName = dateHierarchy.month
            )

            // 4. Localizar ou criar pasta do DIA (ex: 14)
            val dayFolderId = restService.getOrCreateFolder(
                token = token,
                parentId = monthFolderId,
                folderName = dateHierarchy.day
            )

            // 5. Enviar o PDF exatamente igual ao gerado localmente para a pasta do dia
            val fileId = restService.uploadPdf(
                token = token,
                folderId = dayFolderId,
                pdfFile = pdfFile
            )

            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
