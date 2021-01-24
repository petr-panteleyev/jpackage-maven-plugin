/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.jpackage;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.panteleyev.jpackage.OsUtil.isLinux;
import static org.panteleyev.jpackage.OsUtil.isMac;
import static org.panteleyev.jpackage.OsUtil.isWindows;
import static org.panteleyev.jpackage.StringUtil.escape;

/**
 * <p>Generates application package.</p>
 * <p>Each plugin parameter defines <code>jpackage</code> option.
 * For detailed information about these options please refer to
 * <a href="https://docs.oracle.com/en/java/javase/15/jpackage/packaging-tool-user-guide.pdf">Packaging Tool User's Guide</a></p>
 */
@Mojo(name = JPackageMojo.GOAL, defaultPhase = LifecyclePhase.NONE)
public class JPackageMojo extends AbstractMojo {
    public static final String GOAL = "jpackage";

    private static final String JPACKAGE_HOME_ENV = "JPACKAGE_HOME";
    private static final String TOOLCHAIN = "jdk";
    private static final String EXECUTABLE = "jpackage";

    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * --verbose
     *
     * @since 0.0.4
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
     * @since 0.0.1
     */
    @Parameter
    private ImageType type;

    /**
     * --name &lt;name>
     *
     * @since 0.0.1
     */
    @Parameter(required = true)
    private String name;

    /**
     * --app-version &lt;version>
     *
     * @since 0.0.1
     */
    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    /**
     * --vendor &lt;vendor string>
     *
     * @since 0.0.1
     */
    @Parameter
    private String vendor;

    /**
     * --icon &lt;icon file path>
     *
     * @since 0.0.1
     */
    @Parameter
    private String icon;

    /**
     * --runtime-image &lt;file path>
     *
     * @since 0.0.1
     */
    @Parameter
    private String runtimeImage;

    /**
     * --input &lt;input path>
     *
     * @since 0.0.1
     */
    @Parameter
    private String input;

    /**
     * --install-dir &lt;file path>
     *
     * @since 0.0.4
     */
    @Parameter
    private String installDir;

    /**
     * --resource-dir &lt;resource dir path>
     *
     * @since 1.1.0
     */
    @Parameter
    private String resourceDir;

    /**
     * --dest &lt;destination path>
     *
     * @since 0.0.1
     */
    @Parameter(required = true)
    private String destination;

    /**
     * --module &lt;module name>[/&lt;main class>]
     *
     * @since 0.0.1
     */
    @Parameter
    private String module;

    /**
     * --main-class &lt;class name>
     *
     * @since 0.0.1
     */
    @Parameter
    private String mainClass;

    /**
     * --main-jar &lt;main jar file>
     *
     * @since 0.0.1
     */
    @Parameter
    private String mainJar;

    /**
     * --temp &lt;temp dir path>
     *
     * @since 1.1.0
     */
    @Parameter
    private String temp;

    /**
     * --copyright &lt;copyright string>
     *
     * @since 0.0.1
     */
    @Parameter
    private String copyright;

    /**
     * --description &lt;description string>
     *
     * @since 0.0.1
     */
    @Parameter
    private String description;

    /**
     * --module-path &lt;module path>...
     *
     * @since 0.0.1
     */
    @Parameter
    private String modulePath;

    /**
     * --java-options &lt;JVM option>
     *
     * @since 0.0.1
     */
    @Parameter
    private String[] javaOptions;

    /**
     * --arguments &lt;main class arguments>
     *
     * @since 0.0.4
     */
    @Parameter
    private String[] arguments;

    /**
     * --license-file &lt;license file path>
     *
     * @since 1.3.0
     */
    @Parameter
    private String licenseFile;

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
     * @since 1.3.0
     */
    @Parameter
    private String[] fileAssociations;

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
     * @since 1.3.0
     */
    @Parameter
    private Launcher[] launchers;

    // Windows specific parameters

    /**
     * --win-menu
     *
     * @since 0.0.1
     */
    @Parameter
    private boolean winMenu;

    /**
     * --win-dir-chooser
     *
     * @since 0.0.1
     */
    @Parameter
    private boolean winDirChooser;

    /**
     * --win-upgrade-uuid &lt;id string>
     *
     * @since 0.0.1
     */
    @Parameter
    private String winUpgradeUuid;

