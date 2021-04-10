/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.jpackage;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private static final String TOOLCHAIN = "jdk";
    private static final String EXECUTABLE = "jpackage";

    private static final String DRY_RUN_PROPERTY = "jpackage.dryRun";

    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

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
    @Parameter(defaultValue = "${project.name}")
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
    private File icon;

    /**
     * --runtime-image &lt;file path>
     *
     * @since 0.0.1
     */
    @Parameter
    private File runtimeImage;

    /**
     * --input &lt;input path>
     *
     * @since 0.0.1
     */
    @Parameter
    private File input;

    /**
     * --install-dir &lt;file path>
     *
     * @since 0.0.4
     */
    @Parameter
    private File installDir;

    /**
     * --resource-dir &lt;resource dir path>
     *
     * @since 1.1.0
     */
    @Parameter
    private File resourceDir;

    /**
     * --dest &lt;destination path>
     *
     * @since 0.0.1
     */
    @Parameter
    private File destination;

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
    private File temp;

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
     * <p>Each module path is specified by a separate &lt;modulePath> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;modulePaths>
     *     &lt;modulePath>target/jmods&lt;/modulePath>
     * &lt;/modulePaths>
     * </pre>
     * </p>
     *
     * @since 1.4.0
     */
    @Parameter
    private List<File> modulePaths;

    /**
     * --java-options &lt;JVM option>
     *
     * @since 0.0.1
     */
    @Parameter
    private List<String> javaOptions;

    /**
     * --arguments &lt;main class arguments>
     *
     * @since 0.0.4
     */
    @Parameter
    private List<String> arguments;

    /**
     * --license-file &lt;license file path>
     *
     * @since 1.3.0
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
     * @since 1.3.0
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
     * @since 1.3.0
     */
    @Parameter
    private List<Launcher> launchers;

    /**
     * <p>--add-modules &lt;module>[,&lt;module>]</p>
     *
     * @since 1.4.0
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--app-image &lt;path to application image></p>
     *
     * @since 1.5.0
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
     *
     * @since 1.5.0
     */
    @Parameter
    private List<String> additionalOptions;

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
    private File macSigningKeychain;

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

    public void execute() throws MojoExecutionException, MojoFailureException {

        Toolchain tc = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (tc != null) {
            getLog().info("Toolchain in jpackage-maven-plugin: " + tc);
        }

        String executable = getJPackageExecutable(tc)
            .orElseThrow(() -> new MojoExecutionException("Failed to find " + EXECUTABLE));
        getLog().info("Using: " + executable);

        List<String> parameters = new ArrayList<>();
        parameters.add(executable.contains(" ") ? ("\"" + executable + "\"") : executable);
        buildParameters(parameters);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            getLog().warn("Dry-run mode, not executing " + EXECUTABLE);
        } else {
            try {
                execute(parameters);
            } catch (Exception ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
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

    private void execute(List<String> parameters) throws Exception {
        Process process = new ProcessBuilder()
            .redirectErrorStream(true)
            .command(parameters)
            .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
        }

        int status = process.waitFor();
        if (status != 0) {
            throw new MojoExecutionException("Error while executing " + EXECUTABLE);
        }
    }

    private void buildParameters(List<String> parameters) throws MojoFailureException {
        getLog().info("jpackage options:");

        addMandatoryParameter(parameters, "--name", name);
        addMandatoryParameter(parameters, "--dest", destination);
        addParameter(parameters, "--verbose", verbose);
        addParameter(parameters, type);
        addParameter(parameters, "--app-version", appVersion);
        addParameter(parameters, "--copyright", copyright);
        addParameter(parameters, "--description", description);
        addParameter(parameters, "--runtime-image", runtimeImage, true);
        addParameter(parameters, "--input", input, true);
        addParameter(parameters, "--install-dir", installDir);
        addParameter(parameters, "--resource-dir", resourceDir, true);
        addParameter(parameters, "--vendor", vendor);
        addParameter(parameters, "--module", module);
        addParameter(parameters, "--main-class", mainClass);
        addParameter(parameters, "--main-jar", mainJar);
        addParameter(parameters, "--temp", temp);
        addParameter(parameters, "--icon", icon, true);
        addParameter(parameters, "--license-file", licenseFile, true);
        addParameter(parameters, "--app-image", appImage, true);

        if (modulePaths != null) {
            for (File modulePath : modulePaths) {
                addParameter(parameters, "--module-path", modulePath, true);
            }
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(parameters, "--add-modules", String.join(",", addModules));
        }

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
            for (File association : fileAssociations) {
                addParameter(parameters, "--file-associations", association, true);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                launcher.validate();
                addParameter(parameters, "--add-launcher",
                    launcher.getName() + "=" + launcher.getFile().getAbsolutePath());
            }
        }

        if (additionalOptions != null) {
            for (String option : additionalOptions) {
                addParameter(parameters, option);
            }
        }

        if (isMac()) {
            addParameter(parameters, "--mac-package-identifier", macPackageIdentifier);
            addParameter(parameters, "--mac-package-name", macPackageName);
            addParameter(parameters, "--mac-package-signing-prefix", macPackageSigningPrefix);
            addParameter(parameters, "--mac-sign", macSign);
            addParameter(parameters, "--mac-signing-keychain", macSigningKeychain, true);
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

    private void addMandatoryParameter(List<String> params,
                                       @SuppressWarnings("SameParameterValue") String name,
                                       String value) throws MojoFailureException
    {
        if (value == null || value.isEmpty()) {
            throw new MojoFailureException("Mandatory parameter \"" + name + "\" cannot be null or empty");
        }
        addParameter(params, name, value);
    }

    private void addMandatoryParameter(List<String> params,
                                       @SuppressWarnings("SameParameterValue") String name,
                                       File value) throws MojoFailureException
    {
        if (value == null) {
            throw new MojoFailureException("Mandatory parameter \"" + name + "\" cannot be null or empty");
        }
        addParameter(params, name, value);
    }

    private void addParameter(List<String> params, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        params.add(name);
        params.add(value);
    }

    private void addParameter(List<String> params, String name, File value) throws MojoFailureException {
        addParameter(params, name, value, false);
    }

    private void addParameter(List<String> params, String name, File value, boolean checkExistence) throws MojoFailureException {
        if (value == null) {
            return;
        }

        if (checkExistence && !value.exists()) {
            throw new MojoFailureException("File or directory " + value.getAbsolutePath() + " does not exist");
        }

        addParameter(params, name, value.getAbsolutePath());
    }

    private void addParameter(List<String> params, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }

        getLog().info("  " + name);
        params.add(name);
    }

    private void addParameter(List<String> params, String name, boolean value) {
        if (!value) {
            return;
        }

        getLog().info("  " + name);
        params.add(name);
    }

    private void addParameter(List<String> params, EnumParameter value) {
        if (value == null) {
            return;
        }

        addParameter(params, value.getParameterName(), value.getValue());
    }
}
