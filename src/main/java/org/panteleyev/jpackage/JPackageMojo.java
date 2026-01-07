// Copyright © 2020-2026 Petr Panteleyev
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
 *
 * <p>Each plugin parameter defines <code>jpackage</code> option.
 * For detailed information about these options please refer to
 * <a href="https://docs.oracle.com/en/java/javase/25/jpackage/packaging-tool-user-guide.pdf">Packaging Tool User's
 * Guide</a></p>
 */
@Mojo(name = "jpackage", defaultPhase = LifecyclePhase.NONE)
public class JPackageMojo extends AbstractMojo {
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
     * <p>--verbose</p>
     * <p>Enables verbose output.</p>
     *
     * @since 14
     */
    @Parameter
    private boolean verbose;

    /**
     * <p>--type <i>type</i></p>
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
     * <p>--name <i>name</i></p>
     * <p>Name of the application and/or package.</p>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.name}", required = true)
    private String name;

    /**
     * <p>--app-version <i>version</i></p>
     * <p>Version of the application and/or package.</p>
     *
     * @since 14
     */
    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    /**
     * <p>--vendor <i>vendor</i></p>
     * <p>Vendor of the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String vendor;

    /**
     * <p>--icon <i>path</i></p>
     * <p>Path of the icon of the application package.</p>
     *
     * @since 14
     */
    @Parameter
    private File icon;

    /**
     * <p>--runtime-image <i>path</i></p>
     *
     * <p><b>For runtime image:</b> Path of the predefined runtime image that will be copied into the application
     * image.<br> If <code>runtimeImage</code> is not specified, <code>jpackage</code> will run <code>jlink</code> to
     * create the runtime image using options specified by <code>jLinkOptions</code>.</p>
     *
     * <p><b>For application package:</b> Path of the predefined runtime image to install.<br>
     * Option is required when creating a runtime installer.</p>
     *
     * @since 14
     */
    @Parameter
    private File runtimeImage;

    /**
     * <p>--input <i>directory</i></p>
     * <p>Path of the input directory that contains the files to be packaged. All files in the input directory will
     * be packaged into the application image.</p>
     *
     * @since 14
     */
    @Parameter
    private File input;

    /**
     * <p>--install-dir <i>path</i></p>
     * <p>Absolute path of the installation directory of the application (on macOS or linux), or relative sub-path of
     * the installation directory such as &quot;Program Files&quot; or &quot;AppData&quot; (on Windows).</p>
     *
     * @since 14
     */
    @Parameter
    private String installDir;

    /**
     * <p>--resource-dir <i>path</i></p>
     * <p>Path to override <code>jpackage</code> resources. Icons, template files, and other resources of
     * <code>jpackage</code> can be over-ridden by adding replacement resources to this directory.</p>
     *
     * @since 14
     */
    @Parameter
    private File resourceDir;

    /**
     * <p>--dest <i>destination</i></p>
     * <p>Path where generated output file is placed.</p>
     *
     * <p>See also {@link #removeDestination}</p>
     *
     * @since 14
     */
    @Parameter(required = true)
    private File destination;

    /**
     * <p>--module <i></I>module-name</i>[/<i>main-class</i>]</p>
     * <p>The main module (and optionally main class) of the application.</p>
     * <p>This module must be located on the module path.</p>
     * <p>When this option is specified, the main module will be linked in the Java runtime image. Either
     * <code>module</code> or <code>mainJar</code> option can be specified but not both.</p>
     *
     * @since 14
     */
    @Parameter
    private String module;

    /**
     * <p>--main-class <i>class-name</i></p>
     * <p>Qualified name of the application main class to execute.</p>
     *
     * @since 14
     */
    @Parameter
    private String mainClass;

    /**
     * <p>--main-jar <i>main-jar</i></p>
     * <p>The main JAR of the application; containing the main class (specified as a path relative to the input
     * path).</p>
     *
     * @since 14
     */
    @Parameter
    private String mainJar;

    /**
     * <p>--temp <i>directory</i></p>
     * <p>Path of a new or empty directory used to create temporary files.</p>
     * <p>If specified, the temp dir will not be removed upon the task completion and must be removed manually.</p>
     * <p>If not specified, a temporary directory will be created and removed upon the task completion.</p>
     *
     * @since 14
     */
    @Parameter
    private File temp;

