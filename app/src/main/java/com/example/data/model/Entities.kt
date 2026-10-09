package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "grand_commands")
data class GrandCommandEntity(
    @PrimaryKey val code: String, // CPM, CPLMN, CPSA, CPCE, CPE, CPCOM, COPAer, CPTRAN, CPA
    val name: String
)

@Entity(
    tableName = "police_units",
    indices = [Index(value = ["abbreviation"], unique = true)]
)
data class PoliceUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grandCommandCode: String,
    val name: String,
    val abbreviation: String
)

@Entity(tableName = "officers")
data class OfficerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: String, // Posto/Graduação
    val fullName: String,
    val warName: String // Nome de guerra
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prefix: String, // Ex: VTR 2901
    val model: String,  // Ex: Toyota Hilux
    val plate: String,  // Ex: XXX-0000
    val linkedUnit: String // Ex: 29º BPM
)

@Entity(tableName = "checklist_records")
data class ChecklistRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportNumber: String, // Ex: 001
    val verificationDate: String, // Ex: 13/09/2026
    val grandCommand: String, // Ex: CPM
    val unitName: String, // Ex: 29º BPM
    val vehiclePrefix: String = "", // Opcional / Legado
    val vehicleModel: String, // Ex: Toyota Hilux
    val vehiclePlate: String, // Ex: XXX-0000
    val serviceModality: String, // Ordinário, Diário, Planejada
    val responsibleRank: String, // Ex: Capitão
    val responsibleName: String, // Ex: Paulo Henrique
    val commanderName: String,
    val driverName: String,
    val patrolman01Name: String,
    val patrolman02Name: String,
    val initialMileage: String,
    val oilStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val oilReason: String = "",
    val radiatorWaterStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val radiatorWaterReason: String = "",
    val tiresStatus: String, // SIM / NÃO
    val tiresReason: String = "",
    val lightbarStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val lightbarReason: String = "",
    val radioStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val radioReason: String = "",
    val spareTireStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val spareTireReason: String = "",
    val jackStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val jackReason: String = "",
    val headlightsStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val headlightsReason: String = "",
    val airConditioningStatus: String, // SEM ALTERAÇÃO / COM ALTERAÇÃO
    val airConditioningReason: String = "",
    val photoFrontPath: String? = null,
    val photoDriverSidePath: String? = null,
    val photoPassengerSidePath: String? = null,
    val photoRearPath: String? = null,
    val completedDateFormatted: String, // Ex: 13/09/2026 — 20:35
    val completedTimestampMillis: Long,
    val pdfFilePath: String? = null
)
