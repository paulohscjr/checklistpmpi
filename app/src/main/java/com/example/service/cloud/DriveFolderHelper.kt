package com.example.service.cloud

import com.example.data.model.ChecklistRecordEntity
import java.util.Calendar

/**
 * Utilitário para cálculo e extração da hierarquia de pastas da PMPI no Google Drive:
 * CHECKLIST VTR -> [ANO] -> [MÊS EM PORTUGUÊS] -> [DIA]
 */
object DriveFolderHelper {
    private val PORTUGUESE_MONTHS = arrayOf(
        "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
        "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
    )

    data class ReportDateHierarchy(
        val year: String,
        val month: String,
        val day: String
    )

    fun extractDateHierarchy(record: ChecklistRecordEntity): ReportDateHierarchy {
        val cal = Calendar.getInstance()

        // 1. Preferir timestamp de conclusão caso disponível
        if (record.completedTimestampMillis > 0) {
            cal.timeInMillis = record.completedTimestampMillis
            val year = cal.get(Calendar.YEAR).toString()
            val monthIdx = cal.get(Calendar.MONTH)
            val month = if (monthIdx in PORTUGUESE_MONTHS.indices) PORTUGUESE_MONTHS[monthIdx] else "MÊS"
            val day = cal.get(Calendar.DAY_OF_MONTH).toString()
            return ReportDateHierarchy(year = year, month = month, day = day)
        }

        // 2. Extrair da data de verificação ou data formatada caso o timestamp seja 0
        val dateString = record.completedDateFormatted.ifBlank { record.verificationDate }
        val dmyMatch = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})").find(dateString)
        if (dmyMatch != null) {
            val d = dmyMatch.groupValues[1].toIntOrNull() ?: 1
            val m = dmyMatch.groupValues[2].toIntOrNull() ?: 1
            var y = dmyMatch.groupValues[3]
            if (y.length == 2) y = "20$y"

            val monthIdx = (m - 1).coerceIn(0, 11)
            return ReportDateHierarchy(
                year = y,
                month = PORTUGUESE_MONTHS[monthIdx],
                day = d.toString()
            )
        }

        // 3. Fallback seguro para data atual
        val year = cal.get(Calendar.YEAR).toString()
        val monthIdx = cal.get(Calendar.MONTH)
        val month = if (monthIdx in PORTUGUESE_MONTHS.indices) PORTUGUESE_MONTHS[monthIdx] else "MÊS"
        val day = cal.get(Calendar.DAY_OF_MONTH).toString()
        return ReportDateHierarchy(year = year, month = month, day = day)
    }
}