    /**
     * --win-menu-group &lt;menu group name>
     *
     * @since 0.0.1
     */
    @Parameter
    private String winMenuGroup;

    /**
     * --win-shortcut
     *
     * @since 0.0.1
     */
    @Parameter
    private boolean winShortcut;

    /**
     * --win-per-user-install
     *
     * @since 0.0.1
     */
    @Parameter
    private boolean winPerUserInstall;

    /**
     * --win-console
     *
     * @since 1.3.0
     */
    @Parameter
    private boolean winConsole;

    // OS X specific parameters

    /**
     * --mac-package-identifier &lt;ID string>
     *
     * @since 0.0.2
     */
    @Parameter
    private String macPackageIdentifier;

    /**
     * --mac-package-name &lt;name string>
     *
     * @since 0.0.2
     */
    @Parameter
    private String macPackageName;

    /**
     * --mac-package-signing-prefix &lt;prefix string>
     *
     * @since 0.0.2
     */
    @Parameter
    private String macPackageSigningPrefix;

    /**
     * --mac-sign
     *
     * @since 0.0.2
     */
    @Parameter
    private boolean macSign;

    /**
     * --mac-signing-keychain &lt;file path>
     *
     * @since 0.0.2
     */
    @Parameter
    private String macSigningKeychain;

    /**
     * --mac-signing-key-user-name &lt;team name>
     *
     * @since 0.0.2
     */
    @Parameter
    private String macSigningKeyUserName;

    // Linux specific parameters

    /**
     * --linux-package-name &lt;package name>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxPackageName;

    /**
     * --linux-deb-maintainer &lt;email address>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxDebMaintainer;

    /**
     * --linux-menu-group &lt;menu-group-name>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxMenuGroup;

    /**
     * --linux-rpm-license-type &lt;type string>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxRpmLicenseType;

    /**
     * --linux-app-release &lt;release value>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxAppRelease;

    /**
     * --linux-app-category &lt;category value>
     *
     * @since 0.0.3
     */
    @Parameter
    private String linuxAppCategory;

    /**
     * --linux-shortcut
     *
     * @since 0.0.3
     */
    @Parameter
    private boolean linuxShortcut;

    public void execute() throws MojoExecutionException {
        String executable = getJPackageExecutable();
        if (executable == null) {
            throw new MojoExecutionException("Failed to find " + EXECUTABLE);
        }

        getLog().info("Using: " + executable);

        try {
            execute(executable);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MojoExecutionException(ex.getMessage());
        }
    }

    private String getJPackageFromJdkHome(String jdkHome) {
        if (jdkHome == null || jdkHome.isEmpty()) {
            return null;
        }

        getLog().info("Looking for " + EXECUTABLE + " in " + jdkHome);

        String executable = jdkHome + File.separator + "bin" + File.separator + EXECUTABLE;
        if (isWindows()) {
            executable = executable + ".exe";
        }

        if (new File(executable).exists()) {
            return executable;
        } else {
            getLog().warn("File " + executable + " does not exist");
            return null;
        }
    }

    private String getJPackageFromToolchain() {
        getLog().info("Looking for " + EXECUTABLE + " in toolchain");

        Toolchain jdk = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (jdk == null) {
            getLog().warn("Toolchain not configured");
            return null;
        }

        String executable = jdk.findTool(EXECUTABLE);
        if (executable == null) {
            getLog().warn(EXECUTABLE + " is not part of configured toolchain");
            return null;
        }

        return executable;
    }

    private String getJPackageExecutable() {
        // Priority 1: JPACKAGE_HOME
        String executable = getJPackageFromJdkHome(System.getenv(JPACKAGE_HOME_ENV));
        if (executable != null) {
            return executable;
        }

        // Priority 2: maven-toolchain-plugin
        executable = getJPackageFromToolchain();
        if (executable != null) {
            return executable;
        }

        // Priority 3: java.home
        return getJPackageFromJdkHome(System.getProperty("java.home"));
    }

    private void execute(String cmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> parameters = new ArrayList<>();
        parameters.add(cmd.contains(" ") ? ("\"" + cmd + "\"") : cmd);

        buildParameters(parameters);
        processBuilder.command(parameters);

        Process process = processBuilder.start();

        getLog().info(EXECUTABLE + " output:");

        int status = process.waitFor();

        logCmdOutput(process.getInputStream());
        logCmdOutput(process.getErrorStream());

        if (status != 0) {
            throw new MojoExecutionException("Error while executing " + EXECUTABLE);
        }
    }

