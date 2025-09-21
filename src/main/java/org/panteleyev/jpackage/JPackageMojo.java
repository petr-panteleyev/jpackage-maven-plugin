/*
 Copyright © 2020-2025 Petr Panteleyev
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.panteleyev.jpackage.CommandLineParameter.ABOUT_URL;
import static org.panteleyev.jpackage.CommandLineParameter.ADD_LAUNCHER;
import static org.panteleyev.jpackage.CommandLineParameter.ADD_MODULES;
import static org.panteleyev.jpackage.CommandLineParameter.APP_CONTENT;
import static org.panteleyev.jpackage.CommandLineParameter.APP_IMAGE;
import static org.panteleyev.jpackage.CommandLineParameter.APP_VERSION;
import static org.panteleyev.jpackage.CommandLineParameter.ARGUMENTS;
import static org.panteleyev.jpackage.CommandLineParameter.COPYRIGHT;
import static org.panteleyev.jpackage.CommandLineParameter.DESCRIPTION;
import static org.panteleyev.jpackage.CommandLineParameter.DESTINATION;
import static org.panteleyev.jpackage.CommandLineParameter.FILE_ASSOCIATIONS;
import static org.panteleyev.jpackage.CommandLineParameter.ICON;
import static org.panteleyev.jpackage.CommandLineParameter.INPUT;
import static org.panteleyev.jpackage.CommandLineParameter.INSTALL_DIR;
import static org.panteleyev.jpackage.CommandLineParameter.JAVA_OPTIONS;
import static org.panteleyev.jpackage.CommandLineParameter.JLINK_OPTIONS;
import static org.panteleyev.jpackage.CommandLineParameter.LAUNCHER_AS_SERVICE;
import static org.panteleyev.jpackage.CommandLineParameter.LICENSE_FILE;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_APP_CATEGORY;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_APP_RELEASE;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_DEB_MAINTAINER;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_MENU_GROUP;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_PACKAGE_DEPS;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_PACKAGE_NAME;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_RPM_LICENSE_TYPE;
import static org.panteleyev.jpackage.CommandLineParameter.LINUX_SHORTCUT;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_APP_CATEGORY;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_APP_STORE;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_DMG_CONTENT;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_ENTITLEMENTS;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_PACKAGE_IDENTIFIER;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_PACKAGE_NAME;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_PACKAGE_SIGNING_PREFIX;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_SIGN;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_SIGNING_KEYCHAIN;
import static org.panteleyev.jpackage.CommandLineParameter.MAC_SIGNING_KEY_USER_NAME;
import static org.panteleyev.jpackage.CommandLineParameter.MAIN_CLASS;
import static org.panteleyev.jpackage.CommandLineParameter.MAIN_JAR;
import static org.panteleyev.jpackage.CommandLineParameter.MODULE;
import static org.panteleyev.jpackage.CommandLineParameter.MODULE_PATH;
import static org.panteleyev.jpackage.CommandLineParameter.NAME;
import static org.panteleyev.jpackage.CommandLineParameter.RESOURCE_DIR;
import static org.panteleyev.jpackage.CommandLineParameter.RUNTIME_IMAGE;
import static org.panteleyev.jpackage.CommandLineParameter.TEMP;
import static org.panteleyev.jpackage.CommandLineParameter.TYPE;
import static org.panteleyev.jpackage.CommandLineParameter.VENDOR;
import static org.panteleyev.jpackage.CommandLineParameter.VERBOSE;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_CONSOLE;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_DIR_CHOOSER;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_HELP_URL;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_MENU;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_MENU_GROUP;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_PER_USER_INSTALL;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_SHORTCUT;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_SHORTCUT_PROMPT;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_UPDATE_URL;
import static org.panteleyev.jpackage.CommandLineParameter.WIN_UPGRADE_UUID;
import static org.panteleyev.jpackage.util.DirectoryUtil.isNestedDirectory;
import static org.panteleyev.jpackage.util.DirectoryUtil.removeDirectory;
import static org.panteleyev.jpackage.util.OsUtil.isLinux;
import static org.panteleyev.jpackage.util.OsUtil.isMac;
import static org.panteleyev.jpackage.util.OsUtil.isWindows;
import static org.panteleyev.jpackage.util.StringUtil.escape;
import static org.panteleyev.jpackage.util.StringUtil.isEmpty;
import static org.panteleyev.jpackage.util.StringUtil.isNotEmpty;

/**
 * <p>Generates application package.</p>
 * <p>Each plugin parameter defines <code>jpackage</code> option.
 * For detailed information about these options please refer to
 * <a href="https://docs.oracle.com/en/java/javase/21/jpackage/packaging-tool-user-guide.pdf">Packaging Tool User's Guide</a></p>
 */
