package com.termux.app.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.termux.R
import com.termux.app.settings.data.ChoiceOption
import com.termux.app.settings.data.X11SettingsRules
import com.termux.app.settings.theme.MonospaceValueTextStyle
import com.termux.x11.R as X11R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChoicePickerDialog(
    title: String,
    options: List<ChoiceOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val itemColors = expressiveSegmentedListColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                options.forEachIndexed { idx, option ->
                    val isSelected = option.value == selectedValue
                    SegmentedListItem(
                        selected = isSelected,
                        onClick = {
                            onSelect(option.value)
                            onDismiss()
                        },
                        shapes = ListItemDefaults.segmentedShapes(index = idx, count = options.size),
                        colors = itemColors,
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        },
                        content = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun CustomResolutionDialog(
    initialValue: String,
    onSave: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val trimmed = text.trim()
    val valid = X11SettingsRules.isValidCustomResolution(trimmed)
    var attemptedSubmit by remember { mutableStateOf(false) }
    val showError = attemptedSubmit && !valid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(X11R.string.lorie_pref_displayResolutionCustom),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        attemptedSubmit = false
                    },
                    singleLine = true,
                    isError = showError,
                    label = { Text(stringResource(R.string.settings_custom_resolution_hint)) },
                    textStyle = MonospaceValueTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(X11R.string.lorie_invalid_resolution_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid && onSave(trimmed)) {
                        onDismiss()
                    } else {
                        attemptedSubmit = true
                    }
                },
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun ExtraKeysConfigDialog(
    initialConfig: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var configText by remember(initialConfig) { mutableStateOf(initialConfig) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(X11R.string.lorie_pref_extra_keys_config),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = configText,
                    onValueChange = { configText = it },
                    minLines = 3,
                    maxLines = 7,
                    textStyle = MonospaceValueTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                val descHtml = stringResource(X11R.string.extra_keys_config_desc)
                val descPlain = androidx.core.text.HtmlCompat.fromHtml(
                    descHtml,
                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT,
                ).toString()
                Text(
                    text = descPlain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(configText)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onReset()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.settings_reset))
            }
        },
    )
}

@Composable
fun WriteSecureSettingsDialog(
    packageName: String,
    onCopyCommand: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(X11R.string.lorie_permission_denied_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(X11R.string.lorie_write_secure_settings_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = adbCommand,
                        style = MonospaceValueTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCopyCommand(adbCommand)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.settings_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_ok))
            }
        },
    )
}
