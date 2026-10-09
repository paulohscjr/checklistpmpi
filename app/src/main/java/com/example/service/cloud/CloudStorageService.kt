package com.example.service.cloud

import com.example.data.model.ChecklistRecordEntity
import java.io.File

data class CloudReportSummary(
    val cloudId: String,
    val reportNumber: String,
    val vehiclePrefix: String,
    val completedDate: String
)

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val message: String
)

/**
 * Interface conceitual de armazenamento em nuvem para futura integração
 * com serviços como Google Drive, OneDrive ou servidores institucionais da PMPI.
 */
interface CloudStorageService {
    suspend fun saveReport(record: ChecklistRecordEntity, pdfFile: File?): Result<String>
    suspend fun listReports(): Result<List<CloudReportSummary>>
    suspend fun getReport(cloudId: String): Result<ChecklistRecordEntity?>
    suspend fun uploadPdf(recordId: Long, pdfFile: File): Result<String>
    suspend fun syncData(): Result<SyncResult>
}

/**
 * Implementação local-only que atua como fallback seguro e desacoplado
 * até a configuração de provedor de nuvem em atualização futura.
 */
class LocalOnlyCloudStorageService : CloudStorageService {
    override suspend fun saveReport(record: ChecklistRecordEntity, pdfFile: File?): Result<String> {
        return Result.failure(UnsupportedOperationException("Função de armazenamento em nuvem disponível em atualização futura."))
    }

    override suspend fun listReports(): Result<List<CloudReportSummary>> {
        return Result.success(emptyList())
    }

    override suspend fun getReport(cloudId: String): Result<ChecklistRecordEntity?> {
        return Result.failure(UnsupportedOperationException("Função de armazenamento em nuvem disponível em atualização futura."))
    }

    override suspend fun uploadPdf(recordId: Long, pdfFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("Função de armazenamento em nuvem disponível em atualização futura."))
    }

    override suspend fun syncData(): Result<SyncResult> {
        return Result.success(SyncResult(success = true, syncedCount = 0, message = "Modo offline ativo."))
    }
}
