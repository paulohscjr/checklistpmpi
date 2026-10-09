package com.example.ui.screens.checklist

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
import com.example.util.PdfOpener
import java.io.File

@Composable
fun ReportCompletedScreen(
    viewModel: MainViewModel,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val record by viewModel.lastCompletedRecord.collectAsState()
    val isDriveConnected by viewModel.isDriveConnected.collectAsState()
    val isUploadingToDrive by viewModel.isUploadingToDrive.collectAsState()

    var showDriveNotConnectedDialog by remember { mutableStateOf(false) }
    var driveUploadSuccess by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var uploadStatusIsError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Success badge circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(PmpiGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PmpiGreen,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "RELATÓRIO FINALIZADO COM SUCESSO",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PmpiBlueDark,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            modifier = Modifier.testTag("report_success_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "O checklist foi gravado com segurança e o arquivo PDF oficial foi gerado.",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Report details summary card
        if (record != null) {
            val rec = record!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_details_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PmpiSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, PmpiOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(label = "Número do Relatório:", value = "Nº ${rec.reportNumber}")
                    DetailRow(label = "Viatura:", value = "${rec.vehiclePrefix} — ${rec.vehicleModel}")
                    DetailRow(label = "Placa:", value = rec.vehiclePlate)
                    DetailRow(label = "Unidade / Batalhão:", value = rec.unitName)
                    DetailRow(label = "Responsável:", value = "${rec.responsibleRank} ${rec.responsibleName}")
                    DetailRow(label = "Data e Hora da Finalização:", value = rec.completedDateFormatted, isHighlighted = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4 Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. COMPARTILHAR
            Button(
                onClick = {
                    record?.let {
                        val pdfFile = viewModel.ensurePdfFile(it)
                        PdfOpener.sharePdf(context, pdfFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_share_pdf"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("COMPARTILHAR PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // 2. VISUALIZAR PDF
            OutlinedButton(
                onClick = {
                    record?.let {
                        val pdfFile = viewModel.ensurePdfFile(it)
                        PdfOpener.viewPdf(context, pdfFile)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_view_pdf"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PmpiBlue)
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = PmpiBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Text("VISUALIZAR PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PmpiBlue)
            }

            // 3. ENVIAR PARA NUVEM (Google Drive)
            OutlinedButton(
                onClick = {
                    if (isUploadingToDrive) return@OutlinedButton
                    if (!isDriveConnected) {
                        showDriveNotConnectedDialog = true
                    } else {
                        record?.let { rec ->
                            uploadStatusMessage = null
                            viewModel.uploadRecordToGoogleDrive(rec) { success, msg ->
                                uploadStatusMessage = msg
                                uploadStatusIsError = !success
                                if (success) {
                                    driveUploadSuccess = true
                                }
                            }
                        }
                    }
                },
                enabled = !isUploadingToDrive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_send_cloud"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (driveUploadSuccess) PmpiGreen else if (isDriveConnected) PmpiBlue else Color(0xFF64748B)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (driveUploadSuccess) Color(0xFFF0FDF4) else Color.Transparent
                )
            ) {
                if (isUploadingToDrive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PmpiBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enviando relatório para o Google Drive...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PmpiBlueDark
                    )
                } else if (driveUploadSuccess) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = PmpiGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ENVIADO COM SUCESSO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PmpiGreen
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (isDriveConnected) PmpiBlue else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ENVIAR PARA NUVEM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDriveConnected) PmpiBlueDark else Color(0xFF334155)
                    )
                }
            }

            // Banner de Feedback do Upload
            if (uploadStatusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uploadStatusIsError) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uploadStatusIsError) Color(0xFFFECACA) else Color(0xFFBBF7D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uploadStatusIsError) Icons.Default.Info else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (uploadStatusIsError) PmpiRed else PmpiGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uploadStatusMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uploadStatusIsError) Color(0xFF991B1B) else Color(0xFF166534),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 4. VOLTAR (Tela Inicial)
            TextButton(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_back_home")
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = PmpiBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VOLTAR À TELA INICIAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PmpiBlue)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal quando o Google Drive não está conectado
    if (showDriveNotConnectedDialog) {
        AlertDialog(
            onDismissRequest = { showDriveNotConnectedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Google Drive Não Conectado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PmpiBlueDark
                )
            },
            text = {
                Text(
                    text = "Conecte uma conta Google Drive para enviar o relatório para a nuvem.",
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveNotConnectedDialog = false
                        onNavigateHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue)
                ) {
                    Text("CONECTAR NA TELA INICIAL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveNotConnectedDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlighted) PmpiBlueDark else Color(0xFF0F172A),
            textAlign = TextAlign.End
        )
    }
}
