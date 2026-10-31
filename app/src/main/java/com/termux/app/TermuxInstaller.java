package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.system.Os;
import android.util.Pair;
import android.view.WindowManager;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.shared.file.FileUtils;
import com.termux.shared.settings.preferences.SharedPreferenceUtils;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.runner.app.AppShell;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.markdown.MarkdownUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR_PATH;

/**
 * Install the Termux bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX directory below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging directory, $STAGING_PREFIX, is cleared if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
final class TermuxInstaller {

    private static final String LOG_TAG = "TermuxInstaller";

    /** Performs bootstrap setup if necessary. */
    /**
     * Ensure that the termux-setup-microphone script exists in $PREFIX/bin.
     * This automatically handles upgrades where bootstrap.zip is not re-extracted.
     */
    static void ensureSetupMicrophoneScriptExists() {
        try {
            File binDir = TermuxConstants.TERMUX_BIN_PREFIX_DIR;
            if (!binDir.exists() || !binDir.isDirectory()) return;

            File micScript = new File(binDir, "termux-setup-microphone");
            if (!micScript.exists()) {
                String script = "#!/data/data/com.termux/files/usr/bin/bash\n\n" +
                    "if [ \"$#\" != \"0\" ]; then\n" +
                    "\techo 'kullanım: termux-setup-microphone'\n" +
                    "\techo 'Mikrofon izninin verildiğinden emin olmak için argümansız çalıştırın'\n" +
                    "\texit\n" +
                    "fi\n\n" +
                    "case \"${TERMUX__USER_ID:-}\" in ''|*[!0-9]*|0[0-9]*) TERMUX__USER_ID=0;; esac\n\n" +
                    "am broadcast --user \"$TERMUX__USER_ID\" \\\n" +
                    "\t-a \"com.termux.app.request_microphone_permission\" \\\n" +
                    "\t \"com.termux\" > /dev/null\n";
                try (FileOutputStream outStream = new FileOutputStream(micScript)) {
                    outStream.write(script.getBytes(StandardCharsets.UTF_8));
                }
                //noinspection OctalInteger
                Os.chmod(micScript.getAbsolutePath(), 0700);
            }
        } catch (Exception ignored) {
        }
    }

    static String getModernTermuxMotdContent() {
        return "\033[1;34m╔════════════════════════════════════════════════════════╗\n" +
            "║              MODERN TERMUX'A HOŞ GELDİN!               ║\n" +
            "╚════════════════════════════════════════════════════════╝\033[0m\n\n" +
            " Paket aramak için:          \033[1;32mpkg search <sorgu>\033[0m\n" +
            " Paket kurmak için:          \033[1;32mpkg install <paket>\033[0m\n" +
            " Paketleri güncellemek için: \033[1;32mpkg upgrade\033[0m\n" +
            " Sistem bilgisi:             \033[1;32mtermux-info\033[0m\n\n" +
            " Topluluk & Destek: https://github.com/Furkan00487/modern-termux\n\n";
    }

    static void ensureModernTermuxMotdFile(File motdFile) {
        if (motdFile == null) return;
        try {
            boolean shouldWrite = false;
            if (!motdFile.exists()) {
                File parent = motdFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                shouldWrite = true;
            } else {
                byte[] bytes = new byte[(int) Math.min(motdFile.length(), 4096)];
                try (FileInputStream fis = new FileInputStream(motdFile)) {
                    int read = fis.read(bytes);
                    if (read > 0) {
                        String current = new String(bytes, 0, read, StandardCharsets.UTF_8);
                        if (current.contains("Welcome to Termux!") || current.contains("termux.dev") || !current.contains("MODERN TERMUX")) {
                            shouldWrite = true;
                        }
                    } else {
                        shouldWrite = true;
                    }
                }
            }

            if (shouldWrite) {
                try (FileOutputStream fos = new FileOutputStream(motdFile)) {
                    fos.write(getModernTermuxMotdContent().getBytes(StandardCharsets.UTF_8));
                }
                //noinspection OctalInteger
                Os.chmod(motdFile.getAbsolutePath(), 0644);
            }
        } catch (Exception ignored) {
        }
    }

    static void ensureModernTermuxMotdExists() {
        try {
            File etcDir = new File(TERMUX_PREFIX_DIR, "etc");
            if (!etcDir.exists() || !etcDir.isDirectory()) return;

            File motdFile = new File(etcDir, "motd");
            ensureModernTermuxMotdFile(motdFile);
        } catch (Exception ignored) {
        }
    }

    static String getTermuxMotdRefreshScriptContent() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n\n" +
            "# Modern Termux:MOTD eklentisinin varlık kontrolü (pm path ve sharedUserId veri dizini)\n" +
            "if ! /system/bin/pm path com.termux.motd >/dev/null 2>&1 && [ ! -d \"/data/data/com.termux.motd\" ]; then\n" +
            "    echo -e \"\\033[1;31m[HATA] Modern Termux:MOTD eklentisi (com.termux.motd) kurulu değil!\\033[0m\"\n" +
            "    echo -e \"\\033[0;33mDinamik MOTD ekranını kullanmak için lütfen Modern Termux:MOTD eklentisini yükleyin.\\033[0m\"\n" +
            "    exit 1\n" +
            "fi\n\n" +
            "DYN_MOTD_SCRIPT=\"/data/data/com.termux/files/usr/etc/motd.d/dyn-motd.sh\"\n" +
            "if [ -f \"$DYN_MOTD_SCRIPT\" ]; then\n" +
            "    bash \"$DYN_MOTD_SCRIPT\"\n" +
            "else\n" +
            "    echo -e \"\\033[1;36m[Modern Termux:MOTD]\\033[0m Eklenti kurulu ancak henüz kaydedilmiş bir dinamik şablon bulunamadı.\"\n" +
            "    echo -e \"\\033[0;32mLütfen Modern Termux:MOTD uygulamasını açarak dilediğiniz temayı uygulayın.\\033[0m\"\n" +
            "fi\n";
    }

    static void ensureTermuxMotdRefreshScriptExists() {
        try {
            File binDir = TermuxConstants.TERMUX_BIN_PREFIX_DIR;
            if (!binDir.exists() || !binDir.isDirectory()) return;

            File refreshScript = new File(binDir, "termux-motd-refresh");
            try (FileOutputStream outStream = new FileOutputStream(refreshScript)) {
                outStream.write(getTermuxMotdRefreshScriptContent().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection OctalInteger
            Os.chmod(refreshScript.getAbsolutePath(), 0700);
        } catch (Exception ignored) {
        }
    }

    static String getTermuxX11ScriptContent() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n" +
            "if [ ! -e /system/bin/getprop ] || [ ! -e /system/bin/app_process ]; then\n" +
            "  echo \"Başsız (headless) sistem algılandı. Termux:X11 çalışmak için standart Android gerektirir.\"\n" +
            "  exit 1\n" +
            "fi\n" +
            "if [ \"$(getprop ro.build.version.sdk)\" -lt \"24\" ]; then\n" +
            "  echo \"Termux:X11 en az Android 7 veya daha yeni bir sürüm gerektirir.\"\n" +
            "  exit 1\n" +
            "fi\n" +
            "[ -z \"${LD_LIBRARY_PATH+x}\" ] || export XSTARTUP_LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH\"\n" +
            "[ -z \"${LD_PRELOAD+x}\" ] || export XSTARTUP_LD_PRELOAD=\"$LD_PRELOAD\"\n" +
            "[ -z \"${CLASSPATH+x}\" ] || export XSTARTUP_CLASSPATH=\"$CLASSPATH\"\n" +
            "APK_PATH=\"/data/data/com.termux/files/usr/libexec/termux-x11/base.apk\"\n" +
            "if [ ! -e \"$APK_PATH\" ]; then\n" +
            "  APK_PATH=\"$(/system/bin/pm path com.termux 2>/dev/null | head -n 1 | cut -d: -f2)\"\n" +
            "fi\n" +
            "export CLASSPATH=\"$APK_PATH\"\n" +
            "unset LD_LIBRARY_PATH LD_PRELOAD\n" +
            "[ -n \"$(trap -p USR1)\" ] && export TERMUX_X11_NOTIFY_PARENT=1\n" +
            "exec /system/bin/app_process -Xnoimage-dex2oat / --nice-name=\"termux-x11 com.termux $*\" com.termux.x11.CmdEntryPoint \"$@\"\n";
    }

    static String getTermuxX11PreferenceScriptContent() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n" +
            "if [ ! -e /system/bin/getprop ] || [ ! -e /system/bin/app_process ]; then\n" +
            "  echo \"Başsız (headless) sistem algılandı. Termux:X11 çalışmak için standart Android gerektirir.\"\n" +
            "  exit 1\n" +
            "fi\n" +
            "if [ \"$(getprop ro.build.version.sdk)\" -lt \"24\" ]; then\n" +
            "  echo \"Termux:X11 en az Android 7 veya daha yeni bir sürüm gerektirir.\"\n" +
            "  exit 1\n" +
            "fi\n" +
            "if [ \"$(getprop ro.build.version.sdk)\" -ge \"34\" ]; then\n" +
            "  unset LD_LIBRARY_PATH LD_PRELOAD\n" +
            "  APK_PATH=\"/data/data/com.termux/files/usr/libexec/termux-x11/base.apk\"\n" +
            "  if [ ! -e \"$APK_PATH\" ]; then\n" +
            "    APK_PATH=\"$(/system/bin/pm path com.termux 2>/dev/null | head -n 1 | cut -d: -f2)\"\n" +
            "  fi\n" +
            "  export CLASSPATH=\"$APK_PATH\"\n" +
            "  exec /system/bin/app_process -Xnoimage-dex2oat / com.termux.x11.LoriePreferences\\$Receiver \"$@\"\n" +
            "fi\n\n" +
            "COMMAND=(\"am\" \"broadcast\" \"-a\" \"com.termux.x11.CHANGE_PREFERENCE\" \"-p\" \"com.termux\")\n" +
            "help() {\n" +
            "  echo \"$0 [list] {key:value} [{key2:value2}]...\"\n" +
            "  exit 1\n" +
            "}\n\n" +
            "list() {\n" +
            "  output=\"$(\"${COMMAND[@]}\" -e list null 2>&1)\"\n" +
            "  result=$(sed -n '/result=[0-9]*/{s/.*result=\\([0-9]*\\).*/\\1/p;q;}' <<< \"$output\")\n" +
            "  if [[ \"$result\" == \"0\" ]]; then\n" +
            "    echo \"Bir şeyler ters gitti.\"\n" +
            "    exit 1\n" +
            "  fi\n" +
            "  if [[ \"$result\" == \"2\" ]] || [[ \"$result\" == \"4\" ]]; then\n" +
            "    echo \"$(echo \"$output\" | sed -z 's/.*data=\"\\([^\"]*\\)*/\\1/' | sed '${s/\"$//}')\"\n" +
            "    exit 0\n" +
            "  fi\n" +
            "  echo \"list: Beklenmeyen sonuç $result\"\n" +
            "  echo \"$output\"\n" +
            "  exit 1\n" +
            "}\n\n" +
            "if [ $# -eq 0 ]; then\n" +
            "  if [ -t 0 ]; then\n" +
            "    help\n" +
            "  else\n" +
            "    OLD_LENGTH=\"${#COMMAND[@]}\"\n" +
            "    while IFS= read -r line || [[ -n \"$line\" ]]; do\n" +
            "      [ -z \"$line\" ] && continue\n" +
            "      key=$(echo \"$line\" | sed -E 's/^\"([^\"]+)\".*/\\1/')\n" +
            "      value=$(echo \"$line\" | sed -E 's/^\"[^\"]+\"=\"(.*)\"/\\1/')\n" +
            "      COMMAND+=(\"-e\" \"$key\" \"$value\")\n" +
            "    done\n" +
            "    if [[ \"$OLD_LENGTH\" == \"${#COMMAND[@]}\" ]]; then\n" +
            "      help\n" +
            "    fi\n" +
            "  fi\n" +
            "fi\n\n" +
            "while [[ $# -gt 0 ]]; do\n" +
            "  case $1 in\n" +
            "    list) list;;\n" +
            "    *:*) COMMAND+=(\"-e\" \"${1%%:*}\" \"${1#*:}\");;\n" +
            "    *) echo \"Tanınmayan seçenek $1\"; help\n" +
            "  esac\n" +
            "  shift\n" +
            "done\n\n" +
            "output=\"$(\"${COMMAND[@]}\")\"\n" +
            "result=$(sed -n '/result=[0-9]*/{s/.*result=\\([0-9]*\\).*/\\1/p;q;}' <<< \"$output\")\n" +
            "if [[ \"$result\" == \"0\" ]]; then\n" +
            "  echo \"Bir şeyler ters gitti.\"\n" +
            "  exit 1\n" +
            "fi\n\n" +
            "if [[ \"$result\" == \"2\" ]] || [[ \"$result\" == \"4\" ]]; then\n" +
            "  echo \"$(echo \"$output\" | sed -z 's/.*data=\"\\([^\"]*\\)*/\\1/' | sed '${s/\"$//}')\"\n" +
            "  exit 0\n" +
            "fi\n\n" +
            "echo \"set: Beklenmeyen sonuç $result\"\n" +
            "echo \"$output\"\n" +
            "exit 1\n";
    }

    static String getTermuxX11EnvScriptContent() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n" +
            "# Modern Termux: X11 GPU Donanım Hızlandırma Ortam Sarmalayıcısı\n" +
            "HW_ACCEL_FLAG=\"/data/data/com.termux/files/usr/tmp/.lorie_hwaccel\"\n" +
            "USE_GPU=1\n" +
            "if [ -f \"$HW_ACCEL_FLAG\" ] && [ \"$(cat \"$HW_ACCEL_FLAG\" 2>/dev/null)\" = \"0\" ]; then\n" +
            "  USE_GPU=0\n" +
            "fi\n\n" +
            "if [ \"$USE_GPU\" = \"1\" ] && [ -e /system/lib64/libvulkan.so ]; then\n" +
            "  export GALLIUM_DRIVER=zink\n" +
            "  export MESA_LOADER_DRIVER_OVERRIDE=zink\n" +
            "  export LIBGL_ALWAYS_SOFTWARE=0\n" +
            "  if [ -f /data/data/com.termux/files/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json ]; then\n" +
            "    export VK_ICD_FILENAMES=/data/data/com.termux/files/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json\n" +
            "  fi\n" +
            "else\n" +
            "  export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe\n" +
            "  export LIBGL_ALWAYS_SOFTWARE=1\n" +
            "fi\n\n" +
            "if [ $# -gt 0 ]; then\n" +
            "  exec \"$@\"\n" +
            "fi\n";
    }

    static String getModernTermuxX11ProfileContent() {
        return "# Modern Termux: Otomatik X11 GPU Donanım Hızlandırma Enjeksiyonu\n" +
            "HW_ACCEL_FLAG=\"/data/data/com.termux/files/usr/tmp/.lorie_hwaccel\"\n" +
            "if [ -n \"$DISPLAY\" ] || [ -S /data/data/com.termux/files/usr/tmp/.X11-unix/X0 ]; then\n" +
            "  USE_GPU=1\n" +
            "  if [ -f \"$HW_ACCEL_FLAG\" ] && [ \"$(cat \"$HW_ACCEL_FLAG\" 2>/dev/null)\" = \"0\" ]; then\n" +
            "    USE_GPU=0\n" +
            "  fi\n" +
            "  if [ \"$USE_GPU\" = \"1\" ] && [ -e /system/lib64/libvulkan.so ]; then\n" +
            "    export GALLIUM_DRIVER=zink\n" +
            "    export MESA_LOADER_DRIVER_OVERRIDE=zink\n" +
            "    export LIBGL_ALWAYS_SOFTWARE=0\n" +
            "  fi\n" +
            "fi\n";
    }

    static void ensureTermuxX11FilesExist(Context context) {
        try {
            File binDir = TermuxConstants.TERMUX_BIN_PREFIX_DIR;
            if (!binDir.exists() || !binDir.isDirectory()) return;

            File x11Script = new File(binDir, "termux-x11");
            try (FileOutputStream outStream = new FileOutputStream(x11Script)) {
                outStream.write(getTermuxX11ScriptContent().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection OctalInteger
            Os.chmod(x11Script.getAbsolutePath(), 0700);

            File x11PrefScript = new File(binDir, "termux-x11-preference");
            try (FileOutputStream outStream = new FileOutputStream(x11PrefScript)) {
                outStream.write(getTermuxX11PreferenceScriptContent().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection OctalInteger
            Os.chmod(x11PrefScript.getAbsolutePath(), 0700);

            File x11EnvScript = new File(binDir, "termux-x11-env");
            try (FileOutputStream outStream = new FileOutputStream(x11EnvScript)) {
                outStream.write(getTermuxX11EnvScriptContent().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection OctalInteger
            Os.chmod(x11EnvScript.getAbsolutePath(), 0700);

            File profileDir = new File(TERMUX_PREFIX_DIR, "etc/profile.d");
            if (!profileDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                profileDir.mkdirs();
            }
            File x11ProfileScript = new File(profileDir, "modern-termux-x11-env.sh");
            try (FileOutputStream outStream = new FileOutputStream(x11ProfileScript)) {
                outStream.write(getModernTermuxX11ProfileContent().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection OctalInteger
            Os.chmod(x11ProfileScript.getAbsolutePath(), 0644);

            if (context != null && context.getApplicationInfo() != null) {
                File libexecDir = new File(TERMUX_PREFIX_DIR, "libexec/termux-x11");
                if (!libexecDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    libexecDir.mkdirs();
                }

                File apkSymlink = new File(libexecDir, "base.apk");
                //noinspection ResultOfMethodCallIgnored
                apkSymlink.delete();
                if (context.getApplicationInfo().sourceDir != null) {
                    Os.symlink(context.getApplicationInfo().sourceDir, apkSymlink.getAbsolutePath());
                }

                File libSymlink = new File(libexecDir, "libXlorie.so");
                //noinspection ResultOfMethodCallIgnored
                libSymlink.delete();
                if (context.getApplicationInfo().nativeLibraryDir != null) {
                    Os.symlink(context.getApplicationInfo().nativeLibraryDir + "/libXlorie.so", libSymlink.getAbsolutePath());
                }

                File xkbDir = new File(TERMUX_PREFIX_DIR, "share/xkeyboard-config-2");
                File xkbLegacyDir = new File(TERMUX_PREFIX_DIR, "share/X11/xkb");
                if (!xkbDir.exists() && !xkbLegacyDir.exists() && context.getAssets() != null) {
                    try (ZipInputStream zis = new ZipInputStream(context.getAssets().open("xkb.zip"))) {
                        byte[] buf = new byte[8192];
                        ZipEntry entry;
                        String canonicalPrefix = TERMUX_PREFIX_DIR.getCanonicalPath();
                        while ((entry = zis.getNextEntry()) != null) {
                            File outFile = new File(TERMUX_PREFIX_DIR, entry.getName());
                            if (!outFile.getCanonicalPath().startsWith(canonicalPrefix + File.separator)) {
                                continue;
                            }
                            if (entry.isDirectory()) {
                                //noinspection ResultOfMethodCallIgnored
                                outFile.mkdirs();
                            } else {
                                File parent = outFile.getParentFile();
                                if (parent != null && !parent.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    parent.mkdirs();
                                }
                                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                    int len;
                                    while ((len = zis.read(buf)) != -1) {
                                        fos.write(buf, 0, len);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        String bootstrapErrorMessage;
        Error filesDirectoryAccessibleError;

        // This will also call Context.getFilesDir(), which should ensure that termux files directory
        // is created if it does not already exist
        filesDirectoryAccessibleError = TermuxFileUtils.isTermuxFilesDirectoryAccessible(activity, true, true);
        boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

        // Termux can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !PackageUtils.isCurrentUserThePrimaryUser(activity)) {
            bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_not_primary_user_message,
                MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            Logger.logError(LOG_TAG, "isFilesDirectoryAccessible: " + isFilesDirectoryAccessible);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage);
            return;
        }

        if (!isFilesDirectoryAccessible) {
            bootstrapErrorMessage = Error.getMinimalErrorString(filesDirectoryAccessibleError);
            //noinspection SdCardPath
            if (PackageUtils.isAppInstalledOnExternalStorage(activity) &&
                !TermuxConstants.TERMUX_FILES_DIR_PATH.equals(activity.getFilesDir().getAbsolutePath().replaceAll("^/data/user/0/", "/data/data/"))) {
                bootstrapErrorMessage += "\n\n" + activity.getString(R.string.bootstrap_error_installed_on_portable_sd,
                    MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            }

            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.showMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage, null);
            return;
        }

        if (!checkIfMinOrMaxSdkVersionIsIncompatible(activity,
                BuildConfig.TERMUX_APP__BOOTSTRAP_MIN_SDK, BuildConfig.TERMUX_APP__BOOTSTRAP_MIN_RELEASE,
                BuildConfig.TERMUX_APP__BOOTSTRAP_MAX_SDK, BuildConfig.TERMUX_APP__BOOTSTRAP_MAX_RELEASE)) {
            return;
        }

        // If prefix directory exists, even if its a symlink to a valid directory and symlink is not broken/dangling
        if (FileUtils.directoryFileExists(TERMUX_PREFIX_DIR_PATH, true)) {
            if (TermuxFileUtils.isTermuxPrefixDirectoryEmpty()) {
                Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" exists but is empty or only contains specific unimportant files.");
            } else {
                TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity);
                SharedPreferences sharedPreferences = preferences != null ? preferences.getSharedPreferences() : null;
                int lastVersionCode = SharedPreferenceUtils.getInt(sharedPreferences, TERMUX_APP.KEY_LAST_BOOTSTRAP_VERSION_CODE, 0);
                int currentVersionCode = BuildConfig.VERSION_CODE;

                if (lastVersionCode < currentVersionCode) {
                    final ProgressDialog updateProgress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_updater_body), true, false);
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Logger.logInfo(LOG_TAG, "Updating " + TermuxConstants.TERMUX_APP_NAME + " bootstrap components.");
                                ensureSetupMicrophoneScriptExists();
                                ensureTermuxX11FilesExist(activity);
                                ensureModernTermuxMotdExists();
                                ensureTermuxMotdRefreshScriptExists();
                                SharedPreferenceUtils.setInt(sharedPreferences, TERMUX_APP.KEY_LAST_BOOTSTRAP_VERSION_CODE, currentVersionCode, true);
                            } catch (Exception e) {
                                Logger.logStackTraceWithMessage(LOG_TAG, "Error updating bootstrap components", e);
                            } finally {
                                activity.runOnUiThread(() -> {
                                    try {
                                        updateProgress.dismiss();
                                    } catch (RuntimeException ignored) {
                                    }
                                    whenDone.run();
                                });
                            }
                        }
                    }.start();
                    return;
                }

                ensureSetupMicrophoneScriptExists();
                ensureTermuxX11FilesExist(activity);
                ensureModernTermuxMotdExists();
                ensureTermuxMotdRefreshScriptExists();
                whenDone.run();
                return;
            }
        } else if (FileUtils.fileExists(TERMUX_PREFIX_DIR_PATH, false)) {
            Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" does not exist but another file exists at its destination.");
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    Logger.logInfo(LOG_TAG, "Installing " + TermuxConstants.TERMUX_APP_NAME + " bootstrap packages.");

                    Error error;

                    // Delete prefix staging directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix staging directory", TERMUX_STAGING_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Delete prefix directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix staging directory if it does not already exist and set required permissions
                    error = TermuxFileUtils.isTermuxPrefixStagingDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix directory if it does not already exist and set required permissions
                    error = TermuxFileUtils.isTermuxPrefixDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Extracting bootstrap zip to prefix staging directory \"" + TERMUX_STAGING_PREFIX_DIR_PATH + "\".");

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length != 2)
                                        throw new RuntimeException("Malformed symlink line: " + line);
                                    String oldPath = parts[0];
                                    String newPath = TERMUX_STAGING_PREFIX_DIR_PATH + "/" + parts[1];
                                    symlinks.add(Pair.create(oldPath, newPath));

                                    error = ensureDirectoryExists(new File(newPath).getParentFile());
                                    if (error != null) {
                                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                        return;
                                    }
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile = new File(TERMUX_STAGING_PREFIX_DIR, zipEntryName);

                                // Security check to prevent Zip Slip (path traversal) vulnerability
                                String canonicalDestDirPath = TERMUX_STAGING_PREFIX_DIR.getCanonicalPath();
                                String canonicalTargetFilePath = targetFile.getCanonicalPath();
                                if (!canonicalTargetFilePath.startsWith(canonicalDestDirPath + File.separator)) {
                                    throw new IOException("Zip entry is outside of target dir: " + zipEntryName);
                                }

                                boolean isDirectory = zipEntry.isDirectory();

                                error = ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());
                                if (error != null) {
                                    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                    return;
                                }

                                if (!isDirectory) {
                                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            outStream.write(buffer, 0, readBytes);
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") ||
                                        zipEntryName.startsWith("lib/apt/apt-helper") || zipEntryName.startsWith("lib/apt/methods") ||
                                        zipEntryName.equals("etc/termux/bootstrap/termux-bootstrap-second-stage.sh")) {
                                        //noinspection OctalInteger
                                        Os.chmod(targetFile.getAbsolutePath(), 0700);
                                    }
                                }
                            }
                        }
                    }

                    if (symlinks.isEmpty())
                        throw new RuntimeException("No SYMLINKS.txt encountered");
                    for (Pair<String, String> symlink : symlinks) {
                        Os.symlink(symlink.first, symlink.second);
                    }

                    Logger.logInfo(LOG_TAG, "Moving termux prefix staging to prefix directory.");

                    if (!TERMUX_STAGING_PREFIX_DIR.renameTo(TERMUX_PREFIX_DIR)) {
                        throw new RuntimeException("Moving termux prefix staging to prefix directory failed");
                    }

                    // Run Termux bootstrap second stage.
                    String termuxBootstrapSecondStageFile = TERMUX_PREFIX_DIR_PATH + "/etc/termux/bootstrap/termux-bootstrap-second-stage.sh";
                    if (!FileUtils.fileExists(termuxBootstrapSecondStageFile, false)) {
                        Logger.logInfo(LOG_TAG, "Not running Termux bootstrap second stage since script not found at \"" + termuxBootstrapSecondStageFile + "\" path.");
                    } else {
                        if (!FileUtils.fileExists(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", true)) {
                            Logger.logInfo(LOG_TAG, "Not running Termux bootstrap second stage since bash not found.");
                        }
                        Logger.logInfo(LOG_TAG, "Running Termux bootstrap second stage.");

                        ExecutionCommand executionCommand = new ExecutionCommand(-1,
                                termuxBootstrapSecondStageFile, null, null,
                                null, ExecutionCommand.Runner.APP_SHELL.getName(), false);
                        executionCommand.commandLabel = "Termux Bootstrap Second Stage Command";
                        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_NORMAL;
                        AppShell appShell = AppShell.execute(activity, executionCommand, null, new TermuxShellEnvironment(), null, true);
                        if (appShell == null || !executionCommand.isSuccessful() || executionCommand.resultData.exitCode != 0) {
                            // Generate debug report before deleting broken prefix directory to get `stat` info at time of failure.
                            showBootstrapErrorDialog(activity, whenDone, MarkdownUtils.getMarkdownCodeForString(executionCommand.toString(), true));

                            // Delete prefix directory as otherwise when app is restarted, the broken prefix directory would be used and logged into.
                            Logger.logInfo(LOG_TAG, "Deleting broken termux prefix.");
                            error = FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                            if (error != null)
                                Logger.logErrorExtended(LOG_TAG, error.toString());
                            return;
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Bootstrap packages installed successfully.");

                    ensureSetupMicrophoneScriptExists();
                    ensureTermuxX11FilesExist(activity);
                    ensureModernTermuxMotdExists();
                    ensureTermuxMotdRefreshScriptExists();

                    TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity);
                    SharedPreferences sharedPreferences = preferences != null ? preferences.getSharedPreferences() : null;
                    SharedPreferenceUtils.setInt(sharedPreferences, TERMUX_APP.KEY_LAST_BOOTSTRAP_VERSION_CODE, BuildConfig.VERSION_CODE, true);

                    // Recreate env file since termux prefix was wiped earlier
                    TermuxShellEnvironment.writeEnvironmentToFile(activity);

                    activity.runOnUiThread(whenDone);

                } catch (final Exception e) {
                    showBootstrapErrorDialog(activity, whenDone, Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)));

                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    public static boolean checkIfMinOrMaxSdkVersionIsIncompatible(Activity activity,
                                                                  Integer minSdk, String minRelease,
                                                                  Integer maxSdk, String maxRelease) {
        if (minSdk != null && minSdk > 0 && Build.VERSION.SDK_INT < minSdk) {
            String bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_apk_bootstrap_variant_min_sdk_incompatible,
                    MarkdownUtils.getMarkdownCodeForString(TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT.getName(), false),
                    MarkdownUtils.getMarkdownCodeForString(Build.VERSION.RELEASE, false),
                    Build.VERSION.SDK_INT,
                    MarkdownUtils.getMarkdownCodeForString(minRelease, false),
                    minSdk);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                    activity.getString(R.string.bootstrap_error_title),
                    bootstrapErrorMessage);
            return false;
        }

        if (maxSdk != null && maxSdk > 0 && Build.VERSION.SDK_INT > maxSdk) {
            String bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_apk_bootstrap_variant_max_sdk_incompatible,
                    MarkdownUtils.getMarkdownCodeForString(TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT.getName(), false),
                    MarkdownUtils.getMarkdownCodeForString(Build.VERSION.RELEASE, false),
                    Build.VERSION.SDK_INT,
                    MarkdownUtils.getMarkdownCodeForString(maxRelease, false),
                    maxSdk);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                    activity.getString(R.string.bootstrap_error_title),
                    bootstrapErrorMessage);
            return false;
        }

        return true;
    }

    public static void showBootstrapErrorDialog(Activity activity, Runnable whenDone, String message) {
        Logger.logErrorExtended(LOG_TAG, "Bootstrap Error:\n" + message);

        // Send a notification with the exception so that the user knows why bootstrap setup failed
        sendBootstrapCrashReportNotification(activity, message);

        activity.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_body)
                    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                        dialog.dismiss();
                        activity.finish();
                    })
                    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                        dialog.dismiss();
                        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                        TermuxInstaller.setupBootstrapIfNeeded(activity, whenDone);
                    }).show();
            } catch (WindowManager.BadTokenException e1) {
                // Activity already dismissed - ignore.
            }
        });
    }

    private static void sendBootstrapCrashReportNotification(Activity activity, String message) {
        final String title = TermuxConstants.TERMUX_APP_NAME + " Bootstrap Error";

        // Add info of all install Termux plugin apps as well since their target sdk or installation
        // on external/portable sd card can affect Termux app files directory access or exec.
        TermuxCrashUtils.sendCrashReportNotification(activity, LOG_TAG,
            title, null, "## " + title + "\n\n" + message + "\n\n" +
                TermuxUtils.getTermuxDebugMarkdownString(activity),
            true, false, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES, true);
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        final String title = TermuxConstants.TERMUX_APP_NAME + " Setup Storage Error";

        Logger.logInfo(LOG_TAG, "Setting up storage symlinks.");

        new Thread() {
            public void run() {
                try {
                    Error error;
                    File storageDir = TermuxConstants.TERMUX_STORAGE_HOME_DIR;

                    error = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (error != null) {
                        Logger.logErrorAndShowToast(context, LOG_TAG, error.getMessage());
                        Logger.logErrorExtended(LOG_TAG, "Setup Storage Error\n" + error.toString());
                        TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                            "## " + title + "\n\n" + Error.getErrorMarkdownString(error),
                            true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/shared, ~/storage/downloads, ~/storage/dcim, ~/storage/pictures, ~/storage/music and ~/storage/movies for directories in \"" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\".");

                    // Get primary storage root "/storage/emulated/0" symlink
                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    Os.symlink(documentsDir.getAbsolutePath(), new File(storageDir, "documents").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
                    Os.symlink(podcastsDir.getAbsolutePath(), new File(storageDir, "podcasts").getAbsolutePath());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        File audiobooksDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS);
                        Os.symlink(audiobooksDir.getAbsolutePath(), new File(storageDir, "audiobooks").getAbsolutePath());
                    }

                    // Dir 0 should ideally be for primary storage
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ContextImpl.java;l=818
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=219
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=181
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/StorageManagerService.java;l=3796
                    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/services/core/java/com/android/server/MountService.java;l=3053

                    // Create "Android/data/com.termux" symlinks
                    File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    // Create "Android/media/com.termux" symlinks
                    dirs = context.getExternalMediaDirs();
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "media-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Storage symlinks created successfully.");
                } catch (Exception e) {
                    Logger.logErrorAndShowToast(context, LOG_TAG, e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Setup Storage Error: Error setting up link", e);
                    TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                        "## " + title + "\n\n" + Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)),
                        true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                }
            }
        }.start();
    }

    private static Error ensureDirectoryExists(File directory) {
        return FileUtils.createDirectoryFile(directory.getAbsolutePath());
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("termux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();

}
