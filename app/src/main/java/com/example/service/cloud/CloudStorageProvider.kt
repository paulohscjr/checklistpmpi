package com.example.service.cloud

import com.example.data.model.ChecklistRecordEntity
import java.io.File

/**
 * Interface abstrata e modular para provedores de armazenamento em nuvem.
 * Permite que a UI permaneça desacoplada dos detalhes de implementação
 * de cada serviço (como Google Drive, OneDrive, etc.).
 */
interface CloudStorageProvider {
    val providerId: String
    val displayName: String

    fun isConnected(): Boolean
    fun getConnectedAccountEmail(): String?

    suspend fun uploadReportPdf(record: ChecklistRecordEntity, pdfFile: File): Result<String>
}
