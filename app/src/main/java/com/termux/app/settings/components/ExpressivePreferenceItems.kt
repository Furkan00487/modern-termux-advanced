package com.termux.app.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termux.app.settings.theme.ModernTermuxTheme
import com.termux.app.settings.theme.MonospaceValueTextStyle
import kotlin.math.roundToInt

@Composable
fun SettingsGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
fun SettingsSegmentedGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun expressiveSegmentedListColors(): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    leadingContentColor = MaterialTheme.colorScheme.primary,
    trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledLeadingContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledTrailingContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingContentColor = MaterialTheme.colorScheme.primary,
    selectedTrailingContentColor = MaterialTheme.colorScheme.primary,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
)

@Composable
fun expressiveSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
)

@Composable
private fun LeadingIconBadge(
    icon: ImageVector,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val bgColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveNavRow(
    title: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    trailingBadge: String? = null,
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = expressiveSegmentedListColors(),
        leadingContent = icon?.let {
            { LeadingIconBadge(icon = it, selected = selected, enabled = enabled) }
        },
        supportingContent = if (!summary.isNullOrBlank()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            null
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!trailingBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = trailingBadge,
                            style = MonospaceValueTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = expressiveSegmentedListColors(),
        leadingContent = icon?.let {
            { LeadingIconBadge(icon = it, selected = checked && enabled, enabled = enabled) }
        },
        supportingContent = if (!summary.isNullOrBlank()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            null
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = expressiveSwitchColors(),
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveChoiceRow(
    title: String,
    currentLabel: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = expressiveSegmentedListColors(),
        leadingContent = icon?.let {
            { LeadingIconBadge(icon = it, enabled = enabled) }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (currentLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Text(
                            text = currentLabel,
                            style = MonospaceValueTextStyle,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveActionRow(
    title: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    valueBadge: String? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = expressiveSegmentedListColors(),
        leadingContent = icon?.let {
            { LeadingIconBadge(icon = it, enabled = enabled) }
        },
        supportingContent = if (!summary.isNullOrBlank()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            null
        },
        trailingContent = {
            if (!valueBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = valueBadge,
                        style = MonospaceValueTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun ExpressiveSliderRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    onValueCommitted: (Int) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    valueSuffix: String = "%",
    summary: String? = null,
    enabled: Boolean = true,
    snapTransform: (Int) -> Int = { it },
) {
    var sliderFloat by remember(value) { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) {
        sliderFloat = value.toFloat()
    }

    val topRadius = if (index == 0) 24.dp else 6.dp
    val bottomRadius = if (index == count - 1) 24.dp else 6.dp
    val cardShape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
    val stepsCount = if (step > 1) {
        ((valueRange.last - valueRange.first) / step - 1).coerceAtLeast(0)
    } else {
        0
    }
    val previewInt = snapTransform(sliderFloat.roundToInt()).coerceIn(valueRange.first, valueRange.last)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "$previewInt$valueSuffix",
                        style = MonospaceValueTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Slider(
                value = sliderFloat,
                onValueChange = { next ->
                    sliderFloat = snapTransform(next.roundToInt()).coerceIn(valueRange.first, valueRange.last).toFloat()
                },
                onValueChangeFinished = {
                    val committed = snapTransform(sliderFloat.roundToInt()).coerceIn(valueRange.first, valueRange.last)
                    sliderFloat = committed.toFloat()
                    if (committed != value) {
                        onValueCommitted(committed)
                    }
                },
                enabled = enabled,
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = stepsCount,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.testTag("slider_$title"),
            )
        }
    }
}

@Composable
fun SettingsInfoBanner(
    title: String,
    message: String,
    icon: ImageVector,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isWarning) {
        ModernTermuxTheme.extendedColors.warningContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val accentColor = if (isWarning) {
        ModernTermuxTheme.extendedColors.warning
    } else {
        MaterialTheme.colorScheme.primary
    }
    val borderColor = accentColor.copy(alpha = 0.45f)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
