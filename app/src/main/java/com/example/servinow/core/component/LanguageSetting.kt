package com.example.servinow.core.component

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.servinow.R

@Composable
fun LanguageSetting() {
    var show by rememberSaveable { mutableStateOf(false) }
    val selected = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    val system = stringResource(R.string.language_system)
    val options = listOf("es" to "Español", "en" to "English", "" to system)
    val label = options.firstOrNull { it.first == selected }?.second ?: system
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text("${stringResource(R.string.language_title)}: $label")
    }
    if (show) AlertDialog(
        onDismissRequest = { show = false },
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                Text(stringResource(R.string.language_description))
                Spacer(Modifier.height(12.dp))
                options.forEach { (code, name) ->
                    Row(Modifier.fillMaxWidth().selectable(selected = selected == code, role = Role.RadioButton, onClick = {
                        show = false
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                    }).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == code, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { show = false }) { Text(stringResource(R.string.language_cancel)) } }
    )
}
