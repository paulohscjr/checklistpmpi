package com.example.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.model.ChecklistRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Gerador de PDF institucional para o relatório de Checklist de Viatura da PMPI.
 *
 * Estrutura e padrão visual alinhados ao modelo executivo de relatórios oficiais da PMPI:
 * - Cabeçalho com faixa azul petróleo (#0F3A53), friso dourado (#C9A227) e brasão oficial em alta resolução (1024x1024 / vetorial renderizado com filtro bilinear).
 * - Identificação em quadro executivo (#FAFCFE) com bordas suaves e tipografia hierárquica.
 * - Barras de seção com friso azul petróleo e fundo azul claro (#F0F5F9).
 * - Tabela de itens verificados com badge de conformidade (verde para SEM ALTERAÇÃO / SIM e vermelho para COM ALTERAÇÃO / NÃO com detalhamento do motivo).
 * - Bloco executivo de fechamento e assinatura com detalhe dourado.
 * - Grade fotográfica 2x2 com molduras padronizadas, proporção preservada (aspect fit) e legendas destacadas.
 * - Marca-d'água centralizada rotacionada (-32º) e marca-d'água de autenticidade no rodapé em todas as páginas.
 * - Nomenclatura oficial: Relatorio_Parte_[NUMERO]_[DATA]_[NOME_DO_ELABORADOR].pdf.
 */
object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_BOTTOM = 50f

    // Institutional Colors (Paleta Executiva PMPI)
    private val COLOR_AZUL_PETROLEO = Color.rgb(15, 58, 83)      // #0F3A53
    private val COLOR_AZUL_ESCURO = Color.rgb(10, 37, 53)        // #0A2535
    private val COLOR_DOURADO = Color.rgb(201, 162, 39)          // #C9A227
    private val COLOR_DOURADO_CLARO = Color.rgb(245, 238, 206)   // #F5EECE
    private val COLOR_AZUL_CLARO = Color.rgb(240, 245, 249)      // #F0F5F9
    private val COLOR_AZUL_DESTAQUE = Color.rgb(222, 234, 244)   // #DEEAF4
    private val COLOR_CARD_BG = Color.rgb(250, 252, 254)         // #FAFCFE
    private val COLOR_TEXTO = Color.rgb(15, 23, 42)              // #0F172A
    private val COLOR_TEXTO_SEC = Color.rgb(71, 85, 105)         // #475569
    private val COLOR_TEXTO_MUTED = Color.rgb(148, 163, 184)     // #94A3B8
    private val COLOR_BORDA = Color.rgb(203, 213, 225)           // #CBD5E1
    private val COLOR_BORDA_SUAVE = Color.rgb(226, 232, 240)     // #E2E8F0
    private val COLOR_SUCCESS = Color.rgb(27, 115, 57)           // #1B7339
    private val COLOR_SUCCESS_BG = Color.rgb(230, 244, 234)      // #E6F4EA
    private val COLOR_ALERT = Color.rgb(179, 38, 30)             // #B3261E
    private val COLOR_ALERT_BG = Color.rgb(252, 232, 230)        // #FCE8E6

    private fun removeAccents(str: String): String {
        val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "")
    }

    private fun sanitizeString(str: String): String {
        val withoutAccents = removeAccents(str)
        val withUnderscores = withoutAccents.replace(Regex("[^a-zA-Z0-9]+"), "_")
        return withUnderscores.trim('_')
    }

    fun formatNumeroParte(parteNumero: String): String {
        val trimmed = parteNumero.trim()
        if (trimmed.isEmpty()) return "001"

        if (trimmed.all { it.isDigit() }) {
            return trimmed.padStart(3, '0')
        }

        val match = Regex("^(\\d+)[/\\\\-].*").find(trimmed)
        if (match != null) {
            val num = match.groupValues[1]
            return num.padStart(3, '0')
        }

        val sanitized = sanitizeString(trimmed)
        return sanitized.ifBlank { "001" }
    }

    fun formatDataServico(dataServico: String, fallbackTimestamp: Long = System.currentTimeMillis()): String {
        val trimmed = dataServico.trim()
        if (trimmed.isNotBlank()) {
            val digits = trimmed.filter { it.isDigit() }
            if (trimmed.matches(Regex("^\\d{4}[-/.]\\d{2}[-/.]\\d{2}.*")) && digits.length >= 8) {
                val yyyy = digits.substring(0, 4)
                val mm = digits.substring(4, 6)
                val dd = digits.substring(6, 8)
                return "$dd$mm$yyyy"
            }
            val dmyMatch = Regex("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})").find(trimmed)
            if (dmyMatch != null) {
                val dd = dmyMatch.groupValues[1].padStart(2, '0')
                val mm = dmyMatch.groupValues[2].padStart(2, '0')
                var yyyy = dmyMatch.groupValues[3]
                if (yyyy.length == 2) yyyy = "20$yyyy"
                return "$dd$mm$yyyy"
            }
            if (digits.length == 8) {
                return digits
            }
        }
        val ts = if (fallbackTimestamp > 0) fallbackTimestamp else System.currentTimeMillis()
        return SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date(ts))
    }

    fun formatNomeElaborador(nome: String): String {
        val rawName = nome.ifBlank { "Policial_Militar" }
        val sanitized = sanitizeString(rawName)
        return sanitized.ifBlank { "Policial_Militar" }
    }

    fun getPdfFileName(record: ChecklistRecordEntity): String {
        val numParte = formatNumeroParte(record.reportNumber)
        val data = formatDataServico(record.verificationDate, record.completedTimestampMillis)
        val driverOrResp = if (record.driverName.isNotBlank()) record.driverName else record.responsibleName
        val elaborador = formatNomeElaborador(driverOrResp)
        return "Relatorio_Parte_${numParte}_${data}_${elaborador}.pdf"
    }

    fun generatePdf(context: Context, record: ChecklistRecordEntity): File {
        val filename = getPdfFileName(record)
        val outputDir = File(context.filesDir, "reports").apply { mkdirs() }
        val outputFile = File(outputDir, filename)

        val doc = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXTO
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXTO
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDA
            strokeWidth = 0.8f
        }

        val contentWidth = PAGE_WIDTH - 2 * MARGIN_X

        // Carrega o brasão oficial da PMPI em alta resolução
        val badgeBitmap: Bitmap? = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_pmpi_brasao)
        } catch (_: Exception) {
            null
        }

        val totalPages = 2

        val motoristaDisplayName = if (record.driverName.isNotBlank()) record.driverName else record.responsibleName
        val watermarkMotorista = motoristaDisplayName.uppercase().ifBlank { "POLICIAL MILITAR DO PIAUÍ" }
        val watermarkPosto = record.responsibleRank.uppercase().ifBlank { "PMPI" }

        val horaFinalizacao = run {
            val raw = record.completedDateFormatted
            if (raw.contains("—")) {
                raw.substringAfter("—").trim()
            } else if (raw.contains("às")) {
                raw.substringAfter("às").trim()
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.completedTimestampMillis))
            }
        }
        val footerWatermark = "MOTORISTA: $watermarkMotorista ($watermarkPosto) | FINALIZADO ÀS: $horaFinalizacao"

        fun drawHeader(c: Canvas, isPageTwo: Boolean = false) {
            val headerHeight = 74f

            // Barra Azul Petróleo
            paint.color = COLOR_AZUL_PETROLEO
            paint.style = Paint.Style.FILL
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

            // Friso Dourado
            paint.color = COLOR_DOURADO
            c.drawRect(0f, headerHeight, PAGE_WIDTH.toFloat(), headerHeight + 2.5f, paint)

            // Brasão PMPI renderizado em alta definição via RectF com interpolação bilinear
            val textLeftMargin = if (badgeBitmap != null) {
                val targetSize = 44f
                val bw = badgeBitmap.width.toFloat()
                val bh = badgeBitmap.height.toFloat()
                val scale = targetSize / maxOf(bw, bh)
                val destW = bw * scale
                val destH = bh * scale
                val badgeY = (headerHeight - destH) / 2f
                val destRect = RectF(MARGIN_X, badgeY, MARGIN_X + destW, badgeY + destH)

                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true
                }
                c.drawBitmap(badgeBitmap, null, destRect, badgePaint)

                MARGIN_X + destW + 12f
            } else {
                MARGIN_X
            }

            paint.textAlign = Paint.Align.LEFT

            // Linha 1: Kicker dourado
            paint.color = COLOR_DOURADO
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 9.5f
            val headerTop = if (record.grandCommand.isNotBlank()) {
                "POLÍCIA MILITAR DO ESTADO DO PIAUÍ | ${record.grandCommand.uppercase()}"
            } else {
                "POLÍCIA MILITAR DO ESTADO DO PIAUÍ"
            }
            c.drawText(headerTop, textLeftMargin, 24f, paint)

            // Linha 2: Unidade / Batalhão em branco puro
            paint.color = Color.WHITE
            paint.textSize = 11.5f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val unitClean = record.unitName
                .replace("Companhia Independente de Aviação e Policiamento Aéreo", "Companhia Independente de Operações Aéreas", ignoreCase = true)
                .ifBlank { "QUARTEL DO COMANDO GERAL" }
                .uppercase()
            c.drawText(unitClean, textLeftMargin, 42f, paint)

            // Linha 3: Título do documento
            paint.color = Color.rgb(230, 240, 246)
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val subTitle = if (isPageTwo) "REGISTRO FOTOGRÁFICO DA VIATURA" else "RELATÓRIO DE CHECKLIST DE VIATURA"
            c.drawText(subTitle, textLeftMargin, 59f, paint)

            // Número da Parte / Relatório no canto direito
            val numParte = formatNumeroParte(record.reportNumber)
            val parteText = "PARTE Nº $numParte"
            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val parteWidth = paint.measureText(parteText)
            val parteX = PAGE_WIDTH - MARGIN_X - parteWidth
            c.drawText(parteText, parteX, 59f, paint)
        }

        fun drawWatermark(c: Canvas) {
            c.save()
            val centerX = PAGE_WIDTH / 2f
            val centerY = PAGE_HEIGHT / 2f

            c.rotate(-32f, centerX, centerY)

            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(20, 15, 58, 83) // Azul Petróleo translúcido institucional
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            // Linha 1: CHECKLIST DE VIATURA — PMPI
            wmPaint.textSize = 20f
            wmPaint.letterSpacing = 0.08f
            c.drawText("CHECKLIST DE VIATURA — PMPI", centerX, centerY - 24f, wmPaint)

            // Linha 2: [MOTORISTA]
            val nomeLength = watermarkMotorista.length
            wmPaint.textSize = when {
                nomeLength > 36 -> 13.5f
                nomeLength > 28 -> 15f
                else -> 16.5f
            }
            wmPaint.letterSpacing = 0.04f
            c.drawText(watermarkMotorista, centerX, centerY + 2f, wmPaint)

            // Linha 3: MOTORISTA | [POSTO/GRADUAÇÃO]
            val linha3 = "MOTORISTA | $watermarkPosto"
            wmPaint.textSize = 12.5f
            wmPaint.letterSpacing = 0.05f
            c.drawText(linha3, centerX, centerY + 26f, wmPaint)

            c.restore()
        }

        fun drawFooter(c: Canvas, page: Int) {
            val footerLineY = PAGE_HEIGHT - 38f
            val footerY = PAGE_HEIGHT - 26f
            val watermarkY = PAGE_HEIGHT - 14f

            // Hairline divisor suave
            linePaint.color = COLOR_BORDA_SUAVE
            c.drawLine(MARGIN_X, footerLineY, PAGE_WIDTH - MARGIN_X, footerLineY, linePaint)

            // Linha principal do rodapé
            paint.color = COLOR_TEXTO_MUTED
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val footerUnit = record.unitName
                .replace("Companhia Independente de Aviação e Policiamento Aéreo", "Companhia Independente de Operações Aéreas", ignoreCase = true)
                .ifBlank { "PMPI" }
            val footerLeft = if (footerUnit.contains("PMPI", ignoreCase = true)) footerUnit else "$footerUnit | PMPI"
            c.drawText(footerLeft, MARGIN_X, footerY, paint)

            paint.textAlign = Paint.Align.RIGHT
            c.drawText("Página $page de $totalPages", PAGE_WIDTH - MARGIN_X, footerY, paint)
            paint.textAlign = Paint.Align.LEFT

            // Marca d'água sutil e discreta no rodapé
            val wmFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(85, 100, 116, 139)
                textSize = 6.8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                letterSpacing = 0.04f
                textAlign = Paint.Align.CENTER
            }
            c.drawText(footerWatermark, PAGE_WIDTH / 2f, watermarkY, wmFooterPaint)
        }

        fun drawSectionHeader(c: Canvas, title: String, startY: Float): Float {
            val barHeight = 18f
            paint.color = COLOR_AZUL_CLARO
            c.drawRoundRect(
                RectF(MARGIN_X, startY, PAGE_WIDTH - MARGIN_X, startY + barHeight),
                3f, 3f, paint
            )

            // Friso lateral azul petróleo
            paint.color = COLOR_AZUL_PETROLEO
            c.drawRect(MARGIN_X, startY, MARGIN_X + 4f, startY + barHeight, paint)

            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 9.5f
            c.drawText(title, MARGIN_X + 10f, startY + 13f, paint)

            return startY + barHeight + 8f
        }

        // =====================================================================
        // PÁGINA 1: IDENTIFICAÇÃO E CHECKLIST MECÂNICO
        // =====================================================================
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = doc.startPage(pageInfo1)
        val canvas1 = page1.canvas

        drawWatermark(canvas1)
        drawHeader(canvas1, isPageTwo = false)

        var currentY = 88f

        // --- I. IDENTIFICAÇÃO DO SERVIÇO E DA VIATURA ---
        currentY = drawSectionHeader(canvas1, "I – IDENTIFICAÇÃO DO SERVIÇO E DA VIATURA", currentY)

        val identCardTop = currentY
        val identHeight = 84f

        // Moldura do quadro de identificação executivo
        paint.color = COLOR_CARD_BG
        canvas1.drawRoundRect(RectF(MARGIN_X, identCardTop, PAGE_WIDTH - MARGIN_X, identCardTop + identHeight), 4f, 4f, paint)
        linePaint.color = COLOR_BORDA_SUAVE
        linePaint.style = Paint.Style.STROKE
        canvas1.drawRoundRect(RectF(MARGIN_X, identCardTop, PAGE_WIDTH - MARGIN_X, identCardTop + identHeight), 4f, 4f, linePaint)
        paint.style = Paint.Style.FILL

        var identY = identCardTop + 14f
        boldPaint.textSize = 8.5f
        textPaint.textSize = 8.5f

        // Linha 1: UNIDADE & GRANDE COMANDO
        val lblUnidade = "UNIDADE: "
        canvas1.drawText(lblUnidade, MARGIN_X + 10f, identY, boldPaint)
        val unitTextVal = buildString {
            append(record.unitName.replace("Companhia Independente de Aviação e Policiamento Aéreo", "Companhia Independente de Operações Aéreas", ignoreCase = true).ifBlank { "PMPI" })
            if (record.grandCommand.isNotBlank()) append(" (${record.grandCommand})")
        }
        canvas1.drawText(unitTextVal, MARGIN_X + 10f + boldPaint.measureText(lblUnidade), identY, textPaint)

        // Linha 2: DATA & MODALIDADE
        identY += 14f
        val lblData = "DATA DA VERIFICAÇÃO: "
        canvas1.drawText(lblData, MARGIN_X + 10f, identY, boldPaint)
        val dataVal = record.verificationDate.ifBlank { "Não informada" }
        val dataValX = MARGIN_X + 10f + boldPaint.measureText(lblData)
        canvas1.drawText(dataVal, dataValX, identY, textPaint)

        val lblMod = "MODALIDADE: "
        val modStartX = dataValX + textPaint.measureText(dataVal) + 24f
        canvas1.drawText(lblMod, modStartX, identY, boldPaint)
        canvas1.drawText(record.serviceModality.ifBlank { "Ordinário" }, modStartX + boldPaint.measureText(lblMod), identY, textPaint)

        // Linha divisória interna suave
        identY += 13f
        linePaint.color = COLOR_BORDA_SUAVE
        canvas1.drawLine(MARGIN_X + 10f, identY - 4f, PAGE_WIDTH - MARGIN_X - 10f, identY - 4f, linePaint)

        // Linha 3: VIATURA, PLACA & KM INICIAL
        val lblVtr = "MODELO: "
        canvas1.drawText(lblVtr, MARGIN_X + 10f, identY + 7f, boldPaint)
        val vtrVal = record.vehicleModel.ifBlank { "Viatura Operacional" }
        val vtrValX = MARGIN_X + 10f + boldPaint.measureText(lblVtr)
        canvas1.drawText(vtrVal, vtrValX, identY + 7f, textPaint)

        val lblPlaca = "PLACA: "
        val placaStartX = vtrValX + textPaint.measureText(vtrVal) + 16f
        canvas1.drawText(lblPlaca, placaStartX, identY + 7f, boldPaint)
        val placaVal = record.vehiclePlate.ifBlank { "---" }
        val placaValX = placaStartX + boldPaint.measureText(lblPlaca)
        canvas1.drawText(placaVal, placaValX, identY + 7f, textPaint)

        val lblKm = "KM INICIAL: "
        val kmStartX = placaValX + textPaint.measureText(placaVal) + 16f
        canvas1.drawText(lblKm, kmStartX, identY + 7f, boldPaint)
        val kmVal = if (record.initialMileage.isNotBlank()) "${record.initialMileage} km" else "Não informado"
        canvas1.drawText(kmVal, kmStartX + boldPaint.measureText(lblKm), identY + 7f, textPaint)

        identY += 15f

        // Linha 4: MOTORISTA DA VIATURA & POSTO/GRADUAÇÃO
        val lblMot = "MOTORISTA DA VIATURA: "
        canvas1.drawText(lblMot, MARGIN_X + 10f, identY + 7f, boldPaint)
        val motNome = if (record.driverName.isNotBlank()) record.driverName else record.responsibleName
        val motVal = "${record.responsibleRank} $motNome".trim().ifBlank { "Não informado" }
        val motValX = MARGIN_X + 10f + boldPaint.measureText(lblMot)
        canvas1.drawText(motVal, motValX, identY + 7f, textPaint)

        currentY = identCardTop + identHeight + 12f

        // --- II. CONDIÇÕES TÉCNICAS E MECÂNICAS ---
        currentY = drawSectionHeader(canvas1, "II – CONDIÇÕES TÉCNICAS E MECÂNICAS DA VIATURA", currentY)

        val checkItems = listOf(
            Triple("1. Óleo do Motor", record.oilStatus, record.oilReason),
            Triple("2. Água do Radiador", record.radiatorWaterStatus, record.radiatorWaterReason),
            Triple("3. Pneus em Bom Estado de Conservação", record.tiresStatus, record.tiresReason),
            Triple("4. Giroflex / Sinalizador Visual e Sonoro", record.lightbarStatus, record.lightbarReason),
            Triple("5. Rádio Comunicador", record.radioStatus, record.radioReason),
            Triple("6. Estepe da Viatura", record.spareTireStatus, record.spareTireReason),
            Triple("7. Macaco Hidráulico e Chave de Roda", record.jackStatus, record.jackReason),
            Triple("8. Faróis, Lanternas e Iluminação", record.headlightsStatus, record.headlightsReason),
            Triple("9. Ar-Condicionado / Climatização", record.airConditioningStatus, record.airConditioningReason)
        )

        val tableLeft = MARGIN_X
        val tableRight = PAGE_WIDTH - MARGIN_X
        val totalWidth = tableRight - tableLeft
        val itemColWidth = totalWidth * 0.64f
        val defaultRowHeight = 19f

        // Cabeçalho da Tabela
        val tableHeaderPaint = Paint().apply {
            color = COLOR_AZUL_PETROLEO
            style = Paint.Style.FILL
        }
        canvas1.drawRoundRect(RectF(tableLeft, currentY, tableRight, currentY + defaultRowHeight), 3f, 3f, tableHeaderPaint)

        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas1.drawText("ITEM VERIFICADO", tableLeft + 8f, currentY + 12.5f, tableHeaderTextPaint)
        canvas1.drawText("SITUAÇÃO / CONFORMIDADE", tableLeft + itemColWidth + 8f, currentY + 12.5f, tableHeaderTextPaint)

        currentY += defaultRowHeight

        val borderPaint = Paint().apply {
            color = COLOR_BORDA_SUAVE
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val rowItemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXTO
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val reasonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ALERT
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        for (i in checkItems.indices) {
            val (item, status, reason) = checkItems[i]
            val isAlt = (i % 2 == 1)
            val hasReason = reason.isNotBlank()
            val rowHeight = if (hasReason) 28f else defaultRowHeight

            if (isAlt) {
                val rowBgPaint = Paint().apply {
                    color = COLOR_CARD_BG
                    style = Paint.Style.FILL
                }
                canvas1.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, rowBgPaint)
            }

            canvas1.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, borderPaint)
            canvas1.drawLine(tableLeft + itemColWidth, currentY, tableLeft + itemColWidth, currentY + rowHeight, borderPaint)

            if (hasReason) {
                canvas1.drawText(item, tableLeft + 8f, currentY + 11f, rowItemPaint)
                val truncatedReason = if (reason.length > 58) reason.take(55) + "..." else reason
                canvas1.drawText("Motivo da alteração: $truncatedReason", tableLeft + 8f, currentY + 22f, reasonTextPaint)
            } else {
                canvas1.drawText(item, tableLeft + 8f, currentY + 12.5f, rowItemPaint)
            }

            // Status Badge
            val isOk = (status == "SEM ALTERAÇÃO" || status == "SIM")
            val badgeBgColor = if (isOk) COLOR_SUCCESS_BG else COLOR_ALERT_BG
            val badgeTextColor = if (isOk) COLOR_SUCCESS else COLOR_ALERT
            val displayStatus = when {
                status == "SIM" -> "SIM (EM BOM ESTADO)"
                status == "NÃO" -> "NÃO (COM ALTERAÇÃO)"
                else -> status
            }

            val badgeRect = RectF(
                tableLeft + itemColWidth + 8f,
                currentY + (rowHeight - 14f) / 2f,
                tableRight - 8f,
                currentY + (rowHeight + 14f) / 2f
            )
            val badgeBgPaint = Paint().apply {
                color = badgeBgColor
                style = Paint.Style.FILL
            }
            canvas1.drawRoundRect(badgeRect, 3f, 3f, badgeBgPaint)

            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = badgeTextColor
                textSize = 7f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas1.drawText(displayStatus, badgeRect.centerX(), badgeRect.centerY() + 2.5f, badgeTextPaint)

            currentY += rowHeight
        }

        drawFooter(canvas1, 1)
        doc.finishPage(page1)

        // =====================================================================
        // PÁGINA 2: REGISTRO FOTOGRÁFICO OBRIGATÓRIO E ASSINATURA DO MOTORISTA
        // =====================================================================
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = doc.startPage(pageInfo2)
        val canvas2 = page2.canvas

        drawWatermark(canvas2)
        drawHeader(canvas2, isPageTwo = true)

        var currentY2 = 88f
        currentY2 = drawSectionHeader(canvas2, "REGISTRO FOTOGRÁFICO OBRIGATÓRIO (4 ÂNGULOS)", currentY2)

        val photos = listOf(
            Triple("1. Frente da Viatura", record.photoFrontPath, 0),
            Triple("2. Lado do Motorista", record.photoDriverSidePath, 1),
            Triple("3. Lado do Passageiro", record.photoPassengerSidePath, 2),
            Triple("4. Parte Traseira da Viatura", record.photoRearPath, 3)
        )

        val totalWidthP2 = PAGE_WIDTH - 2 * MARGIN_X
        val spacing = 12f
        val photoWidth = (totalWidthP2 - spacing) / 2
        val photoHeight = 215f

        for (item in photos) {
            val (caption, path, index) = item
            val row = index / 2
            val col = index % 2

            val x = MARGIN_X + col * (photoWidth + spacing)
            val y = currentY2 + row * (photoHeight + spacing + 6f)

            // Moldura da foto
            val frameRect = RectF(x, y, x + photoWidth, y + photoHeight)
            val bgPaint = Paint().apply {
                color = COLOR_CARD_BG
                style = Paint.Style.FILL
            }
            canvas2.drawRoundRect(frameRect, 4f, 4f, bgPaint)

            val frameBorderPaint = Paint().apply {
                color = COLOR_BORDA_SUAVE
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas2.drawRoundRect(frameRect, 4f, 4f, frameBorderPaint)

            // Barra de Legenda no topo da moldura
            val captionBarHeight = 18f
            val captionBarRect = RectF(x, y, x + photoWidth, y + captionBarHeight)
            val captionBgPaint = Paint().apply {
                color = COLOR_AZUL_CLARO
                style = Paint.Style.FILL
            }
            canvas2.drawRoundRect(captionBarRect, 4f, 4f, captionBgPaint)

            val captionFrisoPaint = Paint().apply {
                color = COLOR_AZUL_PETROLEO
                style = Paint.Style.FILL
            }
            canvas2.drawRect(x, y, x + 3.5f, y + captionBarHeight, captionFrisoPaint)

            val photoCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_AZUL_PETROLEO
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas2.drawText(caption, x + 8f, y + 12.5f, photoCaptionPaint)

            // Área de exibição da imagem
            val imageRect = RectF(x + 4f, y + captionBarHeight + 4f, x + photoWidth - 4f, y + photoHeight - 4f)

            var loadedBitmap: Bitmap? = null
            if (!path.isNullOrBlank()) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        val sampleSize = calculateInSampleSize(options, photoWidth.toInt(), photoHeight.toInt())
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        loadedBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    }
                } catch (_: Exception) {
                    loadedBitmap = null
                }
            }

            if (loadedBitmap != null) {
                drawBitmapAspectFit(canvas2, loadedBitmap, imageRect)
            } else {
                // Placeholder informativo elegante
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXTO_MUTED
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas2.drawText("Registro fotográfico não anexado", imageRect.centerX(), imageRect.centerY(), placeholderPaint)
            }
        }

        currentY2 += (2 * photoHeight + spacing + 14f)

        // =====================================================================
        // ÚLTIMO TÓPICO OBRIGATÓRIO DO PDF: ASSINATURA DO MOTORISTA
        // =====================================================================
        currentY2 = drawSectionHeader(canvas2, "ASSINATURA DO MOTORISTA", currentY2)

        val signBoxTop = currentY2
        val signBoxHeight = 68f

        paint.color = COLOR_CARD_BG
        canvas2.drawRoundRect(RectF(MARGIN_X, signBoxTop, PAGE_WIDTH - MARGIN_X, signBoxTop + signBoxHeight), 4f, 4f, paint)
        linePaint.color = COLOR_BORDA_SUAVE
        linePaint.style = Paint.Style.STROKE
        canvas2.drawRoundRect(RectF(MARGIN_X, signBoxTop, PAGE_WIDTH - MARGIN_X, signBoxTop + signBoxHeight), 4f, 4f, linePaint)
        paint.style = Paint.Style.FILL

        // Friso dourado na esquerda da caixa executiva
        paint.color = COLOR_DOURADO
        canvas2.drawRect(MARGIN_X, signBoxTop, MARGIN_X + 3.5f, signBoxTop + signBoxHeight, paint)

        val driverRank = record.responsibleRank.trim()
        val driverFullName = if (record.driverName.isNotBlank()) record.driverName.trim() else record.responsibleName.trim().ifBlank { "Motorista da Viatura" }
        val dataFin = record.completedDateFormatted.ifBlank {
            SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(record.completedTimestampMillis))
        }

        paint.color = COLOR_AZUL_PETROLEO
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas2.drawText("MOTORISTA RESPONSÁVEL PELA CONDUÇÃO DA VIATURA", MARGIN_X + 12f, signBoxTop + 16f, paint)

        boldPaint.textSize = 9.5f
        canvas2.drawText("$driverRank $driverFullName".trim().uppercase(), MARGIN_X + 12f, signBoxTop + 30f, boldPaint)

        textPaint.textSize = 8f
        val idString = "Documento assinado digitalmente | Finalizado em: $dataFin"
        canvas2.drawText(idString, MARGIN_X + 12f, signBoxTop + 44f, textPaint)

        paint.color = COLOR_TEXTO_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas2.drawText("Documento gerado e autenticado pelo Sistema Digital de Checklist da PMPI", MARGIN_X + 12f, signBoxTop + 57f, paint)

        drawFooter(canvas2, 2)
        doc.finishPage(page2)

        // Gravação em arquivo
        val outputStream = FileOutputStream(outputFile)
        doc.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        doc.close()

        return outputFile
    }

    private fun drawBitmapAspectFit(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val targetWidth = destRect.width()
        val targetHeight = destRect.height()

        val scale = Math.min(targetWidth / srcWidth, targetHeight / srcHeight)
        val finalWidth = srcWidth * scale
        val finalHeight = srcHeight * scale

        val left = destRect.left + (targetWidth - finalWidth) / 2f
        val top = destRect.top + (targetHeight - finalHeight) / 2f
        val drawRect = RectF(left, top, left + finalWidth, top + finalHeight)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bitmap, null, drawRect, paint)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
