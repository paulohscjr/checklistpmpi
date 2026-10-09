package com.example.service.cloud

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Cliente REST HTTP para comunicação com a API v3 do Google Drive via OkHttp.
 * Realiza a consulta, criação idempotente de pastas e upload de arquivos em formato multipart.
 */
class GoogleDriveRestService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val pdfMediaType = "application/pdf".toMediaType()
    private val multipartRelatedMediaType = "multipart/related".toMediaType()

    /**
     * Pesquisa se já existe uma pasta não deletada com [folderName] sob a pasta-mãe [parentId].
     * Retorna o ID da pasta caso exista, ou null se não encontrada.
     */
    fun searchFolder(token: String, parentId: String, folderName: String): String? {
        val safeName = folderName.replace("'", "\\'")
        val query = "mimeType = 'application/vnd.google-apps.folder' and trashed = false and name = '$safeName' and '$parentId' in parents"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Erro ao pesquisar pasta no Google Drive (${response.code}): $bodyString")
        }

        val json = JSONObject(bodyString)
        val filesArray = json.optJSONArray("files")
        if (filesArray != null && filesArray.length() > 0) {
            return filesArray.getJSONObject(0).getString("id")
        }
        return null
    }

    /**
     * Cria uma nova pasta com [folderName] dentro da pasta pai [parentId].
     */
    fun createFolder(token: String, parentId: String, folderName: String): String {
        val url = "https://www.googleapis.com/drive/v3/files"
        val payload = JSONObject().apply {
            put("name", folderName)
            put("mimeType", "application/vnd.google-apps.folder")
            put("parents", JSONArray().put(parentId))
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Erro ao criar pasta '$folderName' no Google Drive (${response.code}): $bodyString")
        }

        return JSONObject(bodyString).getString("id")
    }

    /**
     * Localiza a pasta existente ou cria caso não exista, garantindo a não-duplicação.
     */
    fun getOrCreateFolder(token: String, parentId: String, folderName: String): String {
        val existingId = searchFolder(token, parentId, folderName)
        if (!existingId.isNullOrBlank()) {
            return existingId
        }
        return createFolder(token, parentId, folderName)
    }

    /**
     * Realiza o upload do arquivo PDF oficial para a pasta especificada [folderId] no Google Drive
     * utilizando envio multipart/related com metadados e conteúdo binário do PDF.
     */
    fun uploadPdf(token: String, folderId: String, pdfFile: File): String {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            throw IOException("O arquivo PDF local não existe ou está vazio.")
        }

        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

        val metadata = JSONObject().apply {
            put("name", pdfFile.name)
            put("parents", JSONArray().put(folderId))
        }

        val multipartBody = MultipartBody.Builder()
            .setType(multipartRelatedMediaType)
            .addPart(metadata.toString().toRequestBody(jsonMediaType))
            .addPart(pdfFile.asRequestBody(pdfMediaType))
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Erro ao enviar arquivo para o Google Drive (${response.code}): $bodyString")
        }

        val resultJson = JSONObject(bodyString)
        return resultJson.getString("id")
    }
}
