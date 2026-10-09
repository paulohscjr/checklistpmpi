package com.example.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.cloud.GoogleDriveProvider
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.PmpiBlue
import com.example.ui.theme.PmpiBlueContainer
import com.example.ui.theme.PmpiBlueDark
import com.example.ui.theme.PmpiGreen
import com.example.ui.theme.PmpiGreenContainer
import com.example.ui.theme.PmpiOutline
import com.example.ui.theme.PmpiRed
import com.example.ui.theme.PmpiSurfaceVariant
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val isDriveConnected by viewModel.isDriveConnected.collectAsState()
    val driveAccountEmail by viewModel.driveAccountEmail.collectAsState()

    val googleSignInClient = remember(context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GoogleDriveProvider.DRIVE_SCOPE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        viewModel.handleGoogleSignInResult(task)
    }

    var showPasswordDialogForScreen by remember { mutableStateOf<AppScreen?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Institutional Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PmpiBlue)
                .statusBarsPadding()
                .padding(top = 16.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Institutional Coat of Arms / Brasão PMPI (Preserving aspect ratio without white circle frame)
                Image(
                    painter = painterResource(id = R.drawable.ic_pmpi_brasao),
                    contentDescription = "Brasão da Polícia Militar do Estado do Piauí",
                    modifier = Modifier.size(108.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "POLÍCIA MILITAR DO ESTADO DO PIAUÍ",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "CHECKLIST DE VIATURA",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sistema de Inspeção e Controle Operacional",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Main Navigation Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "OPÇÕES PRINCIPAIS",
                color = PmpiBlueDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            // 1. NOVO CHECKLIST (No password required)
            MenuCommandCard(
                testTag = "menu_btn_novo_checklist",
                icon = Icons.Default.Assignment,
                title = "NOVO CHECKLIST",
                description = "Iniciar vistoria técnica e preenchimento de checklist da viatura",
                badgeText = "Acesso Rápido",
                badgeColor = PmpiBlue,
                onClick = {
                    viewModel.startNewChecklist()
                }
            )

            // 2. UNIDADE (Requires Admin password every access)
            MenuCommandCard(
                testTag = "menu_btn_unidade",
                icon = Icons.Default.Shield,
                title = "UNIDADE",
                description = "Estrutura administrativa e seleção do Grande Comando e Batalhão",
                badgeText = "Administrativo",
                badgeColor = Color(0xFF6A1B9A),
                onClick = {
                    passwordInput = ""
                    passwordError = false
                    showPasswordDialogForScreen = AppScreen.ADMIN_UNITS
                }
            )

            // 3. CHECKLISTS SALVOS (Requires Admin password every access)
            MenuCommandCard(
                testTag = "menu_btn_checklists_salvos",
                icon = Icons.Default.Folder,
                title = "CHECKLISTS SALVOS",
                description = "Consultar relatórios finalizados, visualizar e compartilhar PDFs",
                badgeText = "Histórico",
                badgeColor = Color(0xFF00695C),
                onClick = {
                    passwordInput = ""
                    passwordError = false
                    showPasswordDialogForScreen = AppScreen.SAVED_CHECKLISTS
                }
            )

            // 4. GOOGLE DRIVE (Armazenamento em Nuvem)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_google_drive"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDriveConnected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDriveConnected) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDriveConnected) PmpiGreenContainer else PmpiBlueContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = "Google Drive",
                                    tint = if (isDriveConnected) PmpiGreen else PmpiBlueDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GOOGLE DRIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isDriveConnected && !driveAccountEmail.isNullOrBlank()) {
                                        driveAccountEmail!!
                                    } else if (isDriveConnected) {
                                        "Conta autorizada"
                                    } else {
                                        "Sincronização em nuvem"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Badge de Status (🟢 Conectado / ⚪ Não conectado)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDriveConnected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDriveConnected) Color(0xFF86EFAC) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isDriveConnected) "🟢 Conectado" else "⚪ Não conectado",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDriveConnected) Color(0xFF166534) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isDriveConnected) {
                            "Estrutura organizada automaticamente no Google Drive: CHECKLIST VTR → [ANO] → [MÊS] → [DIA]."
                        } else {
                            "Conecte sua conta Google para enviar os PDFs dos relatórios diretamente para as pastas oficiais do Drive."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isDriveConnected) {
                        OutlinedButton(
                            onClick = {
                                viewModel.disconnectGoogleDrive(googleSignInClient)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_disconnect_google_drive"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PmpiRed
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESCONECTAR GOOGLE DRIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                driveSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_connect_google_drive"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PmpiBlue
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONECTAR GOOGLE DRIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Institutional Footer info
        Text(
            text = "PMPI • Segurança Pública em Defesa da Sociedade",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    // Modal de Senha Administrativa
    if (showPasswordDialogForScreen != null) {
        val targetScreen = showPasswordDialogForScreen!!
        AlertDialog(
            onDismissRequest = {
                showPasswordDialogForScreen = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = PmpiBlue
                    )
                    Text(
                        text = "SENHA ADMINISTRATIVA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PmpiBlue
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Para acessar a seção de ${if (targetScreen == AppScreen.ADMIN_UNITS) "UNIDADE" else "CHECKLISTS SALVOS"}, informe a senha administrativa:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = false
                        },
                        label = { Text("Senha") },
                        singleLine = true,
                        isError = passwordError,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Alternar visibilidade",
                                    tint = PmpiBlue
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    if (passwordError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Senha incorreta. Acesso negado.",
                            color = PmpiRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminPassword(passwordInput)) {
                            showPasswordDialogForScreen = null
                            onNavigate(targetScreen)
                        } else {
                            passwordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue),
                    modifier = Modifier.testTag("confirm_admin_password_btn")
                ) {
                    Text("Acessar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialogForScreen = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun MenuCommandCard(
    testTag: String,
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PmpiOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Institutional Blue Icon Container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PmpiBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PmpiBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = PmpiBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = Color(0xFF555F6D),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PmpiBlue.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