    /**
     * <p>--copyright <i>copyright</i></p>
     * <p>Copyright for the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String copyright;

    /**
     * <p>--description <i>description</i></p>
     * <p>Description of the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String description;

    /**
     * <p>--module-path <i>module-path</i> [,<i>module-path</i>...]</p>
     * <p>Each path is either a directory of modules or the path to a modular jar.</p>
     * <p>Example:
     * <pre>
     * &lt;modulePaths>
     *     &lt;path>${project.build.directory}/mods&lt;/path>
     * &lt;/modulePaths>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<File> modulePaths;

    /**
     * <p>--java-options <i>options</i></p>
     * <p>Options to pass to the Java runtime.</p>
     * <p>Example:
     * <pre>
     * &lt;javaOptions>
     *     &lt;option>-XX:NewRatio=1&lt;/option>
     *     &lt;option>-Xms100m&lt;/option>
     *     &lt;option>-Xmx100m&lt;/option>
     * &lt;/javaOptions>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<String> javaOptions;

    /**
     * <p>--arguments <i>arguments</i></p>
     * <p>Command line arguments to pass to the main class if no command line arguments are given to the launcher.</p>
     * <p>Example:
     * <pre>
     * &lt;arguments>
     *     &lt;argument>--help&lt;/argument>
     * &lt;/arguments>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<String> arguments;

    /**
     * <p>--license-file <i>path</i></p>
     * <p>Path to the license file.</p>
     *
     * @since 14
     */
    @Parameter
    private File licenseFile;

    /**
     * <p>--file-associations <i>path</i></p>
     * <p>Path to a Properties file that contains list of key, value pairs.</p>
     * <p>Each property file is specified by a separate &lt;fileAssociation> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;fileAssociations>
     *     &lt;association>src/properties/java.properties&lt;/association>
     *     &lt;association>src/properties/cpp.properties&lt;/association>
     * &lt;/fileAssociations>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<File> fileAssociations;

    /**
     * <p>--add-launcher <i>name=path</i></p>
     * <p>Name of launcher, and a path to a Properties file that contains a list of key, value pairs.</p>
     * <p>Example:
     * <pre>
     * &lt;launchers>
     *     &lt;launcher>
     *         &lt;name>name-of-the-launcher&lt;/name>
     *         &lt;file>/path/to/launcher.properties&lt;/file>
     *     &lt;/launcher>
     * &lt;/launchers>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<Launcher> launchers;

    /**
     * <p>--add-modules <i>module</i>[,<i>module</i>]</p>
     * <p>This module list, along with the main module (if specified) will be passed to <code>jlink</code> as the
     * --add-module argument. If not specified, either just the main module (if <code>module</code> is specified), or
     * the default set of modules (if <code>mainJar</code> is specified) are used.</p>
     * <p>Example:
     * <pre>
     * &lt;addModules>
     *     &lt;module>module1&lt;/module>
     *     &lt;module>module2&lt;/module>
     * &lt;/addModules>
     * </pre>
     * </p>
     *
     * @since 14
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--app-image <i>directory</i></p>
     * <p>Location of the predefined application image that is used to build an installable package (on all platforms)
     * or to be signed (on macOS).</p>
     *
     * @since 14
     */
    @Parameter
    private File appImage;

    /**
     * <p>Additional jpackage options not covered by dedicated plugin parameters.</p>
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
     * <p>--jlink-options <i>options</i></p>
     * <p>A list of options to pass to jlink</p>
     * <p>If not specified, defaults to &quot;--strip-native-commands --strip-debug --no-man-pages
     * --no-header-files&quot;.</p>
     * <p>Example:
     * <pre>
     * &lt;jLinkOptions>
     *     &lt;option>--strip-native-commands&lt;/option>
     *     &lt;option>--strip-debug&lt;/option>
     * &lt;/jLinkOptions>
     * </pre>
     * </p>
     *
     * @since 16
     */
    @Parameter
    private List<String> jLinkOptions;

    /**
     * <p>--about-url <i>url</i></p>
     * <p>URL of the application's home page.</p>
     *
     * @since 17
     */
    @Parameter
    private String aboutUrl;

