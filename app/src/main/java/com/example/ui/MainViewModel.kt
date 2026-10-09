package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdminPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.SyncPreferences
import com.example.data.model.ChecklistRecordEntity
import com.example.data.model.GrandCommandEntity
import com.example.data.model.OfficerEntity
import com.example.data.model.PoliceUnitEntity
import com.example.data.model.VehicleEntity
import com.example.data.repository.ChecklistRepository
import com.example.service.cloud.CloudStorageProvider
import com.example.service.cloud.CloudStorageService
import com.example.service.cloud.FirebaseCloudStorageService
import com.example.service.cloud.GoogleDriveProvider
import com.example.util.pdf.PdfGenerator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    HOME,
    NEW_CHECKLIST,
    REPORT_COMPLETED,
    ADMIN_UNITS,
    SAVED_CHECKLISTS
}

data class ChecklistFormData(
    val verificationDate: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val grandCommand: String = "",
    val unitName: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val serviceModality: String = "Ordinário",
    val responsibleRank: String = "Soldado",
    val responsibleName: String = "",
    val commanderName: String = "",
    val driverName: String = "",
    val patrolman01Name: String = "",
    val patrolman02Name: String = "",
    val initialMileage: String = "",
    val oilStatus: String = "SEM ALTERAÇÃO",
    val oilReason: String = "",
    val radiatorWaterStatus: String = "SEM ALTERAÇÃO",
    val radiatorWaterReason: String = "",
    val tiresStatus: String = "SIM",
    val tiresReason: String = "",
    val lightbarStatus: String = "SEM ALTERAÇÃO",
    val lightbarReason: String = "",
    val radioStatus: String = "SEM ALTERAÇÃO",
    val radioReason: String = "",
    val spareTireStatus: String = "SEM ALTERAÇÃO",
    val spareTireReason: String = "",
    val jackStatus: String = "SEM ALTERAÇÃO",
    val jackReason: String = "",
    val headlightsStatus: String = "SEM ALTERAÇÃO",
    val headlightsReason: String = "",
    val airConditioningStatus: String = "SEM ALTERAÇÃO",
    val airConditioningReason: String = "",
    val photoFrontPath: String? = null,
    val photoDriverSidePath: String? = null,
    val photoPassengerSidePath: String? = null,
    val photoRearPath: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = ChecklistRepository(db)
    val adminPreferences = AdminPreferences(application)
    val cloudStorageService: CloudStorageService = FirebaseCloudStorageService()
    private val syncPreferences = SyncPreferences(application)
    private val syncMutex = Mutex()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()
    val googleDriveProvider: GoogleDriveProvider = GoogleDriveProvider(application)

    private val _isDriveConnected = MutableStateFlow(googleDriveProvider.isConnected())
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected.asStateFlow()

    private val _driveAccountEmail = MutableStateFlow(googleDriveProvider.getConnectedAccountEmail())
    val driveAccountEmail: StateFlow<String?> = _driveAccountEmail.asStateFlow()

    private val _isUploadingToDrive = MutableStateFlow(false)
    val isUploadingToDrive: StateFlow<Boolean> = _isUploadingToDrive.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Configured Unit in Admin
    private val _configuredGrandCommandName = MutableStateFlow(adminPreferences.getConfiguredGrandCommandName())
    val configuredGrandCommandName: StateFlow<String> = _configuredGrandCommandName.asStateFlow()

    private val _configuredUnitName = MutableStateFlow(adminPreferences.getConfiguredUnitName())
    val configuredUnitName: StateFlow<String> = _configuredUnitName.asStateFlow()

    private val _configuredUnitAbbrev = MutableStateFlow(adminPreferences.getConfiguredUnitAbbrev())
    val configuredUnitAbbrev: StateFlow<String> = _configuredUnitAbbrev.asStateFlow()

    // Active Form State
    private val _formData = MutableStateFlow(ChecklistFormData())
    val formData: StateFlow<ChecklistFormData> = _formData.asStateFlow()

    // Last completed report
    private val _lastCompletedRecord = MutableStateFlow<ChecklistRecordEntity?>(null)
    val lastCompletedRecord: StateFlow<ChecklistRecordEntity?> = _lastCompletedRecord.asStateFlow()

    // UI Feedback Messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Data lists from DB
    val grandCommands: StateFlow<List<GrandCommandEntity>> = repository.grandCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val policeUnits: StateFlow<List<PoliceUnitEntity>> = repository.policeUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedChecklists: StateFlow<List<ChecklistRecordEntity>> = repository.checklists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.populateDatabase(db)
        }
        refreshDriveConnectionStatus()
        // Reenvia ao Firebase o que ficou pendente (ex.: checklist feito sem internet).
        syncPendingReports(notify = false)
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // --- UNIT CONFIGURATION ACTIONS ---

    fun setConfiguredUnit(cmdCode: String, cmdName: String, unitAbbrev: String, unitName: String) {
        adminPreferences.setConfiguredUnit(cmdCode, cmdName, unitAbbrev, unitName)
        _configuredGrandCommandName.value = cmdName
        _configuredUnitName.value = unitName
        _configuredUnitAbbrev.value = unitAbbrev
        showMessage("Unidade configurada: $unitAbbrev — $unitName")
    }

    // --- FORM ACTIONS ---

    fun startNewChecklist() {
        val configuredCmd = adminPreferences.getConfiguredGrandCommandName()
        val configuredUnit = adminPreferences.getConfiguredUnitName()
        _formData.value = ChecklistFormData(
            grandCommand = configuredCmd,
            unitName = configuredUnit
        )
        _currentScreen.value = AppScreen.NEW_CHECKLIST
    }

    fun updateFormData(update: (ChecklistFormData) -> ChecklistFormData) {
        _formData.value = update(_formData.value)
    }

    fun setPhoto(slotIndex: Int, path: String) {
        _formData.value = when (slotIndex) {
            0 -> _formData.value.copy(photoFrontPath = path)
            1 -> _formData.value.copy(photoDriverSidePath = path)
            2 -> _formData.value.copy(photoPassengerSidePath = path)
            3 -> _formData.value.copy(photoRearPath = path)
            else -> _formData.value
        }
    }

    // --- VALIDATION AND FINALIZE ---

    fun finalizeChecklist(onSuccess: (ChecklistRecordEntity) -> Unit) {
        val form = _formData.value

        // Validate mandatory unit fields
        if (form.grandCommand.isBlank() || form.unitName.isBlank()) {
            showMessage("Grand Comando e Unidade não estão configurados. Acesse a aba UNIDADE.")
            return
        }

        // Validate vehicle info
        if (form.vehicleModel.isBlank() || form.vehiclePlate.isBlank()) {
            showMessage("Preencha o Modelo e a Placa da Viatura.")
            return
        }

        // Validate driver info
        if (form.driverName.isBlank()) {
            showMessage("Informe o Nome Completo do Motorista da Viatura.")
            return
        }

        // Validate mileage
        if (form.initialMileage.isBlank()) {
            showMessage("Informe a Quilometragem Inicial da Viatura.")
            return
        }

        // Section 18: Validação dos Motivos de Alteração obrigatórios
        if (form.oilStatus == "COM ALTERAÇÃO" && form.oilReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Óleo do Motor antes de finalizar o checklist.")
            return
        }
        if (form.radiatorWaterStatus == "COM ALTERAÇÃO" && form.radiatorWaterReason.isBlank()) {
            showMessage("Informe o motivo da alteração na Água do Radiador antes de finalizar o checklist.")
            return
        }
        if (form.tiresStatus == "NÃO" && form.tiresReason.isBlank()) {
            showMessage("Informe o motivo da alteração nos Pneus antes de finalizar o checklist.")
            return
        }
        if (form.lightbarStatus == "COM ALTERAÇÃO" && form.lightbarReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Giroflex / Sinalizador antes de finalizar o checklist.")
            return
        }
        if (form.radioStatus == "COM ALTERAÇÃO" && form.radioReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Rádio Comunicador antes de finalizar o checklist.")
            return
        }
        if (form.spareTireStatus == "COM ALTERAÇÃO" && form.spareTireReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Estepe antes de finalizar o checklist.")
            return
        }
        if (form.jackStatus == "COM ALTERAÇÃO" && form.jackReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Macaco Hidráulico antes de finalizar o checklist.")
            return
        }
        if (form.headlightsStatus == "COM ALTERAÇÃO" && form.headlightsReason.isBlank()) {
            showMessage("Informe o motivo da alteração nos Faróis antes de finalizar o checklist.")
            return
        }
        if (form.airConditioningStatus == "COM ALTERAÇÃO" && form.airConditioningReason.isBlank()) {
            showMessage("Informe o motivo da alteração no Ar-Condicionado antes de finalizar o checklist.")
            return
        }

        // Section 21: Validação das 4 Fotografias Obrigatórias
        if (form.photoFrontPath.isNullOrBlank()) {
            showMessage("É necessário registrar a fotografia da frente da viatura para finalizar o checklist.")
            return
        }
        if (form.photoDriverSidePath.isNullOrBlank()) {
            showMessage("É necessário registrar a fotografia do lado do motorista para finalizar o checklist.")
            return
        }
        if (form.photoPassengerSidePath.isNullOrBlank()) {
            showMessage("É necessário registrar a fotografia do lado do passageiro para finalizar o checklist.")
            return
        }
        if (form.photoRearPath.isNullOrBlank()) {
            showMessage("É necessário registrar a fotografia da parte traseira da viatura para finalizar o checklist.")
            return
        }

        _isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nowMillis = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy — HH:mm", Locale.getDefault())
                val completedDateFormatted = dateFormat.format(Date(nowMillis))

                val nextNum = repository.getNextReportNumber()

                val record = ChecklistRecordEntity(
                    reportNumber = nextNum,
                    verificationDate = form.verificationDate,
                    grandCommand = form.grandCommand,
                    unitName = form.unitName,
                    vehiclePrefix = "",
                    vehicleModel = form.vehicleModel,
                    vehiclePlate = form.vehiclePlate,
                    serviceModality = form.serviceModality,
                    responsibleRank = form.responsibleRank,
                    responsibleName = form.responsibleName,
                    commanderName = form.commanderName,
                    driverName = form.driverName,
                    patrolman01Name = form.patrolman01Name,
                    patrolman02Name = form.patrolman02Name,
                    initialMileage = form.initialMileage,
                    oilStatus = form.oilStatus,
                    oilReason = form.oilReason,
                    radiatorWaterStatus = form.radiatorWaterStatus,
                    radiatorWaterReason = form.radiatorWaterReason,
                    tiresStatus = form.tiresStatus,
                    tiresReason = form.tiresReason,
                    lightbarStatus = form.lightbarStatus,
                    lightbarReason = form.lightbarReason,
                    radioStatus = form.radioStatus,
                    radioReason = form.radioReason,
                    spareTireStatus = form.spareTireStatus,
                    spareTireReason = form.spareTireReason,
                    jackStatus = form.jackStatus,
                    jackReason = form.jackReason,
                    headlightsStatus = form.headlightsStatus,
                    headlightsReason = form.headlightsReason,
                    airConditioningStatus = form.airConditioningStatus,
                    airConditioningReason = form.airConditioningReason,
                    photoFrontPath = form.photoFrontPath,
                    photoDriverSidePath = form.photoDriverSidePath,
                    photoPassengerSidePath = form.photoPassengerSidePath,
                    photoRearPath = form.photoRearPath,
                    completedDateFormatted = completedDateFormatted,
                    completedTimestampMillis = nowMillis,
                    pdfFilePath = null
                )

                // Save to Room DB
                val recordId = repository.saveChecklist(record)

                // Generate PDF
                val generatedPdfFile = PdfGenerator.generatePdf(
                    getApplication(),
                    record.copy(id = recordId)
                )

                // Update entity with PDF path
                val finalRecord = record.copy(
                    id = recordId,
                    pdfFilePath = generatedPdfFile.absolutePath
                )
                repository.updateChecklist(finalRecord)

                withContext(Dispatchers.Main) {
                    _lastCompletedRecord.value = finalRecord
                    _isProcessing.value = false
                    // Reset form state so previous form is not lingering
                    _formData.value = ChecklistFormData(
                        grandCommand = adminPreferences.getConfiguredGrandCommandName(),
                        unitName = adminPreferences.getConfiguredUnitName()
                    )
                    _currentScreen.value = AppScreen.REPORT_COMPLETED
                    onSuccess(finalRecord)
                }
                syncPendingReports(notify = true)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    showMessage("Erro ao finalizar checklist: ${e.message}")
                }
            }
        }
    }

    // --- REGENERATE PDF IF NEEDED ---
    fun ensurePdfFile(record: ChecklistRecordEntity): File {
        if (!record.pdfFilePath.isNullOrBlank()) {
            val file = File(record.pdfFilePath)
            if (file.exists()) return file
        }
        // Regenerate using the saved permanent completedDateFormatted without altering it
        val file = PdfGenerator.generatePdf(getApplication(), record)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChecklist(record.copy(pdfFilePath = file.absolutePath))
        }
        return file
    }

    // --- FIREBASE SYNC (envio para o painel de administradores) ---

    /**
     * Envia ao Firebase todos os checklists ainda não enviados.
     * Falhas (sem internet, etc.) não perdem nada: o relatório continua no aparelho
     * e é tentado de novo na próxima abertura do app ou no próximo checklist.
     */
    fun syncPendingReports(notify: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            syncMutex.withLock {
                val pending = repository.checklists.first()
                    .filter { !syncPreferences.isSynced(it.id) }
                _pendingSyncCount.value = pending.size
                if (pending.isEmpty()) return@withLock

                var sent = 0
                for (record in pending) {
                    val pdf = try { ensurePdfFile(record) } catch (e: Exception) { null }
                    val result = cloudStorageService.saveReport(record, pdf)
                    if (result.isSuccess) {
                        syncPreferences.markSynced(record.id)
                        sent++
                        _pendingSyncCount.value = pending.size - sent
                    } else {
                        // Provavelmente sem conexão: para aqui e tenta depois.
                        break
                    }
                }
                if (notify) {
                    val left = pending.size - sent
                    withContext(Dispatchers.Main) {
                        showMessage(
                            if (left == 0) "Relatório enviado à central com sucesso."
                            else "Sem conexão: o relatório está salvo e será enviado automaticamente."
                        )
                    }
                }
            }
        }
    }

    // --- GOOGLE DRIVE INTEGRATION ACTIONS ---

    fun refreshDriveConnectionStatus() {
        val connected = googleDriveProvider.isConnected()
        _isDriveConnected.value = connected
        _driveAccountEmail.value = if (connected) googleDriveProvider.getConnectedAccountEmail() else null
    }

    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>?) {
        if (task == null) {
            refreshDriveConnectionStatus()
            return
        }
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(GoogleDriveProvider.DRIVE_SCOPE))) {
                _isDriveConnected.value = true
                _driveAccountEmail.value = account.email
                showMessage("Google Drive conectado com sucesso: ${account.email}")
            } else {
                _isDriveConnected.value = false
                _driveAccountEmail.value = null
                showMessage("Permissão para o Google Drive não foi concedida.")
            }
        } catch (e: ApiException) {
            _isDriveConnected.value = false
            _driveAccountEmail.value = null
            showMessage("Erro ao conectar Google Drive (${e.statusCode}): ${e.localizedMessage}")
        }
    }

    fun disconnectGoogleDrive(client: GoogleSignInClient, onComplete: () -> Unit = {}) {
        client.signOut().addOnCompleteListener {
            _isDriveConnected.value = false
            _driveAccountEmail.value = null
            showMessage("Google Drive desconectado.")
            onComplete()
        }
    }

    fun uploadRecordToGoogleDrive(
        record: ChecklistRecordEntity,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!_isDriveConnected.value) {
            val msg = "Conecte uma conta Google Drive para enviar o relatório para a nuvem."
            showMessage(msg)
            onResult(false, msg)
            return
        }
        if (_isUploadingToDrive.value) return

        _isUploadingToDrive.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Utilizar exatamente o mesmo PDF já gerado localmente
                val pdfFile = ensurePdfFile(record)
                val uploadResult = googleDriveProvider.uploadReportPdf(record, pdfFile)
                withContext(Dispatchers.Main) {
                    _isUploadingToDrive.value = false
                    if (uploadResult.isSuccess) {
                        val successMsg = "Relatório enviado para o Google Drive com sucesso."
                        showMessage(successMsg)
                        onResult(true, successMsg)
                    } else {
                        val errorMsg = "Não foi possível enviar o relatório para o Google Drive. O relatório continua salvo no dispositivo."
                        showMessage(errorMsg)
                        onResult(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isUploadingToDrive.value = false
                    val errorMsg = "Não foi possível enviar o relatório para o Google Drive. O relatório continua salvo no dispositivo."
                    showMessage(errorMsg)
                    onResult(false, errorMsg)
                }
            }
        }
    }

    // --- ADMIN ACTIONS ---

    fun verifyAdminPassword(input: String): Boolean {
        return adminPreferences.checkPassword(input)
    }

    fun updateAdminPassword(newPassword: String): Boolean {
        if (newPassword.isBlank()) return false
        adminPreferences.setPassword(newPassword)
        return true
    }

    fun registerOfficer(
        rank: String,
        fullName: String,
        warName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (fullName.isBlank() || warName.isBlank()) {
            onError("Preencha todos os campos do policial.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.insertOfficer(
                OfficerEntity(
                    rank = rank,
                    fullName = fullName.trim(),
                    warName = warName.trim()
                )
            )
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    onSuccess()
                }.onFailure { err ->
                    onError(err.message ?: "Erro ao cadastrar policial.")
                }
            }
        }
    }

    fun registerVehicle(
        prefix: String,
        model: String,
        plate: String,
        unit: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (prefix.isBlank() || model.isBlank() || plate.isBlank() || unit.isBlank()) {
            onError("Preencha todos os campos da viatura.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertVehicle(
                    VehicleEntity(
                        prefix = prefix.trim(),
                        model = model.trim(),
                        plate = plate.trim().uppercase(),
                        linkedUnit = unit.trim()
                    )
                )
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Erro ao cadastrar viatura: ${e.message}")
                }
            }
        }
    }

    fun registerUnit(
        grandCommandCode: String,
        name: String,
        abbreviation: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (grandCommandCode.isBlank() || name.isBlank() || abbreviation.isBlank()) {
            onError("Preencha todos os campos da unidade.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertUnit(
                    PoliceUnitEntity(
                        grandCommandCode = grandCommandCode,
                        name = name.trim(),
                        abbreviation = abbreviation.trim()
                    )
                )
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Erro ao cadastrar unidade: ${e.message}")
                }
            }
        }
    }
}
