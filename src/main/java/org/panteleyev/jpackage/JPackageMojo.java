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

    /**
     * Skips plugin execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * <p>--verbose</p>
     * <p>Enables verbose output.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean verbose;

    /**
     * <p>--type &lt;type></p>
     *
     * <p>The type of package to create. Possible values:</p>
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
     * <p>--name &lt;name></p>
     * <p>Name of the application and/or package.</p>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.name}", required = true)
    private String name;

    /**
     * <p>--app-version &lt;version></p>
     * <p> Version of the application and/or package.</p>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    /**
     * <p>--vendor &lt;vendor string></p>
     * <p>Vendor of the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String vendor;

    /**
     * <p>--icon &lt;icon file path></p>
     * <p>Path of the icon of the application package.</p>
     *
     * @since 14
     */
    @Parameter
    private Path icon;

    /**
     * <p>--runtime-image &lt;file path></p>
     * <p>Path of the predefined runtime image that will be copied into the application image.</p>
     * <p>If <code>runtimeImage</code> is not specified, <code>jpackage</code> will run <code>jlink</code> to create
     * the runtime image using options specified by <code>jLinkOptions</code>.</p>
     *
     * @since 14
     */
    @Parameter
    private Path runtimeImage;

    /**
     * --input &lt;input path>
     *
     * @since 14
     */
    @Parameter
    private Path input;

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
    private Path resourceDir;

    /**
     * <p>--dest &lt;destination path></p>
     * <p>Path where generated output file is placed.</p>
     *
     * @since 14
     */
    @Parameter(required = true)
    private Path destination;

    /**
     * <p>--module &lt;module name>[/&lt;main class>]</p>
     * <p>The main module (and optionally main class) of the application.
     * This module must be located on the module path.
     * When this option is specified, the main module will be linked in the Java runtime image. Either
     * <code>module</code> or <code>mainJar</code> option can be specified but not both.</p>
     *
     * @since 14
     */
    @Parameter
    private String module;

    /**
     * <p>--main-class &lt;class name></p>
     * <p>Qualified name of the application main class to execute.</p>
     *
     * @since 14
     */
    @Parameter
    private String mainClass;

    /**
     * <p>--main-jar &lt;main jar file></p>
     * <p>The main JAR of the application; containing the main class (specified as a path relative to the input path).</p>
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
    private Path temp;

    /**
     * <p>--copyright &lt;copyright string></p>
     * <p>Copyright for the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String copyright;

    /**
     * <p>--description &lt;description string></p>
     * <p>Description of the application.</p>
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
     *     &lt;modulePath>target/modules&lt;/modulePath>
     * &lt;/modulePaths>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<Path> modulePaths;

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
    private Path licenseFile;

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
    private List<Path> fileAssociations;

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
     * <p>This module list, along with the main module (if specified) will be passed to <code>jlink</code> as the
     * --add-module argument. If not specified, either just the main module (if <code>module</code> is specified), or
     * the default set of modules (if <code>mainJar</code> is specified) are used.</p>
     *
     * @since 14
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--app-image &lt;path to application image></p>
     * <p>Location of the predefined application image that is used to build an installable package (on all platforms)
     * or to be signed (on macOS).</p>
     *
     * @since 14
     */
    @Parameter
    private Path appImage;

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
     * <p>--about-url &lt;url></p>
     * <p>URL of the application's home page.</p>
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
    private List<Path> appContentPaths;

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
     * <p>--win-menu</p>
     * <p>Request to add a Start Menu shortcut for this application.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean winMenu;

    /**
     * <p>--win-dir-chooser</p>
     * <p>Adds a dialog to enable the user to choose a directory in which the product is installed.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean winDirChooser;

    /**
     * <p>--win-help-url &lt;url></p>
     * <p>URL where user can obtain further information or technical support.</p>
     *
     * @since 17
     */
    @Parameter
    private String winHelpUrl;

    /**
     * <p>--win-upgrade-uuid &lt;id string></p>
     * <p>UUID associated with upgrades for this package.</p>
     *
     * @since 14
     */
    @Parameter
    private String winUpgradeUuid;

    /**
     * <p>--win-menu-group &lt;menu group name></p>
     * <p>Start Menu group this application is placed in.</p>
     *
     * @since 14
     */
    @Parameter
    private String winMenuGroup;

    /**
     * <p>--win-shortcut</p>
     * <p>Request to create a desktop shortcut for this application.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean winShortcut;

    /**
     * <p>--win-shortcut-prompt</p>
     * <p>Adds a dialog to enable the user to choose if shortcuts will be created by installer.</p>
     *
     * @since 17
     */
    @Parameter
    private boolean winShortcutPrompt;

    /**
     * <p>--win-update-url &lt;url></p>
     * <p>URL of available application update information.</p>
     *
     * @since 17
     */
    @Parameter
    private String winUpdateUrl;

    /**
     * <p>--win-per-user-install</p>
     * <p>Request to perform an installation on a per-user basis.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean winPerUserInstall;

    /**
     * <p>--win-console</p>
     * <p>Creates a console launcher for the application, should be specified for application which requires console
     * interactions.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean winConsole;

    // OS X specific parameters

    /**
     * <p>--mac-package-identifier &lt;ID string></p>
     * <p>An identifier that uniquely identifies the application for macOS.</p>
     *
     * @since 14
     */
    @Parameter
    private String macPackageIdentifier;

    /**
     * <p>--mac-package-name &lt;name string></p>
     * <p>Name of the application as it appears in the Menu Bar.</p>
     * <p>This can be different from the application name.</p>
     * <p>This name must be less than 16 characters long and be suitable for displaying in the menu bar and the
     * application Info window. Defaults to the application name.</p>
     *
     * @since 14
     */
    @Parameter
    private String macPackageName;

    /**
     * <p>--mac-package-signing-prefix &lt;prefix string></p>
     * <p>When signing the application package, this value is prefixed to all components that need to be signed that
     * don't have an existing package identifier.</p>
     *
     * @since 17
     */
    @Parameter
    private String macPackageSigningPrefix;

    /**
     * <p>--mac-sign</p>
     * <p>Request that the package or the predefined application image be signed.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean macSign;

    /**
     * <p>--mac-signing-keychain &lt;keychain-name></p>
     * <p>Name of the keychain to search for the signing identity.</p>
     * <p>If not specified, the standard keychains are used.</p>
     *
     * @since 14
     */
    @Parameter
    private String macSigningKeychain;

    /**
     * <p>--mac-signing-key-user-name &lt;team name></p>
     * <p>Team or user name portion in Apple signing identities.</p>
     *
     * @since 14
     */
    @Parameter
    private String macSigningKeyUserName;

    /**
     * <p>--mac-app-store</p>
     * <p>Indicates that the jpackage output is intended for the Mac App Store.</p>
     *
     * @since 17
     */
    @Parameter
    private boolean macAppStore;

    /**
     * <p>--mac-entitlements &lt;file path></p>
     * <p>Path to file containing entitlements to use when signing executables and libraries in the bundle.</p>
     *
     * @since 17
     */
    @Parameter
    private Path macEntitlements;

    /**
     * <p>--mac-app-category &lt;category string></p>
     * <p>String used to construct LSApplicationCategoryType in application plist.</p>
     * <p>The default value is &quot;utilities&quot;.</p>
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
    private List<Path> macDmgContentPaths;


    // Linux specific parameters

    /**
     * <p>--linux-package-name &lt;package name></p>
     * <p>Name for Linux package.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxPackageName;

    /**
     * <p>--linux-deb-maintainer &lt;email address></p>
     * <p>Maintainer for .deb bundle.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxDebMaintainer;

    /**
     * <p>--linux-menu-group &lt;menu-group-name></p>
     * <p>Menu group this application is placed in.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxMenuGroup;

    /**
     * <p>--linux-package-deps &lt;package-dep-string></p>
     * <p>Required packages or capabilities for the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxPackageDeps;

    /**
     * <p>--linux-rpm-license-type &lt;type string></p>
     * <p>Type of the license ("License: value" of the RPM .spec)</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxRpmLicenseType;

    /**
     * <p>--linux-app-release &lt;release value></p>
     * <p>Release value of the RPM &lt;name>.spec file or Debian revision value of the DEB control file.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxAppRelease;

    /**
     * <p>--linux-app-category &lt;category value></p>
     * <p>Group value of the RPM &lt;name>.spec file or Section value of DEB control file.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxAppCategory;

    /**
     * <p>--linux-shortcut</p>
     * <p>Creates a shortcut for the application.</p>
     *
     * @since 14
     */
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
        logger.info("Toolchain in jpackage-maven-plugin: {}", tc);

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
