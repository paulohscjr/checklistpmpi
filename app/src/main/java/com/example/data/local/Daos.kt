package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChecklistRecordEntity
import com.example.data.model.GrandCommandEntity
import com.example.data.model.OfficerEntity
import com.example.data.model.PoliceUnitEntity
import com.example.data.model.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrandCommandDao {
    @Query("SELECT * FROM grand_commands ORDER BY code ASC")
    fun getAllGrandCommands(): Flow<List<GrandCommandEntity>>

    @Query("SELECT COUNT(*) FROM grand_commands")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<GrandCommandEntity>)
}

@Dao
interface PoliceUnitDao {
    @Query("SELECT * FROM police_units ORDER BY abbreviation ASC")
    fun getAllUnits(): Flow<List<PoliceUnitEntity>>

    @Query("SELECT * FROM police_units WHERE grandCommandCode = :grandCommandCode ORDER BY abbreviation ASC")
    fun getUnitsByCommand(grandCommandCode: String): Flow<List<PoliceUnitEntity>>

    @Query("SELECT COUNT(*) FROM police_units")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<PoliceUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: PoliceUnitEntity): Long

    @Query("DELETE FROM police_units WHERE id NOT IN (SELECT MIN(id) FROM police_units GROUP BY abbreviation)")
    suspend fun removeDuplicates()

    @Query("UPDATE police_units SET name = :newName WHERE name LIKE :oldPattern")
    suspend fun updateUnitNamePattern(oldPattern: String, newName: String)

    @Query("DELETE FROM police_units WHERE abbreviation = :abbrev")
    suspend fun deleteByAbbreviation(abbrev: String)
}

@Dao
interface OfficerDao {
    @Query("SELECT * FROM officers ORDER BY fullName ASC")
    fun getAllOfficers(): Flow<List<OfficerEntity>>

    @Query("SELECT * FROM officers WHERE LOWER(rank) = LOWER(:rank) AND LOWER(warName) = LOWER(:warName) LIMIT 1")
    suspend fun findByRankAndWarName(rank: String, warName: String): OfficerEntity?

    @Query("SELECT COUNT(*) FROM officers")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(officer: OfficerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(officers: List<OfficerEntity>)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY prefix ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE linkedUnit = :unitName ORDER BY prefix ASC")
    fun getVehiclesByUnit(unitName: String): Flow<List<VehicleEntity>>

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vehicles: List<VehicleEntity>)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_records ORDER BY completedTimestampMillis DESC")
    fun getAllChecklists(): Flow<List<ChecklistRecordEntity>>

    @Query("SELECT * FROM checklist_records WHERE id = :id")
    suspend fun getChecklistById(id: Long): ChecklistRecordEntity?

    @Query("SELECT COUNT(*) FROM checklist_records")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ChecklistRecordEntity): Long

    @Update
    suspend fun update(record: ChecklistRecordEntity)
}
