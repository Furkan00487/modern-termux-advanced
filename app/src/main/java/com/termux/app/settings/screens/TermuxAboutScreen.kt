package com.termux.app.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termux.BuildConfig
import com.termux.R
import com.termux.app.settings.components.ExpressiveActionRow
import com.termux.app.settings.components.ExpressiveNavRow
import com.termux.app.settings.components.SettingsDetailTopBar
import com.termux.app.settings.components.SettingsGroupHeader
import com.termux.app.settings.components.SettingsSegmentedGroup
import com.termux.app.settings.data.TermuxAboutInfo
import com.termux.app.settings.data.TermuxSettingsUiState
import com.termux.app.settings.navigation.SettingsRoute
import com.termux.app.settings.theme.ModernTermuxTheme
import com.termux.app.settings.theme.MonospaceValueTextStyle
import com.termux.shared.termux.TermuxConstants

private data class AboutLinkItem(
    val title: String,
    val summary: String,
    val url: String,
    val icon: ImageVector,
    val testTag: String? = null,
)

private fun siblingModernTermuxRepos(): List<Pair<String, String>> = listOf(
    TermuxConstants.MODERN_TERMUX_STANDARD_APP_NAME to TermuxConstants.MODERN_TERMUX_STANDARD_GITHUB_REPO_URL,
    TermuxConstants.MODERN_TERMUX_ADVANCED_APP_NAME to TermuxConstants.MODERN_TERMUX_ADVANCED_GITHUB_REPO_URL,
    TermuxConstants.MODERN_TERMUX_TV_APP_NAME to TermuxConstants.MODERN_TERMUX_TV_GITHUB_REPO_URL,
    TermuxConstants.MODERN_TERMUX_MOTD_APP_NAME to TermuxConstants.MODERN_TERMUX_MOTD_GITHUB_REPO_URL,
).filterNot { (_, url) ->
    url == TermuxConstants.MODERN_TERMUX_GITHUB_REPO_URL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxAboutScreen(
    termuxState: TermuxSettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onLoadAboutInfo: suspend () -> TermuxAboutInfo,
    onOpenUrl: (title: String, url: String) -> Unit,
    onNavigateToDetail: (SettingsRoute) -> Unit,
    onCopyText: (String) -> Unit,
    onShareReport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var aboutInfo by remember { mutableStateOf<TermuxAboutInfo?>(null) }
    var showSystemReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        aboutInfo = onLoadAboutInfo()
    }

    val versionName = aboutInfo?.versionName ?: BuildConfig.VERSION_NAME
    val versionCode = aboutInfo?.versionCode ?: BuildConfig.VERSION_CODE.toString()
    val fallbackSummary = "${BuildConfig.APPLICATION_ID} · v$versionName"
    val fallbackReport = "${TermuxConstants.MODERN_TERMUX_VARIANT_NAME} v$versionName ($versionCode)"
    val reportText = aboutInfo?.markdownReport ?: fallbackReport

    val projectLinks = remember {
        buildList {
            add(
                AboutLinkItem(
                    title = TermuxConstants.MODERN_TERMUX_VARIANT_NAME,
                    summary = "",
                    url = TermuxConstants.MODERN_TERMUX_GITHUB_REPO_URL,
                    icon = Icons.Rounded.Code,
                    testTag = "about_active_repo",
                ),
            )
            add(
                AboutLinkItem(
                    title = "",
                    summary = "",
                    url = TermuxConstants.MODERN_TERMUX_GITHUB_ISSUES_REPO_URL,
                    icon = Icons.Rounded.BugReport,
                    testTag = "about_report_issue",
                ),
            )
            siblingModernTermuxRepos().forEach { (appName, url) ->
                add(
                    AboutLinkItem(
                        title = appName,
                        summary = "",
                        url = url,
                        icon = Icons.Rounded.Public,
                    ),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.about_preference_title),
                sectionBadge = stringResource(R.string.settings_section_termux),
                showBackButton = showBackButton,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("about_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "about_hero") {
                AboutHeroCard(
                    variantName = aboutInfo?.variantName ?: TermuxConstants.MODERN_TERMUX_VARIANT_NAME,
                    versionLabel = "v$versionName ($versionCode)",
                    abiLabel = aboutInfo?.primaryAbi?.takeIf { it.isNotBlank() && it != "unknown" },
                    onCopyReport = { onCopyText(reportText) },
                    onShareReport = { onShareReport(reportText) },
                )
            }

            item(key = "version_and_system") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_about_group_version_system))
                    SettingsSegmentedGroup {
                        ExpressiveActionRow(
                            title = stringResource(R.string.settings_about_view_system_report),
                            summary = aboutInfo?.let {
                                "${it.packageName} · ${it.androidVersion}"
                            } ?: fallbackSummary,
                            icon = Icons.Rounded.Verified,
                            index = 0,
                            count = 2,
                            valueBadge = aboutInfo?.apkRelease?.takeIf { it.isNotBlank() && it != "unknown" },
                            onClick = { showSystemReportDialog = true },
                            modifier = Modifier.testTag("about_view_system_report"),
                        )
                        val releaseTitle = stringResource(
                            R.string.settings_about_release_notes,
                            TermuxConstants.MODERN_TERMUX_VARIANT_NAME,
                        )
                        ExpressiveActionRow(
                            title = releaseTitle,
                            summary = stringResource(
                                R.string.settings_about_release_notes_summary,
                                TermuxConstants.MODERN_TERMUX_GITHUB_RELEASES_URL.removePrefix("https://"),
                            ),
                            icon = Icons.Rounded.NewReleases,
                            index = 1,
                            count = 2,
                            onClick = {
                                onOpenUrl(releaseTitle, TermuxConstants.MODERN_TERMUX_GITHUB_RELEASES_URL)
                            },
                            modifier = Modifier.testTag("about_release_notes"),
                        )
                    }
                }
            }

            if (termuxState.installedPlugins.isNotEmpty()) {
                item(key = "installed_plugins") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsGroupHeader(stringResource(R.string.settings_group_plugins))
                        SettingsSegmentedGroup {
                            val count = termuxState.installedPlugins.size
                            termuxState.installedPlugins.forEachIndexed { idx, plugin ->
                                ExpressiveNavRow(
                                    title = stringResource(plugin.titleRes),
                                    summary = stringResource(plugin.summaryRes),
                                    icon = Icons.Rounded.Extension,
                                    index = idx,
                                    count = count,
                                    onClick = {
                                        onNavigateToDetail(SettingsRoute.TermuxPlugin(plugin.kind))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "modern_termux_project") {
                val issueTitle = stringResource(
                    R.string.settings_about_report_issue,
                    TermuxConstants.MODERN_TERMUX_VARIANT_NAME,
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_about_group_project))
                    SettingsSegmentedGroup {
                        projectLinks.forEachIndexed { idx, item ->
                            val shortPath = item.url.removePrefix("https://")
                            val resolvedTitle = when (idx) {
                                1 -> issueTitle
                                else -> item.title
                            }
                            val resolvedSummary = when (idx) {
                                0 -> stringResource(R.string.settings_about_active_repo_summary, shortPath)
                                1 -> stringResource(R.string.settings_about_report_issue_summary, shortPath)
                                else -> stringResource(R.string.settings_about_sibling_repo_summary, shortPath)
                            }
                            ExpressiveActionRow(
                                title = resolvedTitle,
                                summary = resolvedSummary,
                                icon = item.icon,
                                index = idx,
                                count = projectLinks.size,
                                onClick = { onOpenUrl(resolvedTitle, item.url) },
                                modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                            )
                        }
                    }
                }
            }

            item(key = "upstream_and_docs") {
                val wikiTitle = stringResource(R.string.settings_about_wiki_title)
                val upstreamTitle = stringResource(R.string.settings_about_upstream_title)
                val upstreamIssuesTitle = stringResource(R.string.settings_about_upstream_issues_title)
                val packagesTitle = stringResource(R.string.settings_about_packages_title)
                val redditTitle = stringResource(R.string.settings_about_reddit_title)
                val supportTitle = stringResource(R.string.settings_about_support_email_title)
                val donateTitle = stringResource(R.string.settings_about_donate_upstream_title)
                val items = buildList {
                    add(
                        AboutLinkItem(
                            title = wikiTitle,
                            summary = stringResource(R.string.settings_about_wiki_summary),
                            url = TermuxConstants.TERMUX_WIKI_URL,
                            icon = Icons.Rounded.MenuBook,
                            testTag = "about_wiki_link",
                        ),
                    )
                    add(
                        AboutLinkItem(
                            title = upstreamTitle,
                            summary = TermuxConstants.TERMUX_GITHUB_REPO_URL,
                            url = TermuxConstants.TERMUX_GITHUB_REPO_URL,
                            icon = Icons.Rounded.Terminal,
                        ),
                    )
                    add(
                        AboutLinkItem(
                            title = upstreamIssuesTitle,
                            summary = TermuxConstants.TERMUX_GITHUB_ISSUES_REPO_URL,
                            url = TermuxConstants.TERMUX_GITHUB_ISSUES_REPO_URL,
                            icon = Icons.Rounded.BugReport,
                            testTag = "about_upstream_issues_link",
                        ),
                    )
                    add(
                        AboutLinkItem(
                            title = packagesTitle,
                            summary = TermuxConstants.TERMUX_PACKAGES_GITHUB_REPO_URL,
                            url = TermuxConstants.TERMUX_PACKAGES_GITHUB_REPO_URL,
                            icon = Icons.Rounded.Description,
                        ),
                    )
                    add(
                        AboutLinkItem(
                            title = redditTitle,
                            summary = stringResource(R.string.settings_about_reddit_summary),
                            url = TermuxConstants.TERMUX_REDDIT_SUBREDDIT_URL,
                            icon = Icons.Rounded.Forum,
                        ),
                    )
                    add(
                        AboutLinkItem(
                            title = supportTitle,
                            summary = stringResource(R.string.settings_about_support_email_summary),
                            url = TermuxConstants.TERMUX_SUPPORT_EMAIL_MAILTO_URL,
                            icon = Icons.Rounded.Email,
                        ),
                    )
                    if (termuxState.showDonate) {
                        add(
                            AboutLinkItem(
                                title = donateTitle,
                                summary = stringResource(R.string.settings_donate_summary),
                                url = TermuxConstants.TERMUX_DONATE_URL,
                                icon = Icons.Rounded.Favorite,
                            ),
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_about_group_upstream))
                    SettingsSegmentedGroup {
                        items.forEachIndexed { idx, item ->
                            ExpressiveActionRow(
                                title = item.title,
                                summary = item.summary,
                                icon = item.icon,
                                index = idx,
                                count = items.size,
                                onClick = { onOpenUrl(item.title, item.url) },
                                modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSystemReportDialog) {
        SystemReportDialog(
            aboutInfo = aboutInfo,
            fallbackReport = fallbackReport,
            onCopy = onCopyText,
            onShare = onShareReport,
            onDismiss = { showSystemReportDialog = false },
        )
    }
}

@Composable
private fun AboutHeroCard(
    variantName: String,
    versionLabel: String,
    abiLabel: String?,
    onCopyReport: () -> Unit,
    onShareReport: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_hero_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ">_",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = variantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HeroMetaPill(text = versionLabel, highlighted = true)
                        HeroMetaPill(
                            text = if (!abiLabel.isNullOrBlank()) "$abiLabel · GPLv3" else "GPLv3",
                            highlighted = false,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_about_hero_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onCopyReport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("about_copy_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_copy_report),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onShareReport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("about_share_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_share_report),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetaPill(
    text: String,
    highlighted: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) {
            ModernTermuxTheme.extendedColors.cyanGlow
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Text(
            text = text,
            style = MonospaceValueTextStyle,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SystemReportDialog(
    aboutInfo: TermuxAboutInfo?,
    fallbackReport: String,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showMarkdown by remember { mutableStateOf(false) }
    val reportText = aboutInfo?.markdownReport ?: fallbackReport

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("about_system_report_dialog"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = stringResource(R.string.settings_about_report_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportModePill(
                        label = stringResource(R.string.settings_about_report_mode_summary),
                        selected = !showMarkdown,
                        onClick = { showMarkdown = false },
                    )
                    ReportModePill(
                        label = stringResource(R.string.settings_about_report_mode_markdown),
                        selected = showMarkdown,
                        onClick = { showMarkdown = true },
                    )
                }

                if (showMarkdown || aboutInfo == null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = reportText,
                                style = MonospaceValueTextStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp),
                            )
                        }
                    }
                } else {
                    val context = LocalContext.current
                    val copiedHint = stringResource(R.string.settings_about_report_copied_item)
                    val rows = listOfNotNull(
                        stringResource(R.string.settings_about_field_variant) to aboutInfo.variantName,
                        stringResource(R.string.settings_about_field_package) to aboutInfo.packageName,
                        stringResource(R.string.settings_about_field_version) to "${aboutInfo.versionName} (${aboutInfo.versionCode})",
                        stringResource(R.string.settings_about_field_bootstrap) to "${aboutInfo.packageVariant} / ${aboutInfo.packageManager}",
                        stringResource(R.string.settings_about_field_uid_sdk) to "${aboutInfo.uid} / API ${aboutInfo.targetSdk}",
                        stringResource(R.string.settings_about_field_build_type) to if (aboutInfo.debuggable) "debug" else "release",
                        stringResource(R.string.settings_about_field_apk_release) to aboutInfo.apkRelease,
                        stringResource(R.string.settings_about_field_sha256) to aboutInfo.signingSha256,
                        stringResource(R.string.settings_about_field_selinux) to aboutInfo.seContext,
                        stringResource(R.string.settings_about_field_device) to aboutInfo.deviceModel,
                        stringResource(R.string.settings_about_field_android) to aboutInfo.androidVersion,
                        aboutInfo.securityPatch?.let {
                            stringResource(R.string.settings_about_field_security_patch) to it
                        },
                        stringResource(R.string.settings_about_field_abis) to aboutInfo.abiList,
                        aboutInfo.filesDirWarning?.let {
                            stringResource(R.string.settings_about_field_files_warning) to it
                        },
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        rows.forEachIndexed { idx, (label, value) ->
                            val isWarning = aboutInfo.filesDirWarning != null && value == aboutInfo.filesDirWarning
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopy(value)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isWarning) {
                                        ModernTermuxTheme.extendedColors.warning
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                                Text(
                                    text = value,
                                    style = MonospaceValueTextStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (idx < rows.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${context.packageName} · $copiedHint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onCopy(reportText) }) {
                    Text(stringResource(R.string.settings_about_copy_report))
                }
                TextButton(onClick = { onShare(reportText) }) {
                    Text(stringResource(R.string.settings_about_share_report))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_about_close))
            }
        },
    )
}

@Composable
private fun ReportModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