    /**
     * <p>--app-content <i>additional-content</i>[,<i>additional-content</i>...]</p>
     * <p>A list of paths to files and/or directories to add to the application payload.</p>
     * <p>Example:
     * <pre>
     * &lt;appContentPaths>
     *     &lt;path>./docs&lt;/path>
     *     &lt;path>./images&lt;/path>
     * &lt;/appContentPaths>
     * </pre>
     * </p>
     *
     * @since 18
     */
    @Parameter
    private List<File> appContentPaths;

    /**
     * <p>--launcher-as-service</p>
     * <p>Request to create an installer that will register the main application launcher as a background service-type
     * application.</p>
     *
     * @since 19
     */
    @Parameter
    private boolean launcherAsService;

    /**
     * <p>Remove destination directory.</p>
     * <p>Request to remove <code>destination</code> directory before executing <code>jpackage</code>.</p>
     *
     * <p><code>jpackage</code> utility fails if generated binary already exists. This option allows to overcome this
     * behaviour. If <code>true</code> plugin will try to delete directory specified by <code>destination</code>. This
     * might be useful to relaunch <code>jpackage</code> task without rebuilding an entire project.</p>
     *
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
     * <p>--win-help-url <i>url</i></p>
     * <p>URL where user can obtain further information or technical support.</p>
     *
     * @since 17
     */
    @Parameter
    private String winHelpUrl;

    /**
     * <p>--win-upgrade-uuid <i>id</i></p>
     * <p>UUID associated with upgrades for this package.</p>
     *
     * @since 14
     */
    @Parameter
    private String winUpgradeUuid;

    /**
     * <p>--win-menu-group <i>menu group name</i></p>
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
     * <p>--win-update-url <i>url</i></p>
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
     * <p>--mac-package-identifier <i>identifier</i></p>
     * <p>An identifier that uniquely identifies the application for macOS.</p>
     * <p>Defaults to the main class name.</p>
     * <p>May only use alphanumeric (A-Z,a-z,0-9), hyphen (-), and period (.) characters.</p>
     *
     * @since 14
     */
    @Parameter
    private String macPackageIdentifier;

    /**
     * <p>--mac-package-name <i>name</i></p>
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
     * <p>--mac-package-signing-prefix <i>prefix</i></p>
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
     * <p>--mac-signing-keychain <i>keychain-name</i></p>
     * <p>Name of the keychain to search for the signing identity.</p>
     * <p>If not specified, the standard keychains are used.</p>
     *
     * @since 14
     */
    @Parameter
    private File macSigningKeychain;

    /**
     * <p>--mac-signing-key-user-name <i>name</i></p>
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
     * <p>--mac-entitlements <i>path</i></p>
     * <p>Path to file containing entitlements to use when signing executables and libraries in the bundle.</p>
     *
     * @since 17
     */
    @Parameter
    private File macEntitlements;

    /**
     * <p>--mac-app-category <i>category</i></p>
     * <p>String used to construct LSApplicationCategoryType in application plist.</p>
     * <p>The default value is &quot;utilities&quot;.</p>
     *
     * @since 17
     */
    @Parameter
    private String macAppCategory;

    /**
     * <p>--mac-dmg-content <i>additional-content</i>[,<i>additional-content</i>...]</p>
     * <p>Include all the referenced content in the dmg.</p>
     * <p>Example:
     * <pre>
     * &lt;macDmgContentPaths>
     *     &lt;path>./docs&lt;/path>
     *     &lt;path>./images&lt;/path>
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
     * <p>--linux-package-name <i>name</i></p>
     * <p>Name for Linux package.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxPackageName;

    /**
     * <p>--linux-deb-maintainer <i>email-address</i></p>
     * <p>Maintainer for .deb bundle.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxDebMaintainer;

    /**
     * <p>--linux-menu-group <i>menu-group-name</i></p>
     * <p>Menu group this application is placed in.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxMenuGroup;

    /**
     * <p>--linux-package-deps <i>package-dep-string</i></p>
     * <p>Required packages or capabilities for the application.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxPackageDeps;

    /**
     * <p>--linux-rpm-license-type <i>type</i></p>
     * <p>Type of the license ("License: value" of the RPM .spec)</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxRpmLicenseType;

    /**
     * <p>--linux-app-release <i>release</i></p>
     * <p>Release value of the RPM &lt;name>.spec file or Debian revision value of the DEB control file.</p>
     *
     * @since 14
     */
    @Parameter
    private String linuxAppRelease;

