package com.example.service.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChecklistRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GoogleDriveIntegrationTest {

    @Test
    fun testDateHierarchyExtraction_standardDateFormat() {
        val record = ChecklistRecordEntity(
            reportNumber = "001",
            verificationDate = "14/09/2026",
            grandCommand = "CPM",
            unitName = "29º BPM",
            vehiclePrefix = "VTR-2901",
            vehicleModel = "Duster",
            vehiclePlate = "PI-2901",
            serviceModality = "Ordinário",
            responsibleRank = "Cabo",
            responsibleName = "Carlos Silva",
            commanderName = "",
            driverName = "Carlos Silva",
            patrolman01Name = "",
            patrolman02Name = "",
            initialMileage = "50000",
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
            completedDateFormatted = "14/09/2026 — 10:30",
            completedTimestampMillis = 0L
        )

        val hierarchy = DriveFolderHelper.extractDateHierarchy(record)

        assertEquals("2026", hierarchy.year)
        assertEquals("SETEMBRO", hierarchy.month)
        assertEquals("14", hierarchy.day)
    }

    @Test
    fun testDateHierarchyExtraction_timestamp() {
        // 2026-09-14 12:00:00 UTC = 1789387200000L approx
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Sao_Paulo"))
        calendar.set(2026, java.util.Calendar.SEPTEMBER, 14, 15, 30, 0)
        val timestamp = calendar.timeInMillis

        val record = ChecklistRecordEntity(
            reportNumber = "002",
            verificationDate = "",
            grandCommand = "CPM",
            unitName = "29º BPM",
            vehiclePrefix = "VTR-2901",
            vehicleModel = "Duster",
            vehiclePlate = "PI-2901",
            serviceModality = "Ordinário",
            responsibleRank = "Soldado",
            responsibleName = "João",
            commanderName = "",
            driverName = "João",
            patrolman01Name = "",
            patrolman02Name = "",
            initialMileage = "50000",
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
            completedDateFormatted = "",
            completedTimestampMillis = timestamp
        )

        val hierarchy = DriveFolderHelper.extractDateHierarchy(record)

        assertEquals("2026", hierarchy.year)
        assertEquals("SETEMBRO", hierarchy.month)
        assertEquals("14", hierarchy.day)
    }

    @Test
    fun testGoogleDriveProviderConstantsAndDefaults() {
        assertEquals("https://www.googleapis.com/auth/drive.file", GoogleDriveProvider.DRIVE_SCOPE)
        assertEquals("CHECKLIST VTR", GoogleDriveProvider.ROOT_FOLDER_NAME)

        val context: Context = ApplicationProvider.getApplicationContext()
        val provider = GoogleDriveProvider(context)

        assertEquals("google_drive", provider.providerId)
        assertEquals("Google Drive", provider.displayName)
        assertFalse(provider.isConnected())
    }

    @Test
    fun testGoogleDriveProvider_whenNotConnected_returnsHelpfulErrorAndKeepsLocalFileIntact() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val provider = GoogleDriveProvider(context)

        val testPdf = File(context.cacheDir, "Relatorio_Parte_001_14092026_Policial.pdf")
        testPdf.writeText("%PDF-1.4 Mock PDF Content for Unit Test")
        assertTrue(testPdf.exists())

        val record = ChecklistRecordEntity(
            reportNumber = "001",
            verificationDate = "14/09/2026",
            grandCommand = "CPM",
            unitName = "29º BPM",
            vehiclePrefix = "VTR-2901",
            vehicleModel = "Duster",
            vehiclePlate = "PI-2901",
            serviceModality = "Ordinário",
            responsibleRank = "Cabo",
            responsibleName = "Carlos Silva",
            commanderName = "",
            driverName = "Carlos Silva",
            patrolman01Name = "",
            patrolman02Name = "",
            initialMileage = "50000",
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
            completedDateFormatted = "14/09/2026 — 10:30",
            completedTimestampMillis = 0L,
            pdfFilePath = testPdf.absolutePath
        )

        val result = provider.uploadReportPdf(record, testPdf)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("Conecte uma conta Google Drive"))

        // O arquivo local NÃO deve ser excluído em caso de erro
        assertTrue(testPdf.exists())
    }
}
