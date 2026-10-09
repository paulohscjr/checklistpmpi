package com.example.service.cloud

import android.net.Uri
import com.example.data.model.ChecklistRecordEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Envia o checklist (dados + PDF) para o Firebase.
 *
 *  - Auth anônima: o policial não precisa de login, mas cada instalação tem um uid.
 *  - Storage: reports/{uid}/{docId}.pdf
 *  - Firestore: reports/{docId}
 *
 * O app só ESCREVE. A leitura é restrita a administradores pelas regras
 * (ver pasta /firebase).
 */
class FirebaseCloudStorageService : CloudStorageService {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy {
        FirebaseStorage.getInstance().apply { maxUploadRetryTimeMillis = 30_000L }
    }

    private suspend fun ensureUid(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().awaitResult()
        return result.user?.uid ?: error("Falha na autenticação anônima.")
    }

    override suspend fun saveReport(record: ChecklistRecordEntity, pdfFile: File?): Result<String> {
        if (pdfFile == null || !pdfFile.exists()) {
            return Result.failure(IllegalArgumentException("PDF do relatório não encontrado."))
        }
        return runCatching {
            withTimeout(UPLOAD_TIMEOUT_MS) {
                val uid = ensureUid()
                // ID determinístico: reenviar o mesmo checklist nunca cria duplicata.
                val docId = "${uid}_${record.id}_${record.completedTimestampMillis}"
                val pdfPath = "reports/$uid/$docId.pdf"

                storage.reference.child(pdfPath)
                    .putFile(
                        Uri.fromFile(pdfFile),
                        StorageMetadata.Builder().setContentType("application/pdf").build()
                    )
                    .awaitResult()

                val data = ReportMapper.buildDocument(record, uid, pdfPath)
                val ref = firestore.collection("reports").document(docId)
                // Transação (e não set direto): falha rápido sem internet em vez de
                // ficar numa fila offline que enviaria depois, em duplicidade.
                firestore.runTransaction(Transaction.Function<Any?> { tx ->
                    tx.set(ref, data)
                    null
                }).awaitResult()

                docId
            }
        }
    }

    // O app do policial não lê relatórios; isso é feito no painel web de administradores.
    override suspend fun listReports(): Result<List<CloudReportSummary>> =
        Result.success(emptyList())

    override suspend fun getReport(cloudId: String): Result<ChecklistRecordEntity?> =
        Result.failure(UnsupportedOperationException("Consulta disponível apenas no painel de administradores."))

    override suspend fun uploadPdf(recordId: Long, pdfFile: File): Result<String> =
        Result.failure(UnsupportedOperationException("Use saveReport (envia dados e PDF juntos)."))

    override suspend fun syncData(): Result<SyncResult> =
        Result.success(SyncResult(success = true, syncedCount = 0, message = "Use a sincronização de pendentes."))

    private companion object {
        const val UPLOAD_TIMEOUT_MS = 90_000L
    }
}

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
