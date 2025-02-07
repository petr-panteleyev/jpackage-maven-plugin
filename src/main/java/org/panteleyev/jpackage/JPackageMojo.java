// Copyright © 2020-2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
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
import static org.panteleyev.jpackage.CommandLineParameter.BIND_SERVICES;
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
import static org.panteleyev.jpackage.CommandLineParameter.MAC_BUNDLE_SIGNING_PREFIX;
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
import static org.panteleyev.jpackage.util.StringUtil.parseVersion;

/**
 * <p>Generates application package.</p>
 * <p>Each plugin parameter defines <code>jpackage</code> option.
 * For detailed information about these options please refer to
 * <a href="https://docs.oracle.com/en/java/javase/21/jpackage/packaging-tool-user-guide.pdf">Packaging Tool User's Guide</a></p>
 */
@Mojo(name = JPackageMojo.GOAL, defaultPhase = LifecyclePhase.NONE)
public class JPackageMojo extends AbstractMojo {
    public static final String GOAL = "jpackage";

    private static final String TOOLCHAIN = "jdk";
    public static final String EXECUTABLE = "jpackage";

    private static final String DRY_RUN_PROPERTY = "jpackage.dryRun";

    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String projectBuildDirectory;

    /**
     * Skips plugin execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * --verbose
     *
     * @since 14
     */
    @Parameter
    private boolean verbose;

    /**
     * <p>--type &lt;type></p>
     *
     * <p>Possible values:</p>
     * <table>
     *     <tr>
     *         <th>Plugin</th><th>JPackage</th>
     *     </tr>
     *     <tr><td>APP_IMAGE</td><td>app-image</td></tr>
     *     <tr><td>DMG</td><td>dmg</td></tr>
     *     <tr><td>PKG</td><td>pkg</td></tr>
     *     <tr><td>EXE</td><td>exe</td></tr>
     *     <tr><td>MSI</td><td>msi</td></tr>
     *     <tr><td>DEB</td><td>deb</td></tr>
     * </table>
     *
     * @since 14
     */
    @Parameter
    private ImageType type;

    /**
     * --name &lt;name>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.name}")
    private String name;

    /**
     * --app-version &lt;version>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    /**
     * --vendor &lt;vendor string>
     *
     * @since 14
     */
    @Parameter
    private String vendor;

    /**
     * --icon &lt;icon file path>
     *
     * @since 14
     */
    @Parameter
    private File icon;

    /**
     * --runtime-image &lt;file path>
     *
     * @since 14
     */
    @Parameter
    private File runtimeImage;

    /**
     * --input &lt;input path>
     *
     * @since 14
     */
    @Parameter
    private File input;

    /**
     * --install-dir &lt;dir path>
     *
     * @since 14
     */
    @Parameter
    private String installDir;

    /**
     * --resource-dir &lt;resource dir path>
     *
     * @since 14
     */
    @Parameter
    private File resourceDir;

    /**
     * --dest &lt;destination path>
     *
     * @since 14
     */
    @Parameter
    private File destination;

    /**
     * --module &lt;module name>[/&lt;main class>]
     *
     * @since 14
     */
    @Parameter
    private String module;

    /**
     * --main-class &lt;class name>
     *
     * @since 14
     */
    @Parameter
    private String mainClass;

    /**
     * --main-jar &lt;main jar file>
     *
     * @since 14
     */
    @Parameter
    private String mainJar;

    /**
     * --temp &lt;temp dir path>
     *
     * @since 14
     */
    @Parameter
    private File temp;

    /**
     * --copyright &lt;copyright string>
     *
     * @since 14
     */
    @Parameter
    private String copyright;

    /**
     * --description &lt;description string>
     *
     * @since 14
     */
    @Parameter
    private String description;

    /**
     * <p>Each module path is specified by a separate &lt;modulePath> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;modulePaths>
     *     &lt;modulePath>target/jmods&lt;/modulePath>
     * &lt;/modulePaths>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<File> modulePaths;

    /**
     * --java-options &lt;JVM option>
     *
     * @since 14
     */
    @Parameter
    private List<String> javaOptions;

