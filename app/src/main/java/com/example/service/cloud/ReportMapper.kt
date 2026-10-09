package com.example.service.cloud

import com.example.BuildConfig
import com.example.data.model.ChecklistRecordEntity
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Converte um checklist local no documento que vai para o Firestore.
 *
 * O painel de administradores lê estes campos para montar os gráficos:
 *  - vehiclePlateKey: placa normalizada (agrupa a mesma VTR mesmo que digitada diferente)
 *  - hasDefect / defectCount / defectKeys: resumo de avarias
 *  - items: detalhe de cada item verificado
 */
object ReportMapper {

    private class ItemSpec(
        val key: String,
        val label: String,
        val status: (ChecklistRecordEntity) -> String,
        val reason: (ChecklistRecordEntity) -> String
    )

    private val ITEMS = listOf(
        ItemSpec("oil", "Óleo do motor", { it.oilStatus }, { it.oilReason }),
        ItemSpec("radiatorWater", "Água do radiador", { it.radiatorWaterStatus }, { it.radiatorWaterReason }),
        ItemSpec("tires", "Pneus", { it.tiresStatus }, { it.tiresReason }),
        ItemSpec("lightbar", "Giroflex / sinalizador", { it.lightbarStatus }, { it.lightbarReason }),
        ItemSpec("radio", "Rádio comunicador", { it.radioStatus }, { it.radioReason }),
        ItemSpec("spareTire", "Estepe", { it.spareTireStatus }, { it.spareTireReason }),
        ItemSpec("jack", "Macaco hidráulico", { it.jackStatus }, { it.jackReason }),
        ItemSpec("headlights", "Faróis", { it.headlightsStatus }, { it.headlightsReason }),
        ItemSpec("airConditioning", "Ar-condicionado", { it.airConditioningStatus }, { it.airConditioningReason })
    )

    /** "COM ALTERAÇÃO" em qualquer item, ou "NÃO" em Pneus (pneus em ordem = SIM). */
    fun isAltered(status: String): Boolean {
        val s = status.trim().uppercase()
        return s == "COM ALTERAÇÃO" || s == "NÃO"
    }

    fun normalizePlate(plate: String): String =
        plate.uppercase().filter { it.isLetterOrDigit() }

    fun buildDocument(
        record: ChecklistRecordEntity,
        deviceUid: String,
        pdfStoragePath: String
    ): Map<String, Any?> {
        val items = LinkedHashMap<String, Map<String, Any>>()
        val defectKeys = ArrayList<String>()
        for (spec in ITEMS) {
            val status = spec.status(record)
            val altered = isAltered(status)
            if (altered) defectKeys.add(spec.key)
            items[spec.key] = mapOf(
                "label" to spec.label,
                "status" to status,
                "altered" to altered,
                "reason" to spec.reason(record)
            )
        }

        // Sem campo de data do servidor de propósito: o reenvio fica idêntico
        // e as regras do Firestore aceitam esse reenvio como idempotente.
        return mapOf(
            "deviceUid" to deviceUid,
            "reportNumber" to record.reportNumber,
            "verificationDate" to record.verificationDate,
            "grandCommand" to record.grandCommand,
            "unitName" to record.unitName,
            "vehicleModel" to record.vehicleModel,
            "vehiclePlate" to record.vehiclePlate,
            "vehiclePlateKey" to normalizePlate(record.vehiclePlate),
            "serviceModality" to record.serviceModality,
            "responsibleRank" to record.responsibleRank,
            "responsibleName" to record.responsibleName,
            "commanderName" to record.commanderName,
            "driverName" to record.driverName,
            "patrolman01Name" to record.patrolman01Name,
            "patrolman02Name" to record.patrolman02Name,
            "initialMileage" to record.initialMileage,
            "hasDefect" to defectKeys.isNotEmpty(),
            "defectCount" to defectKeys.size,
            "defectKeys" to defectKeys,
            "items" to items,
            "completedAt" to Timestamp(Date(record.completedTimestampMillis)),
            "completedDateFormatted" to record.completedDateFormatted,
            "pdfPath" to pdfStoragePath,
            "appVersion" to BuildConfig.VERSION_NAME
        )
    }
}