@Mojo(name = "jpackage")
public class JPackageMojo implements org.apache.maven.api.plugin.Mojo {
    private static final Logger logger = LoggerFactory.getLogger(JPackageMojo.class);

    private static final String TOOLCHAIN = "jdk";
    public static final String EXECUTABLE = "jpackage";

    private static final String DRY_RUN_PROPERTY = "jpackage.dryRun";

    @Inject
    private Session session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private Path projectBuildDirectory;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter
    private boolean verbose;

    @Parameter
    private ImageType type;

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    @Parameter
    private String vendor;

    @Parameter
    private Path icon;

    @Parameter
    private Path runtimeImage;

    @Parameter
    private Path input;

    @Parameter
    private String installDir;

    @Parameter
    private Path resourceDir;

    @Parameter
    private Path destination;

    @Parameter
    private String module;

    @Parameter
    private String mainClass;

    @Parameter
    private String mainJar;

    @Parameter
    private Path temp;

    @Parameter
    private String copyright;

    @Parameter
    private String description;

    @Parameter
    private List<Path> modulePaths;

    @Parameter
    private List<String> javaOptions;

    @Parameter
    private List<String> arguments;

    @Parameter
    private Path licenseFile;

    @Parameter
    private List<Path> fileAssociations;

    @Parameter
    private List<Launcher> launchers;

    @Parameter
    private List<String> addModules;

    @Parameter
    private Path appImage;

    @Parameter
    private List<String> additionalOptions;

    @Parameter
    private List<String> jLinkOptions;

    @Parameter
    private String aboutUrl;

    @Parameter
    private List<Path> appContentPaths;

    @Parameter
    private boolean launcherAsService;

    @Parameter
    private boolean removeDestination;

    // Windows specific parameters

    @Parameter
    private boolean winMenu;

    @Parameter
    private boolean winDirChooser;

    @Parameter
    private String winHelpUrl;

    @Parameter
    private String winUpgradeUuid;

    @Parameter
    private String winMenuGroup;

    @Parameter
    private boolean winShortcut;

    @Parameter
    private boolean winShortcutPrompt;

    @Parameter
    private String winUpdateUrl;

    @Parameter
    private boolean winPerUserInstall;

    @Parameter
    private boolean winConsole;

    // OS X specific parameters

    @Parameter
    private String macPackageIdentifier;

    @Parameter
    private String macPackageName;

    @Parameter
    private String macPackageSigningPrefix;

    @Parameter
    private boolean macSign;

    @Parameter
    private String macSigningKeychain;

    @Parameter
    private String macSigningKeyUserName;

    @Parameter
    private boolean macAppStore;

    @Parameter
    private Path macEntitlements;

    @Parameter
    private String macAppCategory;

    @Parameter
    private List<Path> macDmgContentPaths;


    // Linux specific parameters

    @Parameter
    private String linuxPackageName;

    @Parameter
    private String linuxDebMaintainer;

    @Parameter
    private String linuxMenuGroup;

    @Parameter
    private boolean linuxPackageDeps;

    @Parameter
    private String linuxRpmLicenseType;

    @Parameter
    private String linuxAppRelease;

    @Parameter
    private String linuxAppCategory;

    @Parameter
    private boolean linuxShortcut;

    @Override
    public void execute() {
        if (skip) {
            logger.info("Skipping plugin execution");
            return;
        }

        var executable = getExecutable();
        logger.info("Using: {}", executable);

        var commandLine = buildParameters();
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        var dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            logger.warn("Dry-run mode, not executing " + EXECUTABLE);
            return;
        }

        if (removeDestination && destination != null) {
            var destinationPath = destination.toAbsolutePath();
            if (!isNestedDirectory(projectBuildDirectory, destinationPath)) {
                logger.error("Cannot remove destination folder, must belong to {}", projectBuildDirectory);
            } else {
                logger.warn("Trying to remove destination {}", destinationPath);
                removeDirectory(destinationPath);
            }
        }

