package com.varsel.expensetracker.developer

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperRepository @Inject constructor(

    @ApplicationContext
    private val context: Context

) {

    val parserDiagnosticsEnabled: Flow<Boolean> =
        context.developerDataStore.data.map { preferences ->

            preferences[
                DeveloperPreferences.PARSER_DIAGNOSTICS
            ] ?: false

        }

    suspend fun setParserDiagnostics(
        enabled: Boolean
    ) {

        context.developerDataStore.edit {

            it[
                DeveloperPreferences.PARSER_DIAGNOSTICS
            ] = enabled

        }
    }
}
