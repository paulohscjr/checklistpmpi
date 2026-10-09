package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChecklistRecordEntity
import com.example.data.model.GrandCommandEntity
import com.example.data.model.OfficerEntity
import com.example.data.model.PoliceUnitEntity
import com.example.data.model.VehicleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GrandCommandEntity::class,
        PoliceUnitEntity::class,
        OfficerEntity::class,
        VehicleEntity::class,
        ChecklistRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun grandCommandDao(): GrandCommandDao
    abstract fun policeUnitDao(): PoliceUnitDao
    abstract fun officerDao(): OfficerDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pmpi_checklist_db"
                ).fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        cleanAndSyncDatabase(database)
                    }
                }
            }
        }

        suspend fun cleanAndSyncDatabase(database: AppDatabase) {
            try {
                val unitDao = database.policeUnitDao()
                // 1. Remove duplicate units from database
                unitDao.removeDuplicates()

                // 2. Fix legacy name for Companhia Independente de Operações Aéreas
                unitDao.updateUnitNamePattern("%Aviação e Policiamento Aéreo%", "Companhia Independente de Operações Aéreas")
                unitDao.updateUnitNamePattern("%1ª Companhia Independente de Aviação%", "1ª Companhia Independente de Operações Aéreas")
                unitDao.updateUnitNamePattern("%2ª Companhia Independente de Aviação%", "2ª Companhia Independente de Operações Aéreas")
                unitDao.updateUnitNamePattern("%3ª Companhia Independente de Aviação%", "3ª Companhia Independente de Operações Aéreas")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val cmdDao = database.grandCommandDao()
            val unitDao = database.policeUnitDao()
            val officerDao = database.officerDao()
            val vehicleDao = database.vehicleDao()

            cleanAndSyncDatabase(database)

            if (cmdDao.getCount() == 0) {
                cmdDao.insertAll(
                    listOf(
                        GrandCommandEntity("CPM", "COMANDO DE POLICIAMENTO METROPOLITANO"),
                        GrandCommandEntity("CPLMN", "COMANDO DE POLICIAMENTO DO LITORAL MEIO-NORTE"),
                        GrandCommandEntity("CPSA", "COMANDO DE POLICIAMENTO DO SEMIÁRIDO"),
                        GrandCommandEntity("CPCE", "COMANDO DE POLICIAMENTO DOS CERRADOS"),
                        GrandCommandEntity("CPE", "COMANDO DE POLICIAMENTO ESPECIALIZADO"),
                        GrandCommandEntity("CPCOM", "COMANDO DE POLÍCIA COMUNITÁRIA"),
                        GrandCommandEntity("COPAer", "COMANDO DE AVIAÇÃO E OPERAÇÕES AÉREAS"),
                        GrandCommandEntity("CPTRAN", "COMANDO DE POLICIAMENTO DE TRÂNSITO"),
                        GrandCommandEntity("CPA", "COMANDO DE POLICIAMENTO AMBIENTAL")
                    )
                )
            }

            if (unitDao.getCount() == 0) {
                val units = mutableListOf<PoliceUnitEntity>()
                // CPM
                listOf(
                    "1º Batalhão de Polícia Militar" to "1º BPM",
                    "5º Batalhão de Polícia Militar" to "5º BPM",
                    "6º Batalhão de Polícia Militar" to "6º BPM",
                    "8º Batalhão de Polícia Militar" to "8º BPM",
                    "9º Batalhão de Polícia Militar" to "9º BPM",
                    "13º Batalhão de Polícia Militar" to "13º BPM",
                    "16º Batalhão de Polícia Militar" to "16º BPM",
                    "17º Batalhão de Polícia Militar" to "17º BPM",
                    "18º Batalhão de Polícia Militar" to "18º BPM",
                    "21º Batalhão de Polícia Militar" to "21º BPM",
                    "22º Batalhão de Polícia Militar" to "22º BPM",
                    "26º Batalhão de Polícia Militar" to "26º BPM",
                    "29º Batalhão de Polícia Militar" to "29º BPM",
                    "Batalhão de Policiamento de Guardas" to "BPGdas"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPM", name = it.first, abbreviation = it.second)) }

                // CPLMN
                listOf(
                    "2º Batalhão de Polícia Militar" to "2º BPM",
                    "12º Batalhão de Polícia Militar" to "12º BPM",
                    "15º Batalhão de Polícia Militar" to "15º BPM",
                    "24º Batalhão de Polícia Militar" to "24º BPM",
                    "25º Batalhão de Polícia Militar" to "25º BPM",
                    "27º Batalhão de Polícia Militar" to "27º BPM",
                    "30º Batalhão de Polícia Militar" to "30º BPM"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPLMN", name = it.first, abbreviation = it.second)) }

                // CPSA
                listOf(
                    "4º Batalhão de Polícia Militar" to "4º BPM",
                    "11º Batalhão de Polícia Militar" to "11º BPM",
                    "14º Batalhão de Polícia Militar" to "14º BPM",
                    "20º Batalhão de Polícia Militar" to "20º BPM",
                    "23º Batalhão de Polícia Militar" to "23º BPM"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPSA", name = it.first, abbreviation = it.second)) }

                // CPCE
                listOf(
                    "3º Batalhão de Polícia Militar" to "3º BPM",
                    "7º Batalhão de Polícia Militar" to "7º BPM",
                    "10º Batalhão de Polícia Militar" to "10º BPM",
                    "19º Batalhão de Polícia Militar" to "19º BPM",
                    "28º Batalhão de Polícia Militar" to "28º BPM"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPCE", name = it.first, abbreviation = it.second)) }

                // CPE
                listOf(
                    "Batalhão de Operações Policiais Especiais" to "BOPE",
                    "Batalhão de Polícia Rondas Ostensivas de Natureza Especial" to "RONE",
                    "Batalhão de Polícia de Choque" to "BPCHOQUE",
                    "Batalhão de Polícia Rondas Ostensivas com Apoio de Motocicletas" to "ROCAM",
                    "Batalhão Especial de Policiamento do Interior" to "BEPI",
                    "Regimento de Policiamento Montado" to "RPMont"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPE", name = it.first, abbreviation = it.second)) }

                // CPCOM
                listOf(
                    "Coordenadoria Estadual do Programa Educacional de Resistência às Drogas e à Violência" to "PROERD",
                    "Coordenadoria Estadual do Programa Preventivo e Educativo Social Mirim" to "CPMirim",
                    "Coordenadoria de Prevenção e Enfrentamento à Violência Doméstica" to "Patrulha Maria da Penha",
                    "Companhia Independente de Policiamento Escolar" to "CIPE",
                    "Companhia Independente de Ciclopatrulhamento" to "CICLOPATRULHA"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPCOM", name = it.first, abbreviation = it.second)) }

                // COPAer
                listOf(
                    "Batalhão de Operações Aéreas" to "BOPAer",
                    "1ª Companhia Independente de Operações Aéreas" to "1ª CIOPAer",
                    "2ª Companhia Independente de Operações Aéreas" to "2ª CIOPAer",
                    "3ª Companhia Independente de Operações Aéreas" to "3ª CIOPAer"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "COPAer", name = it.first, abbreviation = it.second)) }

                // CPTRAN
                listOf(
                    "Batalhão de Policiamento de Trânsito" to "BPTRAN",
                    "Batalhão de Policiamento Rodoviário Estadual" to "BPRE",
                    "1ª Companhia Independente de Policiamento de Trânsito" to "1ª CITRAN",
                    "2ª Companhia Independente de Policiamento de Trânsito" to "2ª CITRAN",
                    "3ª Companhia Independente de Policiamento de Trânsito" to "3ª CITRAN"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPTRAN", name = it.first, abbreviation = it.second)) }

                // CPA
                listOf(
                    "Batalhão de Policiamento Ambiental" to "BPA",
                    "1ª Companhia Independente de Policiamento Ambiental" to "1ª CIPA",
                    "2ª Companhia Independente de Policiamento Ambiental" to "2ª CIPA",
                    "3ª Companhia Independente de Policiamento Ambiental" to "3ª CIPA"
                ).forEach { units.add(PoliceUnitEntity(grandCommandCode = "CPA", name = it.first, abbreviation = it.second)) }

                unitDao.insertAll(units)
            }

            if (officerDao.getCount() == 0) {
                officerDao.insertAll(
                    listOf(
                        OfficerEntity(rank = "Capitão", fullName = "Paulo Henrique da Silva", warName = "Paulo Henrique"),
                        OfficerEntity(rank = "1º Tenente", fullName = "Marcos Vinicius de Sousa", warName = "M. Sousa"),
                        OfficerEntity(rank = "1º Sargento", fullName = "Raimundo Nonato Pereira", warName = "Nonato"),
                        OfficerEntity(rank = "2º Sargento", fullName = "Antonio Carlos Ribeiro", warName = "A. Carlos"),
                        OfficerEntity(rank = "3º Sargento", fullName = "Francisco de Assis Castro", warName = "Castro"),
                        OfficerEntity(rank = "Cabo", fullName = "José Ribamar Albuquerque", warName = "Ribamar"),
                        OfficerEntity(rank = "Cabo", fullName = "Lucas Gabriel Mendes", warName = "Mendes"),
                        OfficerEntity(rank = "Soldado", fullName = "Thiago Ferreira Lima", warName = "Ferreira"),
                        OfficerEntity(rank = "Soldado", fullName = "Rafael dos Santos Bezerra", warName = "Bezerra")
                    )
                )
            }

            if (vehicleDao.getCount() == 0) {
                vehicleDao.insertAll(
                    listOf(
                        VehicleEntity(prefix = "VTR 2901", model = "Toyota Hilux", plate = "PIX-2901", linkedUnit = "29º BPM"),
                        VehicleEntity(prefix = "VTR 2902", model = "Renault Duster", plate = "PIX-2902", linkedUnit = "29º BPM"),
                        VehicleEntity(prefix = "VTR 0101", model = "Toyota Hilux", plate = "PIX-0101", linkedUnit = "1º BPM"),
                        VehicleEntity(prefix = "VTR 0501", model = "Chevrolet S10", plate = "PIX-0501", linkedUnit = "5º BPM"),
                        VehicleEntity(prefix = "VTR 0801", model = "Toyota Hilux", plate = "PIX-0801", linkedUnit = "8º BPM"),
                        VehicleEntity(prefix = "VTR 0901", model = "Renault Duster", plate = "PIX-0901", linkedUnit = "9º BPM"),
                        VehicleEntity(prefix = "VTR 0201", model = "Toyota Hilux", plate = "PIX-0201", linkedUnit = "2º BPM"),
                        VehicleEntity(prefix = "VTR 0401", model = "Chevrolet S10", plate = "PIX-0401", linkedUnit = "4º BPM"),
                        VehicleEntity(prefix = "VTR BOPE-01", model = "Toyota Hilux 4x4", plate = "PIX-9001", linkedUnit = "BOPE"),
                        VehicleEntity(prefix = "VTR RONE-01", model = "Toyota Hilux 4x4", plate = "PIX-9002", linkedUnit = "RONE")
                    )
                )
            }
        }
    }
}