        try {
            execute(commandLine);
        } catch (Exception ex) {
            throw new MojoException(ex.getMessage(), ex);
        }
    }

    private String getExecutable() {
        var toolchainManager = session.getService(ToolchainManager.class);
        return toolchainManager.getToolchainFromBuildContext(session, TOOLCHAIN)
                .map(this::getJPackageFromToolchain)
                .orElseGet(this::getJPackageFromJdkHome)
                .orElseThrow(() -> new MojoException("Failed to find " + EXECUTABLE));
    }

    private Optional<String> getJPackageFromJdkHome() {
        var jdkHome = System.getProperty("java.home");
        if (jdkHome == null || jdkHome.isEmpty()) {
            return Optional.empty();
        }

        logger.debug("Looking for {} in {}", EXECUTABLE, jdkHome);

        var executable = jdkHome + File.separator + "bin" + File.separator + EXECUTABLE;
        if (isWindows()) {
            executable = executable + ".exe";
        }

        if (new File(executable).exists()) {
            return Optional.of(executable);
        } else {
            logger.warn("File {} does not exist", executable);
            return Optional.empty();
        }
    }

    private Optional<String> getJPackageFromToolchain(Toolchain tc) {
        if (tc == null) {
            return Optional.empty();
        }

        var executable = tc.findTool(EXECUTABLE);
        if (executable == null) {
            logger.warn(EXECUTABLE + " is not part of configured toolchain");
        }

        return Optional.ofNullable(executable);
    }

    private void execute(Commandline commandline) {
        var err = new CommandLineUtils.StringStreamConsumer();
        var out = new CommandLineUtils.StringStreamConsumer();

        try {
            int exitCode = CommandLineUtils.executeCommandLine(commandline, out, err);

            var output = (isEmpty(out.getOutput()) ? null : '\n' + out.getOutput().trim());

            if (exitCode != 0) {
                if (isNotEmpty(output)) {
                    for (var line : output.split("\n")) {
                        logger.error(line);
                    }
                }

                var msg = new StringBuilder("\nExit code: ")
                        .append(exitCode);
                var errOutput = err.getOutput();
                if (isNotEmpty(errOutput)) {
                    msg.append(" - ").append(errOutput);
                }
                msg.append('\n')
                        .append("Command line was: ")
                        .append(commandline)
                        .append('\n')
                        .append('\n');

                throw new MojoException(msg.toString());
            } else {
                if (isNotEmpty(output)) {
                    for (var outputLine : output.split("\n")) {
                        logger.info(outputLine);
                    }
                }
            }
        } catch (CommandLineException e) {
            throw new MojoException("Error while executing " + EXECUTABLE + ": " + e.getMessage(), e);
        }
    }

    private Commandline buildParameters() {
        logger.info("jpackage options:");

        var commandline = new Commandline();
        addMandatoryParameter(commandline, NAME, name);
        addMandatoryParameter(commandline, DESTINATION, destination, false);
        addParameter(commandline, VERBOSE, verbose);
        addParameter(commandline, TYPE, type);
        addParameter(commandline, APP_VERSION, appVersion);
        addParameter(commandline, COPYRIGHT, copyright);
        addParameter(commandline, DESCRIPTION, description);
        addParameter(commandline, RUNTIME_IMAGE, runtimeImage, true);
        addParameter(commandline, INPUT, input, true);
        addParameter(commandline, INSTALL_DIR, installDir);
        addParameter(commandline, RESOURCE_DIR, resourceDir, true);
        addParameter(commandline, VENDOR, vendor);
        addParameter(commandline, MODULE, module);
        addParameter(commandline, MAIN_CLASS, mainClass);
        addParameter(commandline, MAIN_JAR, mainJar);
        addParameter(commandline, TEMP, temp, false);
        addParameter(commandline, ICON, icon, true);
        addParameter(commandline, LICENSE_FILE, licenseFile, true);
        addParameter(commandline, ABOUT_URL, aboutUrl);
        addParameter(commandline, APP_IMAGE, appImage, true);
        addParameter(commandline, LAUNCHER_AS_SERVICE, launcherAsService);

        if (modulePaths != null) {
            for (var modulePath : modulePaths) {
                addParameter(commandline, MODULE_PATH, modulePath, true);
            }
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, ADD_MODULES, String.join(",", addModules));
        }

        if (jLinkOptions != null && !jLinkOptions.isEmpty()) {
            addParameter(commandline, JLINK_OPTIONS, String.join(" ", jLinkOptions));
        }

        if (javaOptions != null) {
            for (var option : javaOptions) {
                addParameter(commandline, JAVA_OPTIONS, escape(option));
            }
        }

        if (arguments != null) {
            for (var arg : arguments) {
                addParameter(commandline, ARGUMENTS, escape(arg));
            }
        }

        if (fileAssociations != null) {
            for (var association : fileAssociations) {
                addParameter(commandline, FILE_ASSOCIATIONS, association, true);
            }
        }

        if (appContentPaths != null) {
            for (var appContent : appContentPaths) {
                addParameter(commandline, APP_CONTENT, appContent, true);
            }
        }

        if (launchers != null) {
            for (var launcher : launchers) {
                launcher.validate();
                addParameter(commandline, ADD_LAUNCHER,
                        launcher.getName() + "=" + launcher.getFile().toAbsolutePath());
            }
        }

        if (additionalOptions != null) {
            for (var option : additionalOptions) {
                addParameter(commandline, option);
            }
        }

        if (isMac()) {
            addParameter(commandline, MAC_PACKAGE_IDENTIFIER, macPackageIdentifier);
            addParameter(commandline, MAC_PACKAGE_NAME, macPackageName);
            addParameter(commandline, MAC_PACKAGE_SIGNING_PREFIX, macPackageSigningPrefix);
            addParameter(commandline, MAC_SIGN, macSign);
            addParameter(commandline, MAC_SIGNING_KEYCHAIN, macSigningKeychain);
            addParameter(commandline, MAC_SIGNING_KEY_USER_NAME, macSigningKeyUserName);
            addParameter(commandline, MAC_APP_STORE, macAppStore);
            addParameter(commandline, MAC_ENTITLEMENTS, macEntitlements, true);
            addParameter(commandline, MAC_APP_CATEGORY, macAppCategory);
            if (macDmgContentPaths != null) {
                for (var content : macDmgContentPaths) {
                    addParameter(commandline, MAC_DMG_CONTENT, content, true);
                }
            }
        } else if (isWindows()) {
            addParameter(commandline, WIN_CONSOLE, winConsole);
            addParameter(commandline, WIN_DIR_CHOOSER, winDirChooser);
            addParameter(commandline, WIN_HELP_URL, winHelpUrl);
            addParameter(commandline, WIN_MENU, winMenu);
            addParameter(commandline, WIN_MENU_GROUP, winMenuGroup);
            addParameter(commandline, WIN_PER_USER_INSTALL, winPerUserInstall);
            addParameter(commandline, WIN_SHORTCUT, winShortcut);
            addParameter(commandline, WIN_SHORTCUT_PROMPT, winShortcutPrompt);
            addParameter(commandline, WIN_UPDATE_URL, winUpdateUrl);
            addParameter(commandline, WIN_UPGRADE_UUID, winUpgradeUuid);
        } else if (isLinux()) {
            addParameter(commandline, LINUX_PACKAGE_NAME, linuxPackageName);
            addParameter(commandline, LINUX_DEB_MAINTAINER, linuxDebMaintainer);
            addParameter(commandline, LINUX_MENU_GROUP, linuxMenuGroup);
            addParameter(commandline, LINUX_PACKAGE_DEPS, linuxPackageDeps);
            addParameter(commandline, LINUX_RPM_LICENSE_TYPE, linuxRpmLicenseType);
            addParameter(commandline, LINUX_APP_RELEASE, linuxAppRelease);
            addParameter(commandline, LINUX_APP_CATEGORY, linuxAppCategory);
            addParameter(commandline, LINUX_SHORTCUT, linuxShortcut);
        }

        return commandline;
    }

    @SuppressWarnings("SameParameterValue")
    private void addMandatoryParameter(Commandline commandline, CommandLineParameter parameter, String value) {
        if (value == null || value.isEmpty()) {
            throw new MojoException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter.getName(), value);
    }

    @SuppressWarnings("SameParameterValue")
    private void addMandatoryParameter(Commandline commandline, CommandLineParameter parameter, Path value,
            boolean checkExistence)
    {
        if (value == null) {
            throw new MojoException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, checkExistence);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        logger.info("  {} {}", name, value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, String value) {
        addParameter(commandline, parameter.getName(), value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, Path value,
            boolean checkExistence)
    {
        if (value == null) {
            return;
        }

        var absolutePath = value.toAbsolutePath();

        if (checkExistence && !Files.exists(absolutePath)) {
            throw new MojoException("File or directory " + absolutePath + " does not exist");
        }

        addParameter(commandline, parameter.getName(), absolutePath.toString());
    }

    private void addParameter(Commandline commandline, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }

        logger.info("  {}", name);
        commandline.createArg().setValue(name);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, boolean value) {
        if (!value) {
            return;
        }

        logger.info("  {}", parameter.getName());
        commandline.createArg().setValue(parameter.getName());
    }

    @SuppressWarnings("SameParameterValue")
    private void addParameter(Commandline commandline, CommandLineParameter parameter, EnumParameter value) {
        if (value == null) {
            return;
        }

        addParameter(commandline, parameter, value.getValue());
    }
}