    /**
     * --arguments &lt;main class arguments>
     *
     * @since 14
     */
    @Parameter
    private List<String> arguments;

    /**
     * --license-file &lt;license file path>
     *
     * @since 14
     */
    @Parameter
    private File licenseFile;

    /**
     * <p>--file-associations &lt;file association property file></p>
     *
     * <p>Each property file is specified by a separate &lt;fileAssociation> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;fileAssociations>
     *     &lt;fileAssociation>src/properties/java.properties&lt;/fileAssociation>
     *     &lt;fileAssociation>src/properties/cpp.properties&lt;/fileAssociation>
     * &lt;/fileAssociations>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<File> fileAssociations;

    /**
     * <p>--add-launcher &lt;name>=&lt;file></p>
     *
     * <p>Application launchers specified by one</p>
     * <pre>
     * &lt;launcher>
     *     &lt;name>name-of-the-launcher&lt;/name>
     *     &lt;file>/path/to/launcher.properties&lt;/file>
     * &lt;/launcher>
     * </pre>
     *
     * <p>element for each launcher.</p>
     *
     * @since 14
     */
    @Parameter
    private List<Launcher> launchers;

    /**
     * <p>--add-modules &lt;module>[,&lt;module>]</p>
     *
     * @since 14
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--app-image &lt;path to application image></p>
     *
     * @since 14
     */
    @Parameter
    private File appImage;

    /**
     * <p>Additional jpackage options not covered by dedicated plugin parameters.</p>
     *
     * <p>Example:
     * <pre>
     * &lt;additionalOptions>
     *     &lt;option>--jlink-options&lt;/option>
     *     &lt;option>--bind-services&lt;/option>
     * &lt;/additionalOptions>
     * </pre>
     * </p>
     */
    @Parameter
    private List<String> additionalOptions;

    /**
     * <p>jlink options.</p>
     *
     * <p>Example:
     * <pre>
     * &lt;jLinkOptions>
     *     &lt;jLinkOption>--strip-native-commands&lt;/jLinkOption>
     *     &lt;jLinkOption>--strip-debug&lt;/jLinkOption>
     * &lt;/jLinkOptions>
     * </pre>
     * </p>
     *
     * @since 16
     */
    @Parameter
    private List<String> jLinkOptions;

    /**
     * <p>--bind-services</p>
     * <p>This option is supported by jpackage versions 14 and 15 only.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean bindServices;

    /**
     * --about-url &lt;url>
     *
     * @since 17
     */
    @Parameter
    private String aboutUrl;

    /**
     * <p>--app-content additional-content[,additional-content...]</p>
     * <p>Example:
     * <pre>
     * &lt;appContentPaths>
     *     &lt;appContentPath>./docs&lt;/appContentPath>
     *     &lt;appContentPath>./images&lt;/appContentPath>
     * &lt;/appContentPaths>
     * </pre>
     * </p>
     *
     * @since 18
     */
    @Parameter
    private List<File> appContentPaths;

    /**
     * --launcher-as-service
     *
     * @since 19
     */
    @Parameter
    private boolean launcherAsService;

    /**
     * <p>Remove destination directory.</p>
     * <p><code>jpackage</code> utility fails if generated binary already exists. In order to work around this behaviour
     * there is plugin boolean option <code>removeDestination</code>. If <code>true</code> plugin will try to delete
     * directory specified by <code>destination</code>. This might be useful to relaunch <code>jpackage</code> task
     * without rebuilding an entire project.</p>
     * <p>For safety reasons plugin will not process <code>removeDestination</code> if <code>destination</code> points
     * to a location outside of <code>${project.build.directory}</code>.</p>
     */
    @Parameter
    private boolean removeDestination;

    // Windows specific parameters

    /**
     * --win-menu
     *
     * @since 14
     */
    @Parameter
    private boolean winMenu;

