package com.example

import com.example.data.model.ChecklistRecordEntity
import com.example.util.pdf.PdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun testPdfFilenameFormatting() {
        val numParte = PdfGenerator.formatNumeroParte("1")
        assertEquals("001", numParte)

        val numParteComplex = PdfGenerator.formatNumeroParte("042/2026")
        assertEquals("042", numParteComplex)

        val dateFormatted = PdfGenerator.formatDataServico("13/09/2026")
        assertEquals("13092026", dateFormatted)

        val elaborador = PdfGenerator.formatNomeElaborador("Sd PM Paulo Henrique")
        assertEquals("Sd_PM_Paulo_Henrique", elaborador)

        val record = ChecklistRecordEntity(
            reportNumber = "5",
            verificationDate = "13/09/2026",
            grandCommand = "CPM",
            unitName = "1º BPM",
            vehiclePrefix = "VTR-101",
            vehicleModel = "Toyota Hilux 4x4",
            vehiclePlate = "PI-PMPI-101",
            serviceModality = "Ordinário",
            responsibleRank = "Soldado",
            responsibleName = "Paulo Henrique",
            commanderName = "",
            driverName = "Silva",
            patrolman01Name = "",
            patrolman02Name = "",
            initialMileage = "124500",
            oilStatus = "SEM ALTERAÇÃO",
            oilReason = "",
            radiatorWaterStatus = "SEM ALTERAÇÃO",
            radiatorWaterReason = "",
            tiresStatus = "SIM",
            tiresReason = "",
            lightbarStatus = "SEM ALTERAÇÃO",
            lightbarReason = "",
            radioStatus = "SEM ALTERAÇÃO",
            radioReason = "",
            spareTireStatus = "SEM ALTERAÇÃO",
            spareTireReason = "",
            jackStatus = "SEM ALTERAÇÃO",
            jackReason = "",
            headlightsStatus = "SEM ALTERAÇÃO",
            headlightsReason = "",
            airConditioningStatus = "SEM ALTERAÇÃO",
            airConditioningReason = "",
            completedDateFormatted = "13/09/2026 — 20:35",
            completedTimestampMillis = 1726260000000L
        )

        val fileName = PdfGenerator.getPdfFileName(record)
        assertEquals("Relatorio_Parte_005_13092026_Silva.pdf", fileName)
    }
}
