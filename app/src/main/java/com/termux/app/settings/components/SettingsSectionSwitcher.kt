package com.termux.app.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.termux.R
import com.termux.app.activities.SettingsSection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSectionSwitcher(
    selectedSection: SettingsSection,
    onSelectSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleColors = ToggleButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.primary,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val termuxSelected = selectedSection == SettingsSection.TERMUX
        ToggleButton(
            checked = termuxSelected,
            onCheckedChange = { checked ->
                if (checked && !termuxSelected) {
                    onSelectSection(SettingsSection.TERMUX)
                }
            },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            colors = toggleColors,
            modifier = Modifier
                .weight(1f)
                .testTag("section_tab_termux")
                .semantics { role = Role.RadioButton },
        ) {
            Icon(
                imageVector = if (termuxSelected) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                contentDescription = null,
                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
            Text(
                text = stringResource(R.string.settings_section_termux),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val x11Selected = selectedSection == SettingsSection.X11
        ToggleButton(
            checked = x11Selected,
            onCheckedChange = { checked ->
                if (checked && !x11Selected) {
                    onSelectSection(SettingsSection.X11)
                }
            },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            colors = toggleColors,
            modifier = Modifier
                .weight(1f)
                .testTag("section_tab_x11")
                .semantics { role = Role.RadioButton },
        ) {
            Icon(
                imageVector = if (x11Selected) Icons.Filled.DesktopWindows else Icons.Outlined.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
            Text(
                text = stringResource(R.string.settings_section_x11),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
