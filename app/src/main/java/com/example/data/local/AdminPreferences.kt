package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AdminPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pmpi_admin_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        const val DEFAULT_ADMIN_PASSWORD = "admin"

        private const val KEY_CONFIGURED_CMD_CODE = "configured_cmd_code"
        private const val KEY_CONFIGURED_CMD_NAME = "configured_cmd_name"
        private const val KEY_CONFIGURED_UNIT_ABBREV = "configured_unit_abbrev"
        private const val KEY_CONFIGURED_UNIT_NAME = "configured_unit_name"

        const val DEFAULT_CMD_CODE = "CPM"
        const val DEFAULT_CMD_NAME = "COMANDO DE POLICIAMENTO METROPOLITANO"
        const val DEFAULT_UNIT_ABBREV = "29º BPM"
        const val DEFAULT_UNIT_NAME = "29º Batalhão de Polícia Militar"
    }

    fun getPassword(): String {
        return prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
    }

    fun setPassword(newPass: String) {
        prefs.edit().putString(KEY_ADMIN_PASSWORD, newPass).apply()
    }

    fun checkPassword(input: String): Boolean {
        return input.trim() == getPassword().trim()
    }

    fun getConfiguredGrandCommandCode(): String {
        return prefs.getString(KEY_CONFIGURED_CMD_CODE, DEFAULT_CMD_CODE) ?: DEFAULT_CMD_CODE
    }

    fun getConfiguredGrandCommandName(): String {
        return prefs.getString(KEY_CONFIGURED_CMD_NAME, DEFAULT_CMD_NAME) ?: DEFAULT_CMD_NAME
    }

    fun getConfiguredUnitAbbrev(): String {
        return prefs.getString(KEY_CONFIGURED_UNIT_ABBREV, DEFAULT_UNIT_ABBREV) ?: DEFAULT_UNIT_ABBREV
    }

    fun getConfiguredUnitName(): String {
        val raw = prefs.getString(KEY_CONFIGURED_UNIT_NAME, DEFAULT_UNIT_NAME) ?: DEFAULT_UNIT_NAME
        return raw.replace("Companhia Independente de Aviação e Policiamento Aéreo", "Companhia Independente de Operações Aéreas", ignoreCase = true)
    }

    fun setConfiguredUnit(
        cmdCode: String,
        cmdName: String,
        unitAbbrev: String,
        unitName: String
    ) {
        prefs.edit()
            .putString(KEY_CONFIGURED_CMD_CODE, cmdCode)
            .putString(KEY_CONFIGURED_CMD_NAME, cmdName)
            .putString(KEY_CONFIGURED_UNIT_ABBREV, unitAbbrev)
            .putString(KEY_CONFIGURED_UNIT_NAME, unitName)
            .apply()
    }
}