    /**
     * --win-dir-chooser
     *
     * @since 14
     */
    @Parameter
    private boolean winDirChooser;

    /**
     * --win-help-url &lt;url>
     *
     * @since 17
     */
    @Parameter
    private String winHelpUrl;

    /**
     * --win-upgrade-uuid &lt;id string>
     *
     * @since 14
     */
    @Parameter
    private String winUpgradeUuid;

    /**
     * --win-menu-group &lt;menu group name>
     *
     * @since 14
     */
    @Parameter
    private String winMenuGroup;

    /**
     * --win-shortcut
     *
     * @since 14
     */
    @Parameter
    private boolean winShortcut;

    /**
     * --win-shortcut-prompt
     *
     * @since 17
     */
    @Parameter
    private boolean winShortcutPrompt;

    /**
     * --win-update-url &lt;url>
     *
     * @since 17
     */
    @Parameter
    private String winUpdateUrl;

    /**
     * --win-per-user-install
     *
     * @since 14
     */
    @Parameter
    private boolean winPerUserInstall;

    /**
     * --win-console
     *
     * @since 14
     */
    @Parameter
    private boolean winConsole;

    // OS X specific parameters

    /**
     * --mac-package-identifier &lt;ID string>
     *
     * @since 14
     */
    @Parameter
    private String macPackageIdentifier;

    /**
     * --mac-package-name &lt;name string>
     *
     * @since 14
     */
    @Parameter
    private String macPackageName;

    /**
     * <p>--mac-bundle-signing-prefix &lt;prefix string></p>
     * <p>This option is supported by jpackage versions 14, 15 and 16 only.</p>
     *
     * @since 14
     */
    @Parameter
    private String macBundleSigningPrefix;

    /**
     * --mac-package-signing-prefix &lt;prefix string>
     *
     * @since 17
     */
    @Parameter
    private String macPackageSigningPrefix;

    /**
     * --mac-sign
     *
     * @since 14
     */
    @Parameter
    private boolean macSign;

    /**
     * --mac-signing-keychain &lt;file path>
     *
     * @since 14
     */
    @Parameter
    private File macSigningKeychain;

    /**
     * --mac-signing-key-user-name &lt;team name>
     *
     * @since 14
     */
    @Parameter
    private String macSigningKeyUserName;

    /**
     * --mac-app-store
     *
     * @since 17
     */
    @Parameter
    private boolean macAppStore;

    /**
     * --mac-entitlements &lt;file path>
     *
     * @since 17
     */
    @Parameter
    private File macEntitlements;

    /**
     * --mac-app-category &lt;category string>
     *
     * @since 17
     */
    @Parameter
    private String macAppCategory;

    /**
     * <p>--mac-dmg-content additional-content[,additional-content...]</p>
     * <p>Example:
     * <pre>
     * &lt;macDmgContentPaths>
     *     &lt;macDmgContentPath>./docs&lt;/macDmgContentPath>
     *     &lt;macDmgContentPath>./images&lt;/macDmgContentPath>
     * &lt;/macDmgContentPaths>
     * </pre>
     * </p>
     *
     * @since 18
     */
    @Parameter
    private List<File> macDmgContentPaths;


    // Linux specific parameters

    /**
     * --linux-package-name &lt;package name>
     *
     * @since 14
     */
    @Parameter
    private String linuxPackageName;

    /**
     * --linux-deb-maintainer &lt;email address>
     *
     * @since 14
     */
    @Parameter
    private String linuxDebMaintainer;

    /**
     * --linux-menu-group &lt;menu-group-name>
     *
     * @since 14
     */
    @Parameter
    private String linuxMenuGroup;

    /**
     * --linux-package-deps
     *
     * @since 14
     */
    @Parameter
    private boolean linuxPackageDeps;

    /**
     * --linux-rpm-license-type &lt;type string>
     *
     * @since 14
     */
    @Parameter
    private String linuxRpmLicenseType;

    /**
     * --linux-app-release &lt;release value>
     *
     * @since 14
     */
    @Parameter
    private String linuxAppRelease;

