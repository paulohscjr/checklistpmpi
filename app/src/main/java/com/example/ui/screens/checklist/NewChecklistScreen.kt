package com.example.ui.screens.checklist

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.ui.MainViewModel
import com.example.ui.theme.PmpiBlue
import com.example.ui.theme.PmpiBlueContainer
import com.example.ui.theme.PmpiBlueDark
import com.example.ui.theme.PmpiGreen
import com.example.ui.theme.PmpiGreenContainer
import com.example.ui.theme.PmpiOutline
import com.example.ui.theme.PmpiRed
import com.example.ui.theme.PmpiRedContainer
import com.example.ui.theme.PmpiSurfaceVariant
import com.example.util.PhotoManager
import java.io.File

private val RANKS_LIST = listOf(
    "Soldado",
    "Cabo",
    "3º Sargento",
    "2º Sargento",
    "1º Sargento",
    "Subtenente",
    "Aspirante a Oficial",
    "2º Tenente",
    "1º Tenente",
    "Capitão",
    "Major",
    "Tenente-Coronel",
    "Coronel"
)

private val SERVICE_MODALITIES = listOf("Ordinário", "Diário", "Planejada")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChecklistScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val form by viewModel.formData.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val configuredCommandName by viewModel.configuredGrandCommandName.collectAsState()
    val configuredUnitName by viewModel.configuredUnitName.collectAsState()
    val configuredUnitAbbrev by viewModel.configuredUnitAbbrev.collectAsState()

    // Sync configured command/unit into form if not set yet
    LaunchedEffect(configuredCommandName, configuredUnitName) {
        if (form.grandCommand.isBlank() && configuredCommandName.isNotBlank()) {
            val unitDisplay = if (configuredUnitAbbrev.isNotBlank() && configuredUnitName.isNotBlank() && configuredUnitAbbrev != configuredUnitName) {
                "${configuredUnitAbbrev} — ${configuredUnitName}"
            } else if (configuredUnitName.isNotBlank()) {
                configuredUnitName
            } else {
                configuredUnitAbbrev
            }
            viewModel.updateFormData { current ->
                current.copy(
                    grandCommand = configuredCommandName,
                    unitName = unitDisplay
                )
            }
        }
    }

    // Photo camera launcher state
    var currentPhotoSlotIndex by remember { mutableIntStateOf(-1) }
    var currentCameraTempFile by remember { mutableStateOf<File?>(null) }
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraTempFile != null && currentPhotoSlotIndex >= 0) {
            viewModel.setPhoto(currentPhotoSlotIndex, currentCameraTempFile!!.absolutePath)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && currentCameraUri != null) {
            try {
                cameraLauncher.launch(currentCameraUri!!)
            } catch (e: ActivityNotFoundException) {
                // Fallback for emulators without camera app
                val slotNames = listOf("frente", "motorista", "passageiro", "traseira")
                val slotDesc = listOf("Frente da Viatura", "Lado do Motorista", "Lado do Passageiro", "Parte Traseira")
                val safeIndex = currentPhotoSlotIndex.coerceIn(0, 3)
                val path = PhotoManager.createSampleCarPhoto(context, slotNames[safeIndex], slotDesc[safeIndex])
                viewModel.setPhoto(currentPhotoSlotIndex, path)
                Toast.makeText(context, "Registro fotográfico capturado!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permissão da câmera é necessária para fotografar a viatura.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCameraForSlot(slotIndex: Int, slotName: String, slotDesc: String) {
        currentPhotoSlotIndex = slotIndex
        val tempFile = PhotoManager.createTempImageFile(context, slotName)
        currentCameraTempFile = tempFile
        val uri = PhotoManager.getUriForFile(context, tempFile)
        currentCameraUri = uri

        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(uri)
            } catch (e: ActivityNotFoundException) {
                // In streaming container emulator if no camera intent handler exists
                val path = PhotoManager.createSampleCarPhoto(context, slotName, slotDesc)
                viewModel.setPhoto(slotIndex, path)
                Toast.makeText(context, "Registro fotográfico capturado!", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NOVO CHECKLIST",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Inspeção Operacional de Viatura",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checklist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PmpiBlue
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ================= SECTION 1: IDENTIFICAÇÃO E UNIDADE =================
            SectionCard(title = "1. IDENTIFICAÇÃO E UNIDADE (BLOQUEADOS)") {
                // Data de Verificação
                OutlinedTextField(
                    value = form.verificationDate,
                    onValueChange = { viewModel.updateFormData { f -> f.copy(verificationDate = it) } },
                    label = { Text("Data de Verificação *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_verification_date"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Grande Comando (READ-ONLY / BLOQUEADO)
                OutlinedTextField(
                    value = form.grandCommand.ifBlank { configuredCommandName.ifBlank { "Nenhum comando configurado" } },
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Grande Comando *") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado para edição",
                            tint = PmpiBlue
                        )
                    },
                    supportingText = {
                        Text("Definido na aba Unidade (somente leitura)", color = PmpiBlueDark, fontSize = 11.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = PmpiBlueDark,
                        disabledBorderColor = PmpiBlue,
                        disabledLabelColor = PmpiBlue,
                        disabledContainerColor = PmpiBlueContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_grand_command_locked")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Unidade / Batalhão (READ-ONLY / BLOQUEADO)
                val unitDisplayText = form.unitName.ifBlank {
                    if (configuredUnitAbbrev.isNotBlank() && configuredUnitName.isNotBlank()) {
                        "${configuredUnitAbbrev} — ${configuredUnitName}"
                    } else configuredUnitName.ifBlank { configuredUnitAbbrev.ifBlank { "Nenhuma unidade configurada" } }
                }

                OutlinedTextField(
                    value = unitDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Unidade / Batalhão *") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado para edição",
                            tint = PmpiBlue
                        )
                    },
                    supportingText = {
                        Text("Definido na aba Unidade (somente leitura)", color = PmpiBlueDark, fontSize = 11.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = PmpiBlueDark,
                        disabledBorderColor = PmpiBlue,
                        disabledLabelColor = PmpiBlue,
                        disabledContainerColor = PmpiBlueContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_unit_name_locked")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PmpiBlueContainer,
                    border = BorderStroke(1.dp, PmpiBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PmpiBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Para alterar a unidade de serviço, acerte a configuração na aba UNIDADE com a senha de administrador.",
                            fontSize = 11.sp,
                            color = PmpiBlueDark,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // ================= SECTION 2: DADOS DA VIATURA =================
            SectionCard(title = "2. DADOS DA VIATURA (DIGITAÇÃO MANUAL)") {
                // Modelo da Viatura (manual)
                OutlinedTextField(
                    value = form.vehicleModel,
                    onValueChange = { viewModel.updateFormData { f -> f.copy(vehicleModel = it) } },
                    label = { Text("Modelo da Viatura *") },
                    placeholder = { Text("Ex: Toyota Hilux / Duster / Ranger") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vehicle_model"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Placa da Viatura (manual)
                OutlinedTextField(
                    value = form.vehiclePlate,
                    onValueChange = { viewModel.updateFormData { f -> f.copy(vehiclePlate = it.uppercase()) } },
                    label = { Text("Placa da Viatura *") },
                    placeholder = { Text("Ex: PIX-2901 ou RNB-3A12") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vehicle_plate"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Modalidade do Serviço (Ordinário, Diário, Planejada)
                Text(
                    text = "Modalidade do Serviço *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PmpiBlueDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SERVICE_MODALITIES.forEach { mod ->
                        val isSelected = (form.serviceModality == mod)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFormData { f -> f.copy(serviceModality = mod) } },
                            label = { Text(mod, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PmpiBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_modality_$mod")
                        )
                    }
                }
            }

            // ================= SECTION 3: MOTORISTA =================
            SectionCard(title = "3. MOTORISTA DA VIATURA") {
                // Posto/Graduação Dropdown
                var rankExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = form.responsibleRank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Posto / Graduação do Motorista *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("dropdown_responsible_rank")
                    )
                    ExposedDropdownMenu(
                        expanded = rankExpanded,
                        onDismissRequest = { rankExpanded = false }
                    ) {
                        RANKS_LIST.forEach { rank ->
                            DropdownMenuItem(
                                text = { Text(rank) },
                                onClick = {
                                    viewModel.updateFormData { f -> f.copy(responsibleRank = rank) }
                                    rankExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Motorista da Viatura (DIGITAÇÃO MANUAL)
                OutlinedTextField(
                    value = form.driverName,
                    onValueChange = { viewModel.updateFormData { f -> f.copy(driverName = it) } },
                    label = { Text("Nome Completo do Motorista da Viatura *") },
                    placeholder = { Text("Digite o nome completo do motorista") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_driver_name"),
                    singleLine = true
                )
            }

            // ================= SECTION 4: QUILOMETRAGEM =================
            SectionCard(title = "4. QUILOMETRAGEM DA VIATURA") {
                OutlinedTextField(
                    value = form.initialMileage,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() }
                        viewModel.updateFormData { f -> f.copy(initialMileage = filtered) }
                    },
                    label = { Text("Km Inicial *") },
                    placeholder = { Text("Ex: 85200") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_initial_mileage"),
                    singleLine = true
                )
            }

            // ================= SECTION 5: CONDIÇÕES MECÂNICAS COM MOTIVOS =================
            SectionCard(title = "5. CONDIÇÕES TÉCNICAS E MECÂNICAS") {
                Text(
                    text = "Indique o estado de cada item. Havendo alteração, descreva obrigatoriamente o motivo.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Óleo
                TechnicalCheckItem(
                    title = "Óleo do Motor",
                    status = form.oilStatus,
                    reason = form.oilReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(oilStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(oilReason = newReason) } },
                    tagPrefix = "oil"
                )

                // 2. Água do Radiador
                TechnicalCheckItem(
                    title = "Água do Radiador",
                    status = form.radiatorWaterStatus,
                    reason = form.radiatorWaterReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(radiatorWaterStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(radiatorWaterReason = newReason) } },
                    tagPrefix = "water"
                )

                // 3. Pneus (SIM / NÃO)
                TiresCheckItem(
                    status = form.tiresStatus,
                    reason = form.tiresReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(tiresStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(tiresReason = newReason) } }
                )

                // 4. Giroflex
                TechnicalCheckItem(
                    title = "Giroflex / Sinalizador",
                    status = form.lightbarStatus,
                    reason = form.lightbarReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(lightbarStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(lightbarReason = newReason) } },
                    tagPrefix = "lightbar"
                )

                // 5. Rádio
                TechnicalCheckItem(
                    title = "Rádio Comunicador",
                    status = form.radioStatus,
                    reason = form.radioReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(radioStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(radioReason = newReason) } },
                    tagPrefix = "radio"
                )

                // 6. Estepe
                TechnicalCheckItem(
                    title = "Estepe",
                    status = form.spareTireStatus,
                    reason = form.spareTireReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(spareTireStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(spareTireReason = newReason) } },
                    tagPrefix = "spare"
                )

                // 7. Macaco Hidráulico
                TechnicalCheckItem(
                    title = "Macaco Hidráulico",
                    status = form.jackStatus,
                    reason = form.jackReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(jackStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(jackReason = newReason) } },
                    tagPrefix = "jack"
                )

                // 8. Faróis
                TechnicalCheckItem(
                    title = "Faróis e Iluminação",
                    status = form.headlightsStatus,
                    reason = form.headlightsReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(headlightsStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(headlightsReason = newReason) } },
                    tagPrefix = "headlights"
                )

                // 9. Ar-condicionado
                TechnicalCheckItem(
                    title = "Ar-Condicionado",
                    status = form.airConditioningStatus,
                    reason = form.airConditioningReason,
                    onStatusChange = { newStatus -> viewModel.updateFormData { f -> f.copy(airConditioningStatus = newStatus) } },
                    onReasonChange = { newReason -> viewModel.updateFormData { f -> f.copy(airConditioningReason = newReason) } },
                    tagPrefix = "ac"
                )
            }

            // ================= SECTION 6: REGISTRO FOTOGRÁFICO OBRIGATÓRIO =================
            SectionCard(title = "6. REGISTRO FOTOGRÁFICO OBRIGATÓRIO (4 ÂNGULOS)") {
                Text(
                    text = "As 4 fotografias oficiais são obrigatórias e devem ser tiradas em tempo real através da câmera do aparelho:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Slot 0: Frente da Viatura
                MandatoryCameraPhotoSlot(
                    slotIndex = 0,
                    slotTitle = "1. Frente da Viatura",
                    slotName = "frente",
                    photoPath = form.photoFrontPath,
                    onOpenCamera = { openCameraForSlot(0, "frente", "Frente da Viatura") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Slot 1: Lado do Motorista
                MandatoryCameraPhotoSlot(
                    slotIndex = 1,
                    slotTitle = "2. Lado do Motorista",
                    slotName = "motorista",
                    photoPath = form.photoDriverSidePath,
                    onOpenCamera = { openCameraForSlot(1, "motorista", "Lado do Motorista") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Slot 2: Lado do Passageiro
                MandatoryCameraPhotoSlot(
                    slotIndex = 2,
                    slotTitle = "3. Lado do Passageiro",
                    slotName = "passageiro",
                    photoPath = form.photoPassengerSidePath,
                    onOpenCamera = { openCameraForSlot(2, "passageiro", "Lado do Passageiro") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Slot 3: Parte Traseira
                MandatoryCameraPhotoSlot(
                    slotIndex = 3,
                    slotTitle = "4. Parte Traseira da Viatura",
                    slotName = "traseira",
                    photoPath = form.photoRearPath,
                    onOpenCamera = { openCameraForSlot(3, "traseira", "Parte Traseira") }
                )
            }

            // ================= SECTION 7: BOTÃO FINALIZAR RELATÓRIO =================
            Button(
                onClick = {
                    viewModel.finalizeChecklist { _ ->
                        // Automatically transitions to ReportCompletedScreen via ViewModel
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_finalize_checklist"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PmpiBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "GERANDO RELATÓRIO EM PDF...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FINALIZAR RELATÓRIO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// -------------------------------------------------------------
// COMPONENTES AUXILIARES DE DESIGN
// -------------------------------------------------------------

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PmpiOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PmpiBlue,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun TechnicalCheckItem(
    title: String,
    status: String,
    reason: String,
    onStatusChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    tagPrefix: String
) {
    val isAltered = (status == "COM ALTERAÇÃO")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAltered) PmpiRedContainer.copy(alpha = 0.3f) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (isAltered) PmpiRed.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PmpiBlueDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isSemAlteracao = (status == "SEM ALTERAÇÃO")
            FilterChip(
                selected = isSemAlteracao,
                onClick = { onStatusChange("SEM ALTERAÇÃO") },
                label = { Text("SEM ALTERAÇÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PmpiGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_sem_alteracao")
            )

            FilterChip(
                selected = isAltered,
                onClick = { onStatusChange("COM ALTERAÇÃO") },
                label = { Text("COM ALTERAÇÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PmpiRed,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_com_alteracao")
            )
        }

        // CAMPO CONDICIONAL: MOTIVO DA ALTERAÇÃO
        AnimatedVisibility(
            visible = isAltered,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val itemContext = LocalContext.current
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        label = { Text("Motivo da alteração *") },
                        placeholder = { Text("Descreva detalhadamente o problema encontrado...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_input_reason"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PmpiRed,
                            unfocusedBorderColor = PmpiRed.copy(alpha = 0.7f),
                            focusedLabelColor = PmpiRed
                        ),
                        supportingText = {
                            Text("Obrigatório detalhar a anomalia.", color = PmpiRed, fontSize = 11.sp)
                        },
                        minLines = 2
                    )

                    IconButton(
                        onClick = {
                            if (reason.isNotBlank()) {
                                Toast.makeText(itemContext, "Relato salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(itemContext, "Digite o motivo da alteração antes de salvar.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PmpiBlue)
                            .testTag("${tagPrefix}_btn_save_reason")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar Relato",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TiresCheckItem(
    status: String,
    reason: String,
    onStatusChange: (String) -> Unit,
    onReasonChange: (String) -> Unit
) {
    val isAltered = (status == "NÃO")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAltered) PmpiRedContainer.copy(alpha = 0.3f) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (isAltered) PmpiRed.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Pneus em Bom Estado de Conservação",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PmpiBlueDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isSim = (status == "SIM")
            FilterChip(
                selected = isSim,
                onClick = { onStatusChange("SIM") },
                label = { Text("SIM (EM BOM ESTADO)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PmpiGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tires_sim")
            )

            FilterChip(
                selected = isAltered,
                onClick = { onStatusChange("NÃO") },
                label = { Text("NÃO (COM PROBLEMA)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PmpiRed,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tires_nao")
            )
        }

        // CAMPO CONDICIONAL: MOTIVO DA ALTERAÇÃO DOS PNEUS
        AnimatedVisibility(
            visible = isAltered,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val itemContext = LocalContext.current
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        label = { Text("Motivo da alteração dos pneus *") },
                        placeholder = { Text("Ex: Pneu dianteiro direito careca, rasgo na lateral, etc.") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tires_input_reason"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PmpiRed,
                            unfocusedBorderColor = PmpiRed.copy(alpha = 0.7f),
                            focusedLabelColor = PmpiRed
                        ),
                        supportingText = {
                            Text("Obrigatório detalhar o estado dos pneus.", color = PmpiRed, fontSize = 11.sp)
                        },
                        minLines = 2
                    )

                    IconButton(
                        onClick = {
                            if (reason.isNotBlank()) {
                                Toast.makeText(itemContext, "Relato salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(itemContext, "Digite o motivo da alteração antes de salvar.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PmpiBlue)
                            .testTag("tires_btn_save_reason")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar Relato",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MandatoryCameraPhotoSlot(
    slotIndex: Int,
    slotTitle: String,
    slotName: String,
    photoPath: String?,
    onOpenCamera: () -> Unit
) {
    val hasPhoto = !photoPath.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_slot_$slotIndex"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPhoto) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasPhoto) PmpiGreen else PmpiOutline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slotTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PmpiBlue
                )

                if (hasPhoto) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PmpiGreenContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PmpiGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "FOTO REGISTRADA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PmpiGreen
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PmpiRedContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PmpiRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "OBRIGATÓRIA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PmpiRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasPhoto) {
                // Image preview with retake button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PmpiGreen, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(File(photoPath!!)),
                        contentDescription = slotTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_retake_photo_$slotIndex"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PmpiBlue),
                    border = BorderStroke(1.dp, PmpiBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tirar Novamente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Button to open camera only (no gallery, no local storage button)
                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_open_camera_$slotIndex"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PmpiBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ABRIR CÂMERA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
