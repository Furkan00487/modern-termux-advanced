package com.termux.app.settings.navigation

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.termux.app.activities.SettingsActivity
import com.termux.app.activities.SettingsSection
import com.termux.app.settings.data.PluginKind
import com.termux.app.settings.data.SearchControlType
import com.termux.app.settings.data.SettingsSearchIndex
import com.termux.app.settings.data.TermuxSettingsRepository
import com.termux.app.settings.data.X11SettingsRepository
import com.termux.app.settings.screens.SettingsDetailPlaceholderPane
import com.termux.app.settings.screens.SettingsRootListPane
import com.termux.app.settings.screens.TermuxDebuggingScreen
import com.termux.app.settings.screens.TermuxPluginScreen
import com.termux.app.settings.screens.TermuxTerminalIOScreen
import com.termux.app.settings.screens.TermuxTerminalViewScreen
import com.termux.app.settings.screens.X11ExtraKeysBarScreen
import com.termux.app.settings.screens.X11KeyboardScreen
import com.termux.app.settings.screens.X11OtherScreen
import com.termux.app.settings.screens.X11OutputScreen
import com.termux.app.settings.screens.X11PointerScreen
import com.termux.app.settings.screens.X11UserActionsScreen
import com.termux.x11.X11PreferenceBridge
import com.termux.x11.R as X11R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsNavHost(
    launchTarget: SettingsActivity.SettingsLaunchTarget,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val termuxRepo = remember(context) { TermuxSettingsRepository(context.applicationContext) }
    val x11Repo = remember(context, launchTarget.displayId) {
        X11SettingsRepository(context.applicationContext, launchTarget.displayId)
    }

    val termuxState by termuxRepo.state.collectAsStateWithLifecycle()
    val x11State by x11Repo.state.collectAsStateWithLifecycle()

    LaunchedEffect(termuxRepo, x11Repo, context) {
        val localized = X11PreferenceBridge.resolveLocalizedContext(context, launchTarget.displayId)
        termuxRepo.refreshDynamicAvailability(localized)
        x11Repo.refresh(localized)
    }

    DisposableEffect(lifecycleOwner, x11Repo, termuxRepo, context) {
        x11Repo.startObserving()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val localized = X11PreferenceBridge.resolveLocalizedContext(context, launchTarget.displayId)
                x11Repo.refresh(localized)
                scope.launch {
                    termuxRepo.refreshDynamicAvailability(localized)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            x11Repo.stopObserving()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        val localized = X11PreferenceBridge.resolveLocalizedContext(context, launchTarget.displayId)
        x11Repo.refresh(localized)
    }

    var selectedSection by rememberSaveable(launchTarget.section) {
        mutableStateOf(launchTarget.section)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val initialDetailRoute: SettingsRoute? = remember(launchTarget) {
        when {
            launchTarget.subscreen == "ekbar" -> SettingsRoute.X11ExtraKeysBar
            launchTarget.subscreen == "userActions" -> SettingsRoute.X11UserActions
            launchTarget.subscreen == "output" -> SettingsRoute.X11Output
            launchTarget.subscreen == "pointer" -> SettingsRoute.X11Pointer
            launchTarget.subscreen == "kbd" -> SettingsRoute.X11Keyboard
            launchTarget.subscreen == "other" -> SettingsRoute.X11Other
            else -> null
        }
    }

    val backStack: NavBackStack<NavKey> = if (initialDetailRoute != null) {
        rememberNavBackStack(SettingsRoute.RootList, initialDetailRoute)
    } else {
        rememberNavBackStack(SettingsRoute.RootList)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    // Intercept back gestures while search text is active before popping nav entries.
    val searchBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = searchBackState,
        isBackEnabled = searchQuery.isNotEmpty(),
        onBackCompleted = { searchQuery = "" },
    )

    val searchResults = remember(searchQuery, termuxState, x11State, context) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            SettingsSearchIndex.search(searchQuery, context, termuxState, x11State)
        }
    }

    fun copyAndNotify(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(text, text))
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(X11R.string.lorie_copied_to_clipboard))
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun handleBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onNavigateUp()
        }
    }

    val currentDetailRoute = backStack.lastOrNull { it != SettingsRoute.RootList } as? SettingsRoute

    Box(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = ::handleBack,
            sceneStrategies = listOf(listDetailStrategy),
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(260)) +
                    fadeIn(animationSpec = tween(220))) togetherWith
                    (slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(220)) +
                        fadeOut(animationSpec = tween(160)))
            },
            popTransitionSpec = {
                (slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(240)) +
                    fadeIn(animationSpec = tween(200))) togetherWith
                    (slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(220)) +
                        fadeOut(animationSpec = tween(160)))
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(initialOffsetX = { -it / 6 }) + fadeIn()) togetherWith
                    (slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut())
            },
            entryProvider = entryProvider {
                entry<SettingsRoute.RootList>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { SettingsDetailPlaceholderPane() },
                    ),
                ) {
                    SettingsRootListPane(
                        selectedSection = selectedSection,
                        currentDetailRoute = currentDetailRoute,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        termuxState = termuxState,
                        x11State = x11State,
                        onSelectSection = { section ->
                            if (selectedSection != section) {
                                selectedSection = section
                                // Keep list/detail separated cleanly per section when switching tabs
                                backStack.clearToRoot()
                            }
                        },
                        onSearchQueryChange = { searchQuery = it },
                        onNavigateToDetail = { route -> backStack.replaceDetail(route) },
                        onSearchResultClick = { entry ->
                            selectedSection = entry.section
                            when (entry.key) {
                                "about" -> scope.launch { termuxRepo.openAboutReport(context) }
                                "donate" -> termuxRepo.openDonate(context)
                                "version" -> copyAndNotify(x11State.x11VersionName)
                                "requestNotificationPermission" -> requestNotifications()
                                "enableAccessibilityService" -> openAccessibilitySettings()
                                else -> backStack.replaceDetail(entry.targetRoute)
                            }
                        },
                        onSearchResultToggleSwitch = { entry, nextValue ->
                            when (entry.key) {
                                "terminal_view_key_logging_enabled" -> termuxRepo.setTerminalViewKeyLogging(nextValue)
                                "plugin_error_notifications_enabled" -> termuxRepo.setPluginErrorNotifications(nextValue)
                                "crash_report_notifications_enabled" -> termuxRepo.setCrashReportNotifications(nextValue)
                                "soft_keyboard_enabled" -> termuxRepo.setSoftKeyboardEnabled(nextValue)
                                "soft_keyboard_enabled_only_if_no_hardware" -> termuxRepo.setSoftKeyboardOnlyIfNoHardware(nextValue)
                                "terminal_margin_adjustment" -> termuxRepo.setTerminalMarginAdjustment(nextValue)
                                "plugin_float_key_logging" -> termuxRepo.setPluginKeyLogging(PluginKind.FLOAT, nextValue)
                                "enableAccessibilityServiceAutomatically" -> backStack.replaceDetail(entry.targetRoute)
                                else -> x11Repo.setBoolean(entry.key, nextValue)
                            }
                        },
                        onOpenAbout = {
                            scope.launch { termuxRepo.openAboutReport(context) }
                        },
                        onOpenDonate = { termuxRepo.openDonate(context) },
                        onCopyX11Version = ::copyAndNotify,
                        onNavigateUp = onNavigateUp,
                    )
                }

                entry<SettingsRoute.TermuxDebugging>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    TermuxDebuggingScreen(
                        state = termuxState,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetLogLevel = termuxRepo::setLogLevel,
                        onSetKeyLogging = termuxRepo::setTerminalViewKeyLogging,
                        onSetPluginErrorNotifications = termuxRepo::setPluginErrorNotifications,
                        onSetCrashReportNotifications = termuxRepo::setCrashReportNotifications,
                    )
                }

                entry<SettingsRoute.TermuxTerminalIO>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    TermuxTerminalIOScreen(
                        state = termuxState,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetSoftKeyboardEnabled = termuxRepo::setSoftKeyboardEnabled,
                        onSetSoftKeyboardOnlyIfNoHardware = termuxRepo::setSoftKeyboardOnlyIfNoHardware,
                    )
                }

                entry<SettingsRoute.TermuxTerminalView>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    TermuxTerminalViewScreen(
                        state = termuxState,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetTerminalMarginAdjustment = termuxRepo::setTerminalMarginAdjustment,
                    )
                }

                entry<SettingsRoute.TermuxPlugin>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) { route ->
                    TermuxPluginScreen(
                        pluginKind = route.plugin,
                        state = termuxState,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetPluginLogLevel = termuxRepo::setPluginLogLevel,
                        onSetPluginKeyLogging = termuxRepo::setPluginKeyLogging,
                    )
                }

                entry<SettingsRoute.X11Output>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11OutputScreen(
                        state = x11State,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetBoolean = x11Repo::setBoolean,
                        onSetList = x11Repo::setListOrString,
                        onSetDisplayScale = x11Repo::setDisplayScale,
                        onSetCustomResolution = x11Repo::setCustomResolution,
                    )
                }

                entry<SettingsRoute.X11Pointer>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11PointerScreen(
                        state = x11State,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetBoolean = x11Repo::setBoolean,
                        onSetList = x11Repo::setListOrString,
                        onSetInt = x11Repo::setInt,
                    )
                }

                entry<SettingsRoute.X11Keyboard>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11KeyboardScreen(
                        state = x11State,
                        packageName = context.packageName,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onOpenEkBar = { backStack.pushChildDetail(SettingsRoute.X11ExtraKeysBar) },
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onSetBoolean = x11Repo::setBoolean,
                        onSetAccessibilityAuto = x11Repo::setAccessibilityAuto,
                        onCopyCommand = ::copyAndNotify,
                    )
                }

                entry<SettingsRoute.X11ExtraKeysBar>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11ExtraKeysBarScreen(
                        state = x11State,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetBoolean = x11Repo::setBoolean,
                        onSetList = x11Repo::setListOrString,
                        onSetInt = x11Repo::setInt,
                        onSaveExtraKeysConfig = x11Repo::setExtraKeysConfig,
                        onResetExtraKeysConfig = x11Repo::resetExtraKeysConfig,
                    )
                }

                entry<SettingsRoute.X11Other>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11OtherScreen(
                        state = x11State,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetBoolean = x11Repo::setBoolean,
                        onRequestNotificationPermission = ::requestNotifications,
                        onOpenUserActions = { backStack.pushChildDetail(SettingsRoute.X11UserActions) },
                    )
                }

                entry<SettingsRoute.X11UserActions>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) {
                    X11UserActionsScreen(
                        state = x11State,
                        showBackButton = backStack.size > 1,
                        onBack = ::handleBack,
                        onSetList = x11Repo::setListOrString,
                    )
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                actionColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun NavBackStack<NavKey>.replaceDetail(detail: SettingsRoute) {
    removeIf { it != SettingsRoute.RootList }
    add(detail)
}

private fun NavBackStack<NavKey>.pushChildDetail(detail: SettingsRoute) {
    if (lastOrNull() != detail) {
        add(detail)
    }
}

private fun NavBackStack<NavKey>.clearToRoot() {
    removeIf { it != SettingsRoute.RootList }
}
