package com.example.ui.screens.history

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistRecordEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.PmpiBlue
import com.example.ui.theme.PmpiBlueContainer
import com.example.ui.theme.PmpiBlueDark
import com.example.ui.theme.PmpiGreen
import com.example.ui.theme.PmpiGreenContainer
import com.example.ui.theme.PmpiOutline
import com.example.ui.theme.PmpiSurfaceVariant
import com.example.util.PdfOpener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedChecklistsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val checklists by viewModel.savedChecklists.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedRecordForDetails by remember { mutableStateOf<ChecklistRecordEntity?>(null) }

    val filtered = remember(searchQuery, checklists) {
        if (searchQuery.isBlank()) checklists
        else checklists.filter {
            it.reportNumber.contains(searchQuery, ignoreCase = true) ||
            it.vehicleModel.contains(searchQuery, ignoreCase = true) ||
            it.vehiclePlate.contains(searchQuery, ignoreCase = true) ||
            it.unitName.contains(searchQuery, ignoreCase = true) ||
            it.responsibleName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CHECKLISTS SALVOS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Histórico de Relatórios Operacionais",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PmpiBlue)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por VTR, placa, unidade ou responsável...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PmpiBlue) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_checklists_search_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Total de relatórios: ${filtered.size}",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum checklist encontrado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { record ->
                        ChecklistRecordCard(
                            record = record,
                            onViewDetails = { selectedRecordForDetails = record },
                            onOpenPdf = {
                                val pdfFile = viewModel.ensurePdfFile(record)
                                PdfOpener.viewPdf(context, pdfFile)
                            },
                            onSharePdf = {
                                val pdfFile = viewModel.ensurePdfFile(record)
                                PdfOpener.sharePdf(context, pdfFile)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal de Detalhes do Relatório
    if (selectedRecordForDetails != null) {
        val rec = selectedRecordForDetails!!
        AlertDialog(
            onDismissRequest = { selectedRecordForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PmpiBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detalhes do Relatório Nº ${rec.reportNumber}", fontWeight = FontWeight.Bold, color = PmpiBlue, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportDetailItem("Data de Verificação:", rec.verificationDate)
                    ReportDetailItem("Grande Comando:", rec.grandCommand)
                    ReportDetailItem("Unidade / Batalhão:", rec.unitName)
                    ReportDetailItem("Modelo da Viatura:", rec.vehicleModel)
                    ReportDetailItem("Placa da Viatura:", rec.vehiclePlate)
                    ReportDetailItem("Modalidade:", rec.serviceModality)
                    ReportDetailItem("Quilometragem Inicial:", "${rec.initialMileage} km")
                    ReportDetailItem("Responsável:", "${rec.responsibleRank} ${rec.responsibleName}")
                    ReportDetailItem("Comandante:", rec.commanderName.ifBlank { "Não informado" })
                    ReportDetailItem("Motorista:", rec.driverName.ifBlank { "Não informado" })
                    ReportDetailItem("Patrulheiro 01:", rec.patrolman01Name.ifBlank { "Não informado" })
                    ReportDetailItem("Patrulheiro 02:", rec.patrolman02Name.ifBlank { "Não informado" })
                    ReportDetailItem("Horário de Finalização:", rec.completedDateFormatted)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Condições Mecânicas:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PmpiBlueDark)
                    ReportDetailItem("Óleo:", rec.oilStatus)
                    ReportDetailItem("Água Radiador:", rec.radiatorWaterStatus)
                    ReportDetailItem("Pneus:", rec.tiresStatus)
                    ReportDetailItem("Giroflex:", rec.lightbarStatus)
                    ReportDetailItem("Rádio:", rec.radioStatus)
                    ReportDetailItem("Estepe:", rec.spareTireStatus)
                    ReportDetailItem("Macaco:", rec.jackStatus)
                    ReportDetailItem("Faróis:", rec.headlightsStatus)
                    ReportDetailItem("Ar-Condicionado:", rec.airConditioningStatus)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pdfFile = viewModel.ensurePdfFile(rec)
                        PdfOpener.viewPdf(context, pdfFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue)
                ) {
                    Text("Abrir PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecordForDetails = null }) {
                    Text("Fechar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ChecklistRecordCard(
    record: ChecklistRecordEntity,
    onViewDetails: () -> Unit,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checklist_card_${record.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PmpiOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header do Card: Número do Relatório + Data de Verificação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PmpiBlueContainer
                ) {
                    Text(
                        text = "RELATÓRIO Nº ${record.reportNumber}",
                        color = PmpiBlueDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = record.verificationDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Viatura e Unidade
            Text(
                text = if (record.vehiclePrefix.isNotBlank()) "${record.vehiclePrefix} • ${record.vehicleModel}" else record.vehicleModel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PmpiBlueDark
            )

            Text(
                text = "Placa: ${record.vehiclePlate} • Unidade: ${record.unitName}",
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Responsável
            Text(
                text = "Responsável: ${record.responsibleRank} ${record.responsibleName}",
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            // Data/Hora Finalização Permanente
            Text(
                text = "Finalizado em: ${record.completedDateFormatted}",
                fontSize = 11.sp,
                color = PmpiGreen,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ações: Visualizar Detalhes, Abrir PDF, Compartilhar PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_card_details_${record.id}"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Detalhes", fontSize = 11.sp, color = PmpiBlue)
                }

                Button(
                    onClick = onOpenPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = PmpiBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_card_open_pdf_${record.id}"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 11.sp)
                }

                Button(
                    onClick = onSharePdf,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_card_share_pdf_${record.id}"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enviar", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ReportDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
    }
}
