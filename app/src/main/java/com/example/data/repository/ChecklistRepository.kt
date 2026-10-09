package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.ChecklistRecordEntity
import com.example.data.model.GrandCommandEntity
import com.example.data.model.OfficerEntity
import com.example.data.model.PoliceUnitEntity
import com.example.data.model.VehicleEntity
import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val db: AppDatabase) {
    val grandCommands: Flow<List<GrandCommandEntity>> = db.grandCommandDao().getAllGrandCommands()
    val policeUnits: Flow<List<PoliceUnitEntity>> = db.policeUnitDao().getAllUnits()
    val officers: Flow<List<OfficerEntity>> = db.officerDao().getAllOfficers()
    val vehicles: Flow<List<VehicleEntity>> = db.vehicleDao().getAllVehicles()
    val checklists: Flow<List<ChecklistRecordEntity>> = db.checklistDao().getAllChecklists()

    fun getUnitsByCommand(code: String): Flow<List<PoliceUnitEntity>> =
        db.policeUnitDao().getUnitsByCommand(code)

    fun getVehiclesByUnit(unit: String): Flow<List<VehicleEntity>> =
        db.vehicleDao().getVehiclesByUnit(unit)

    suspend fun insertUnit(unit: PoliceUnitEntity): Long {
        return db.policeUnitDao().insert(unit)
    }

    suspend fun insertOfficer(officer: OfficerEntity): Result<Long> {
        val existing = db.officerDao().findByRankAndWarName(officer.rank, officer.warName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Este policial já está cadastrado."))
        }
        val id = db.officerDao().insert(officer)
        return Result.success(id)
    }

    suspend fun insertVehicle(vehicle: VehicleEntity): Long {
        return db.vehicleDao().insert(vehicle)
    }

    suspend fun getNextReportNumber(): String {
        val count = db.checklistDao().getCount() + 1
        return String.format("%03d", count)
    }

    suspend fun getChecklistById(id: Long): ChecklistRecordEntity? {
        return db.checklistDao().getChecklistById(id)
    }

    suspend fun saveChecklist(record: ChecklistRecordEntity): Long {
        return db.checklistDao().insert(record)
    }

    suspend fun updateChecklist(record: ChecklistRecordEntity) {
        db.checklistDao().update(record)
    }
}
