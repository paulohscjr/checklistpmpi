package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrandCommandEntity
import com.example.data.model.PoliceUnitEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.PmpiBlue
import com.example.ui.theme.PmpiBlueContainer
import com.example.ui.theme.PmpiBlueDark
import com.example.ui.theme.PmpiGreen
import com.example.ui.theme.PmpiGreenContainer
import com.example.ui.theme.PmpiOutline
import com.example.ui.theme.PmpiSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminUnitScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val grandCommands by viewModel.grandCommands.collectAsState()
    val policeUnits by viewModel.policeUnits.collectAsState()

    val configuredCommandName by viewModel.configuredGrandCommandName.collectAsState()
    val configuredUnitName by viewModel.configuredUnitName.collectAsState()
    val configuredUnitAbbrev by viewModel.configuredUnitAbbrev.collectAsState()

    // Default selected command in view: find match or default to first
    var selectedCommandCode by remember(grandCommands, configuredCommandName) {
        val matched = grandCommands.find { it.name.equals(configuredCommandName, ignoreCase = true) }
        mutableStateOf(matched?.code ?: "CPM")
    }

    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "UNIDADE OPERACIONAL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Grandes Comandos & Batalhões",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPasswordChangeDialog = true },
                        modifier = Modifier.testTag("admin_change_password_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Alterar Senha Administrativa",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PmpiBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddUnitDialog = true },
                containerColor = PmpiBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_unit")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Batalhão / Unidade"
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BANNER: UNIDADE ATUALMENTE CONFIGURADA
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_active_configured_unit"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PmpiBlueContainer),
                    border = BorderStroke(1.5.dp, PmpiBlue)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PmpiBlue,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "UNIDADE ATUALMENTE CONFIGURADA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PmpiBlue,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = configuredUnitAbbrev.ifBlank { "NÃO DEFINIDA" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PmpiBlueDark
                        )

                        Text(
                            text = configuredUnitName.ifBlank { "Selecione uma unidade abaixo" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Grande Comando:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = configuredCommandName.ifBlank { "Não definido" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PmpiBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PmpiGreenContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PmpiGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ativa para o Novo Checklist (preenchimento automático)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PmpiGreen
                                )
                            }
                        }
                    }
                }
            }

            // 2. SEÇÃO: GRANDES COMANDOS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. SELECIONE O GRANDE COMANDO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PmpiBlue,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Toque em um Grande Comando para visualizar os seus respectivos Batalhões e Companhias:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        grandCommands.forEach { cmd ->
                            val isSelected = (cmd.code == selectedCommandCode)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCommandCode = cmd.code
                                },
                                label = {
                                    Text(
                                        text = cmd.code,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PmpiBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = PmpiBlueDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) PmpiBlue else PmpiOutline
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("chip_cmd_${cmd.code}")
                            )
                        }
                    }
                }
            }

            // 3. SEÇÃO: HIERARQUIA & LISTAGEM DAS UNIDADES
            val currentSelectedCommand = grandCommands.find { it.code == selectedCommandCode }
            val unitsForCommand = policeUnits.filter { it.grandCommandCode == selectedCommandCode }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Breadcrumb da Hierarquia
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, PmpiOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CorporateFare,
                                contentDescription = null,
                                tint = PmpiBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "UNIDADE → ${selectedCommandCode}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PmpiBlue
                                )
                                Text(
                                    text = currentSelectedCommand?.name ?: selectedCommandCode,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "2. SELECIONE A UNIDADE / BATALHÃO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PmpiBlue,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Toque na unidade desejada para defini-la como a unidade de serviço do aplicativo:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (unitsForCommand.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PmpiSurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma unidade cadastrada para este Grande Comando.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(unitsForCommand) { unit ->
                    val isCurrentlyConfigured =
                        (unit.abbreviation.equals(configuredUnitAbbrev, ignoreCase = true) ||
                                unit.name.equals(configuredUnitName, ignoreCase = true))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unit_item_${unit.abbreviation}")
                            .clickable {
                                val cmdName = currentSelectedCommand?.name ?: selectedCommandCode
                                viewModel.setConfiguredUnit(
                                    cmdCode = selectedCommandCode,
                                    cmdName = cmdName,
                                    unitAbbrev = unit.abbreviation,
                                    unitName = unit.name
                                )
                                Toast.makeText(
                                    context,
                                    "Unidade configurada: ${unit.abbreviation}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentlyConfigured) PmpiBlueContainer else Color.White
                        ),
                        border = BorderStroke(
                            width = if (isCurrentlyConfigured) 2.dp else 1.dp,
                            color = if (isCurrentlyConfigured) PmpiBlue else PmpiOutline
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyConfigured) 2.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = unit.abbreviation,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PmpiBlue
                                    )

                                    if (isCurrentlyConfigured) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PmpiGreenContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = PmpiGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "SELECIONADA",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = PmpiGreen
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = unit.name,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 17.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    val cmdName = currentSelectedCommand?.name ?: selectedCommandCode
                                    viewModel.setConfiguredUnit(
                                        cmdCode = selectedCommandCode,
                                        cmdName = cmdName,
                                        unitAbbrev = unit.abbreviation,
                                        unitName = unit.name
                                    )
                                    Toast.makeText(
                                        context,
                                        "Unidade configurada: ${unit.abbreviation}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentlyConfigured) PmpiGreen else PmpiBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_select_unit_${unit.abbreviation}")
                            ) {
                                Text(
                                    text = if (isCurrentlyConfigured) "Ativa" else "Selecionar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // --- MODAL: ALTERAR SENHA ADMINISTRATIVA ---
    if (showPasswordChangeDialog) {
        var currentPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showPass by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PmpiBlue
                    )
                    Text(
                        text = "ALTERAR SENHA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PmpiBlue
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Informe a senha atual e defina a nova senha de acesso administrativo:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it; errorMessage = null },
                        label = { Text("Senha Atual") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_current_password")
                    )

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it; errorMessage = null },
                        label = { Text("Nova Senha") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                    )

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it; errorMessage = null },
                        label = { Text("Confirmar Nova Senha") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_confirm_new_password")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showPass = !showPass }
                    ) {
                        IconButton(onClick = { showPass = !showPass }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (showPass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = PmpiBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mostrar caracteres",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!viewModel.verifyAdminPassword(currentPassInput)) {
                            errorMessage = "Senha atual incorreta."
                            return@Button
                        }
                        if (newPassInput.isBlank()) {
                            errorMessage = "A nova senha não pode estar em branco."
                            return@Button
                        }
                        if (newPassInput != confirmPassInput) {
                            errorMessage = "A confirmação da senha não coincide."
                            return@Button
                        }
                        viewModel.updateAdminPassword(newPassInput)
                        Toast.makeText(context, "Senha administrativa alterada com sucesso!", Toast.LENGTH_SHORT).show()
                        showPasswordChangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue),
                    modifier = Modifier.testTag("btn_confirm_change_password")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordChangeDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // --- MODAL: ADICIONAR NOVA UNIDADE AO COMANDO SELECIONADO ---
    if (showAddUnitDialog) {
        val currentSelectedCommand = grandCommands.find { it.code == selectedCommandCode }
        var unitNameInput by remember { mutableStateOf("") }
        var unitAbbrevInput by remember { mutableStateOf("") }
        var addUnitError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = {
                Text(
                    text = "NOVA UNIDADE / BATALHÃO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PmpiBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Vincular ao comando: ${currentSelectedCommand?.name ?: selectedCommandCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PmpiBlue
                    )

                    OutlinedTextField(
                        value = unitAbbrevInput,
                        onValueChange = { unitAbbrevInput = it; addUnitError = null },
                        label = { Text("Sigla da Unidade (ex: 31º BPM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_unit_abbrev")
                    )

                    OutlinedTextField(
                        value = unitNameInput,
                        onValueChange = { unitNameInput = it; addUnitError = null },
                        label = { Text("Nome Completo (ex: 31º Batalhão)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_unit_name")
                    )

                    if (addUnitError != null) {
                        Text(
                            text = addUnitError!!,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (unitAbbrevInput.isBlank() || unitNameInput.isBlank()) {
                            addUnitError = "Preencha todos os campos."
                            return@Button
                        }
                        viewModel.registerUnit(
                            grandCommandCode = selectedCommandCode,
                            name = unitNameInput,
                            abbreviation = unitAbbrevInput,
                            onSuccess = {
                                Toast.makeText(context, "Unidade adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                                showAddUnitDialog = false
                            },
                            onError = { err ->
                                addUnitError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue),
                    modifier = Modifier.testTag("btn_save_new_unit")
                ) {
                    Text("Cadastrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}