    private void buildParameters(List<String> parameters) {
        getLog().info("jpackage parameters:");

        addParameter(parameters, "--verbose", verbose);
        addParameter(parameters, "--type", type);
        addParameter(parameters, "--name", name);
        addParameter(parameters, "--app-version", appVersion);
        addPathParameter(parameters, "--dest", destination);
        addParameter(parameters, "--copyright", copyright);
        addParameter(parameters, "--description", description);
        addPathParameter(parameters, "--runtime-image", runtimeImage);
        addPathParameter(parameters, "--input", input);
        addPathParameter(parameters, "--install-dir", installDir);
        addPathParameter(parameters, "--resource-dir", resourceDir);
        addParameter(parameters, "--vendor", vendor);
        addParameter(parameters, "--module", module);
        addParameter(parameters, "--main-class", mainClass);
        addParameter(parameters, "--main-jar", mainJar);
        addPathParameter(parameters, "--temp", temp);
        addPathParameter(parameters, "--module-path", modulePath);
        addPathParameter(parameters, "--icon", icon);
        addPathParameter(parameters, "--license-file", licenseFile);

        if (javaOptions != null) {
            for (String option : javaOptions) {
                addParameter(parameters, "--java-options", escape(option));
            }
        }

        if (arguments != null) {
            for (String arg : arguments) {
                addParameter(parameters, "--arguments", escape(arg));
            }
        }

        if (fileAssociations != null) {
            for (String association : fileAssociations) {
                addPathParameter(parameters, "--file-associations", association);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                addParameter(parameters, "--add-launcher",
                    launcher.getName() + "=" + resolvePath(launcher.getFile()));
            }
        }

        if (isMac()) {
            addParameter(parameters, "--mac-package-identifier", macPackageIdentifier);
            addParameter(parameters, "--mac-package-name", macPackageName);
            addParameter(parameters, "--mac-package-signing-prefix", macPackageSigningPrefix);
            addParameter(parameters, "--mac-sign", macSign);
            addParameter(parameters, "--mac-signing-keychain", macSigningKeychain);
            addParameter(parameters, "--mac-signing-key-user-name", macSigningKeyUserName);
        } else if (isWindows()) {
            addParameter(parameters, "--win-menu", winMenu);
            addParameter(parameters, "--win-dir-chooser", winDirChooser);
            addParameter(parameters, "--win-upgrade-uuid", winUpgradeUuid);
            addParameter(parameters, "--win-menu-group", winMenuGroup);
            addParameter(parameters, "--win-shortcut", winShortcut);
            addParameter(parameters, "--win-per-user-install", winPerUserInstall);
            addParameter(parameters, "--win-console", winConsole);
        } else if (isLinux()) {
            addParameter(parameters, "--linux-package-name", linuxPackageName);
            addParameter(parameters, "--linux-deb-maintainer", linuxDebMaintainer);
            addParameter(parameters, "--linux-menu-group", linuxMenuGroup);
            addParameter(parameters, "--linux-rpm-license-type", linuxRpmLicenseType);
            addParameter(parameters, "--linux-app-release", linuxAppRelease);
            addParameter(parameters, "--linux-app-category", linuxAppCategory);
            addParameter(parameters, "--linux-shortcut", linuxShortcut);
        }
    }

    private void addParameter(List<String> params, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        params.add(name);
        params.add(value);
    }

    private void addPathParameter(List<String> params, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        addParameter(params, name, resolvePath(value));
    }

    private String resolvePath(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        Path path = new File(value).toPath();
        if (!path.isAbsolute()) {
            String oldValue = value;
            value = project.getBasedir().getAbsolutePath() + File.separator + value;
            getLog().debug("Resolving path " + oldValue + " to " + value);
        }
        return value;
    }

    private void addParameter(List<String> params, String name, boolean value) {
        if (!value) {
            return;
        }

        getLog().info("  " + name);
        params.add(name);
    }

    private void addParameter(List<String> params, String name, EnumParameter value) {
        if (value == null) {
            return;
        }

        addParameter(params, name, value.getValue());
    }

    private void logCmdOutput(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
        }
    }
}