    /**
     * <p>--linux-app-category <i>category-value</i></p>
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
        getLog().info("Using: " + executable);

        Commandline commandLine = buildParameters();
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            getLog().warn("Dry-run mode, not executing " + EXECUTABLE);
            return;
        }

        if (removeDestination && destination != null) {
            Path destinationPath = destination.toPath().toAbsolutePath();
            if (!isNestedDirectory(new File(projectBuildDirectory).toPath(), destinationPath)) {
                getLog().error("Cannot remove destination folder, must belong to " + projectBuildDirectory);
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

    private Commandline buildParameters() throws MojoFailureException {
        getLog().info("jpackage options:");

        Commandline commandline = new Commandline();
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
            for (File modulePath : modulePaths) {
                addParameter(commandline, MODULE_PATH, modulePath, true);
            }
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, ADD_MODULES,
                    addModules.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));
        }

        if (jLinkOptions != null && !jLinkOptions.isEmpty()) {
            addParameter(commandline, JLINK_OPTIONS,
                    jLinkOptions.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(" ")));
        }

        if (javaOptions != null) {
            for (String option : javaOptions) {
                addParameter(commandline, JAVA_OPTIONS, escape(option));
            }
        }

        if (arguments != null) {
            for (String arg : arguments) {
                addParameter(commandline, ARGUMENTS, escape(arg));
            }
        }

        if (fileAssociations != null) {
            for (File association : fileAssociations) {
                addParameter(commandline, FILE_ASSOCIATIONS, association, true);
            }
        }

        if (appContentPaths != null) {
            for (File appContent : appContentPaths) {
                addParameter(commandline, APP_CONTENT, appContent, true);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                if (launcher == null) continue;
                launcher.validate();
                addParameter(commandline, ADD_LAUNCHER,
                        launcher.getName() + "=" + launcher.getFile().getAbsolutePath());
            }
        }

        if (additionalOptions != null) {
            for (String option : additionalOptions) {
                addParameter(commandline, option);
            }
        }

        if (isMac()) {
            addParameter(commandline, MAC_PACKAGE_IDENTIFIER, macPackageIdentifier);
            addParameter(commandline, MAC_PACKAGE_NAME, macPackageName);
            addParameter(commandline, MAC_PACKAGE_SIGNING_PREFIX, macPackageSigningPrefix);
            addParameter(commandline, MAC_SIGN, macSign);
            addParameter(commandline, MAC_SIGNING_KEYCHAIN, macSigningKeychain, true);
            addParameter(commandline, MAC_SIGNING_KEY_USER_NAME, macSigningKeyUserName);
            addParameter(commandline, MAC_APP_STORE, macAppStore);
            addParameter(commandline, MAC_ENTITLEMENTS, macEntitlements, true);
            addParameter(commandline, MAC_APP_CATEGORY, macAppCategory);
            if (macDmgContentPaths != null) {
                for (File content : macDmgContentPaths) {
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
    private void addMandatoryParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            String value) throws MojoFailureException
    {
        if (value == null || value.isEmpty()) {
            throw new MojoFailureException(
                    "Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value);
    }

    @SuppressWarnings("SameParameterValue")
    private void addMandatoryParameter(Commandline commandline, CommandLineParameter parameter, File value,
            boolean checkExistence) throws MojoFailureException
    {
        if (value == null) {
            throw new MojoFailureException(
                    "Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, checkExistence);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + parameter.getName() + " " + value);
        commandline.createArg().setValue(parameter.getName());
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, File value,
            boolean checkExistence) throws MojoFailureException
    {
        if (value == null) {
            return;
        }

        String path = value.getAbsolutePath();

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

    private void addParameter(Commandline commandline, CommandLineParameter parameter, boolean value) {
        if (!value) {
            return;
        }

        getLog().info("  " + parameter.getName());
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