    /**
     * --linux-app-category &lt;category value>
     *
     * @since 14
     */
    @Parameter
    private String linuxAppCategory;

    /**
     * --linux-shortcut
     *
     * @since 14
     */
    @Parameter
    private boolean linuxShortcut;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping plugin execution");
            return;
        }

        Toolchain tc = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (tc != null) {
            getLog().info("Toolchain in jpackage-maven-plugin: " + tc);
        }

        String executable = getJPackageExecutable(tc)
                .orElseThrow(() -> new MojoExecutionException("Failed to find " + EXECUTABLE));

        int majorVersion = getMajorVersion(executable);
        if (majorVersion == 0) {
            throw new MojoExecutionException("Could not determine " + EXECUTABLE + " version");
        } else {
            getLog().info("Using: " + executable + ", major version: " + majorVersion);
        }

        Commandline commandLine = buildParameters(majorVersion);
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            getLog().warn("Dry-run mode, not executing " + EXECUTABLE);
            return;
        }

        if (removeDestination && destination != null) {
            Path destinationPath = destination.toPath().toAbsolutePath();
            if (!isNestedDirectory(new File(projectBuildDirectory).toPath(), destinationPath)) {
                getLog().error("Cannot remove destination folder, must belong to " +  projectBuildDirectory);
            } else {
                getLog().warn("Trying to remove destination " + destinationPath);
                removeDirectory(destinationPath);
            }
        }

        try {
            execute(commandLine);
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private Optional<String> getJPackageFromJdkHome(String jdkHome) {
        if (jdkHome == null || jdkHome.isEmpty()) {
            return Optional.empty();
        }

        getLog().debug("Looking for " + EXECUTABLE + " in " + jdkHome);

        String executable = jdkHome + File.separator + "bin" + File.separator + EXECUTABLE;
        if (isWindows()) {
            executable = executable + ".exe";
        }

        if (new File(executable).exists()) {
            return Optional.of(executable);
        } else {
            getLog().warn("File " + executable + " does not exist");
            return Optional.empty();
        }
    }

    private Optional<String> getJPackageFromToolchain(Toolchain tc) {
        if (tc == null) {
            return Optional.empty();
        }

        String executable = tc.findTool(EXECUTABLE);
        if (executable == null) {
            getLog().warn(EXECUTABLE + " is not part of configured toolchain");
        }

        return Optional.ofNullable(executable);
    }

    private Optional<String> getJPackageExecutable(Toolchain tc) {
        Optional<String> executable = getJPackageFromToolchain(tc);
        return executable.isPresent() ?
                executable : getJPackageFromJdkHome(System.getProperty("java.home"));
    }

    private void execute(Commandline commandline) throws Exception {
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        try {
            int exitCode = CommandLineUtils.executeCommandLine(commandline, out, err);

            String output = (isEmpty(out.getOutput()) ? null : '\n' + out.getOutput().trim());

            if (exitCode != 0) {
                if (isNotEmpty(output)) {
                    for (String line : output.split("\n")) {
                        getLog().error(line);
                    }
                }

                StringBuilder msg = new StringBuilder("\nExit code: ")
                        .append(exitCode);
                String errOutput = err.getOutput();
                if (isNotEmpty(errOutput)) {
                    msg.append(" - ").append(errOutput);
                }
                msg.append('\n');
                msg.append("Command line was: ").append(commandline).append('\n').append('\n');

                throw new MojoExecutionException(msg.toString());
            } else {
                if (isNotEmpty(output)) {
                    for (String outputLine : output.split("\n")) {
                        getLog().info(outputLine);
                    }
                }
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Error while executing " + EXECUTABLE + ": " + e.getMessage(), e);
        }
    }

    private Commandline buildParameters(int version) throws MojoFailureException {
        getLog().info("jpackage options:");

        Commandline commandline = new Commandline();
        addMandatoryParameter(commandline, NAME, name, version);
        addMandatoryParameter(commandline, DESTINATION, destination, false, version);
        addParameter(commandline, VERBOSE, verbose, version);
        addParameter(commandline, TYPE, type, version);
        addParameter(commandline, APP_VERSION, appVersion, version);
        addParameter(commandline, COPYRIGHT, copyright, version);
        addParameter(commandline, DESCRIPTION, description, version);
        addParameter(commandline, RUNTIME_IMAGE, runtimeImage, true, version);
        addParameter(commandline, INPUT, input, true, version);
        addParameter(commandline, INSTALL_DIR, installDir, version);
        addParameter(commandline, RESOURCE_DIR, resourceDir, true, version);
        addParameter(commandline, VENDOR, vendor, version);
        addParameter(commandline, MODULE, module, version);
        addParameter(commandline, MAIN_CLASS, mainClass, version);
        addParameter(commandline, MAIN_JAR, mainJar, version);
        addParameter(commandline, TEMP, temp, false, version);
        addParameter(commandline, ICON, icon, true, version);
        addParameter(commandline, LICENSE_FILE, licenseFile, true, version);
        addParameter(commandline, ABOUT_URL, aboutUrl, version);
        addParameter(commandline, APP_IMAGE, appImage, true, version);
        addParameter(commandline, LAUNCHER_AS_SERVICE, launcherAsService, version);

        if (modulePaths != null) {
            for (File modulePath : modulePaths) {
                addParameter(commandline, MODULE_PATH, modulePath, true, version);
            }
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, ADD_MODULES, String.join(",", addModules), version);
        }

        addParameter(commandline, BIND_SERVICES, bindServices, version);
        if (jLinkOptions != null && !jLinkOptions.isEmpty()) {
            addParameter(commandline, JLINK_OPTIONS, String.join(" ", jLinkOptions), version);
        }

        if (javaOptions != null) {
            for (String option : javaOptions) {
                addParameter(commandline, JAVA_OPTIONS, escape(option), version);
            }
        }

        if (arguments != null) {
            for (String arg : arguments) {
                addParameter(commandline, ARGUMENTS, escape(arg), version);
            }
        }

        if (fileAssociations != null) {
            for (File association : fileAssociations) {
                addParameter(commandline, FILE_ASSOCIATIONS, association, true, version);
            }
        }

        if (appContentPaths != null) {
            for (File appContent : appContentPaths) {
                addParameter(commandline, APP_CONTENT, appContent, true, version);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                launcher.validate();
                addParameter(commandline, ADD_LAUNCHER,
                        launcher.getName() + "=" + launcher.getFile().getAbsolutePath(), version);
            }
        }

        if (additionalOptions != null) {
            for (String option : additionalOptions) {
                addParameter(commandline, option);
            }
        }

        if (isMac()) {
            addParameter(commandline, MAC_PACKAGE_IDENTIFIER, macPackageIdentifier, version);
            addParameter(commandline, MAC_PACKAGE_NAME, macPackageName, version);
            addParameter(commandline, MAC_BUNDLE_SIGNING_PREFIX, macBundleSigningPrefix, version);
            addParameter(commandline, MAC_PACKAGE_SIGNING_PREFIX, macPackageSigningPrefix, version);
            addParameter(commandline, MAC_SIGN, macSign, version);
            addParameter(commandline, MAC_SIGNING_KEYCHAIN, macSigningKeychain, true, version);
            addParameter(commandline, MAC_SIGNING_KEY_USER_NAME, macSigningKeyUserName, version);
            addParameter(commandline, MAC_APP_STORE, macAppStore, version);
            addParameter(commandline, MAC_ENTITLEMENTS, macEntitlements, true, version);
            addParameter(commandline, MAC_APP_CATEGORY, macAppCategory, version);
            if (macDmgContentPaths != null) {
                for (File content : macDmgContentPaths) {
                    addParameter(commandline, MAC_DMG_CONTENT, content, true, version);
                }
            }
        } else if (isWindows()) {
            addParameter(commandline, WIN_CONSOLE, winConsole, version);
            addParameter(commandline, WIN_DIR_CHOOSER, winDirChooser, version);
            addParameter(commandline, WIN_HELP_URL, winHelpUrl, version);
            addParameter(commandline, WIN_MENU, winMenu, version);
            addParameter(commandline, WIN_MENU_GROUP, winMenuGroup, version);
            addParameter(commandline, WIN_PER_USER_INSTALL, winPerUserInstall, version);
            addParameter(commandline, WIN_SHORTCUT, winShortcut, version);
            addParameter(commandline, WIN_SHORTCUT_PROMPT, winShortcutPrompt, version);
            addParameter(commandline, WIN_UPDATE_URL, winUpdateUrl, version);
            addParameter(commandline, WIN_UPGRADE_UUID, winUpgradeUuid, version);
        } else if (isLinux()) {
            addParameter(commandline, LINUX_PACKAGE_NAME, linuxPackageName, version);
            addParameter(commandline, LINUX_DEB_MAINTAINER, linuxDebMaintainer, version);
            addParameter(commandline, LINUX_MENU_GROUP, linuxMenuGroup, version);
            addParameter(commandline, LINUX_PACKAGE_DEPS, linuxPackageDeps, version);
            addParameter(commandline, LINUX_RPM_LICENSE_TYPE, linuxRpmLicenseType, version);
            addParameter(commandline, LINUX_APP_RELEASE, linuxAppRelease, version);
            addParameter(commandline, LINUX_APP_CATEGORY, linuxAppCategory, version);
            addParameter(commandline, LINUX_SHORTCUT, linuxShortcut, version);
        }

        return commandline;
    }

    private void addMandatoryParameter(
            Commandline commandline,
            @SuppressWarnings("SameParameterValue") CommandLineParameter parameter,
            String value,
            int version
    ) throws MojoFailureException {
        if (value == null || value.isEmpty()) {
            throw new MojoFailureException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, version);
    }

    private void addMandatoryParameter(
            Commandline commandline,
            @SuppressWarnings("SameParameterValue") CommandLineParameter parameter,
            File value,
            boolean checkExistence,
            int version
    ) throws MojoFailureException {
        if (value == null) {
            throw new MojoFailureException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, checkExistence, version);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            String value,
            int version
    ) throws MojoFailureException {
        if (value == null || value.isEmpty()) {
            return;
        }

        parameter.checkVersion(version);

        getLog().info("  " + parameter.getName() + " " + value);
        commandline.createArg().setValue(parameter.getName());
        commandline.createArg().setValue(value);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            File value,
            boolean checkExistence,
            int version
    ) throws MojoFailureException {
        addParameter(
                commandline,
                parameter,
                value,
                checkExistence,
                true,
                version
        );
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            File value,
            boolean checkExistence,
            boolean makeAbsolute,
            int version
    ) throws MojoFailureException {
        if (value == null) {
            return;
        }

        parameter.checkVersion(version);

        String path = makeAbsolute ? value.getAbsolutePath() : value.getPath();

        if (checkExistence && !value.exists()) {
            throw new MojoFailureException("File or directory " + path + " does not exist");
        }

        addParameter(commandline, parameter.getName(), path);
    }

    private void addParameter(Commandline commandline, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }

        getLog().info("  " + name);
        commandline.createArg().setValue(name);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            boolean value,
            int version
    ) throws MojoFailureException {
        if (!value) {
            return;
        }

        parameter.checkVersion(version);

        getLog().info("  " + parameter.getName());
        commandline.createArg().setValue(parameter.getName());
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            EnumParameter value,
            int version
    ) throws MojoFailureException {
        if (value == null) {
            return;
        }

        addParameter(commandline, parameter, value.getValue(), version);
    }

    private int getMajorVersion(String executable) {
        Commandline commandLine = new Commandline();
        commandLine.createArg().setValue("--version");
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        try {
            int exitCode = CommandLineUtils.executeCommandLine(commandLine, out, err);
            if (exitCode != 0) {
                return 0;
            } else {
                return parseVersion(out.getOutput());
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
