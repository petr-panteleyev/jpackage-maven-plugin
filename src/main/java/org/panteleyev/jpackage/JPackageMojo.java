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
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import java.io.File;
import java.util.List;
import java.util.Optional;
import static org.panteleyev.jpackage.OsUtil.isLinux;
import static org.panteleyev.jpackage.OsUtil.isMac;
import static org.panteleyev.jpackage.OsUtil.isWindows;
import static org.panteleyev.jpackage.StringUtil.escape;
import static org.panteleyev.jpackage.StringUtil.isEmpty;
import static org.panteleyev.jpackage.StringUtil.isNotEmpty;

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

        Commandline commandLine = buildParameters();
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            getLog().warn("Dry-run mode, not executing " + EXECUTABLE);
        } else {
            try {
                execute(commandLine);
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
        addMandatoryParameter(commandline, "--name", name);
        addMandatoryParameter(commandline, "--dest", destination);
        addParameter(commandline, "--verbose", verbose);
        addParameter(commandline, type);
        addParameter(commandline, "--app-version", appVersion);
        addParameter(commandline, "--copyright", copyright);
        addParameter(commandline, "--description", description);
        addParameter(commandline, "--runtime-image", runtimeImage, true);
        addParameter(commandline, "--input", input, true);
        addParameter(commandline, "--install-dir", installDir);
        addParameter(commandline, "--resource-dir", resourceDir, true);
        addParameter(commandline, "--vendor", vendor);
        addParameter(commandline, "--module", module);
        addParameter(commandline, "--main-class", mainClass);
        addParameter(commandline, "--main-jar", mainJar);
        addParameter(commandline, "--temp", temp);
        addParameter(commandline, "--icon", icon, true);
        addParameter(commandline, "--license-file", licenseFile, true);
        addParameter(commandline, "--app-image", appImage, true);

        if (modulePaths != null) {
            for (File modulePath : modulePaths) {
                addParameter(commandline, "--module-path", modulePath, true);
            }
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, "--add-modules", String.join(",", addModules));
        }

        if (javaOptions != null) {
            for (String option : javaOptions) {
                addParameter(commandline, "--java-options", escape(option));
            }
        }

        if (arguments != null) {
            for (String arg : arguments) {
                addParameter(commandline, "--arguments", escape(arg));
            }
        }

        if (fileAssociations != null) {
            for (File association : fileAssociations) {
                addParameter(commandline, "--file-associations", association, true);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                launcher.validate();
                addParameter(commandline, "--add-launcher",
                    launcher.getName() + "=" + launcher.getFile().getAbsolutePath());
            }
        }

        if (additionalOptions != null) {
            for (String option : additionalOptions) {
                addParameter(commandline, option);
            }
        }

        if (isMac()) {
            addParameter(commandline, "--mac-package-identifier", macPackageIdentifier);
            addParameter(commandline, "--mac-package-name", macPackageName);
            addParameter(commandline, "--mac-package-signing-prefix", macPackageSigningPrefix);
            addParameter(commandline, "--mac-sign", macSign);
            addParameter(commandline, "--mac-signing-keychain", macSigningKeychain, true);
            addParameter(commandline, "--mac-signing-key-user-name", macSigningKeyUserName);
        } else if (isWindows()) {
            addParameter(commandline, "--win-menu", winMenu);
            addParameter(commandline, "--win-dir-chooser", winDirChooser);
            addParameter(commandline, "--win-upgrade-uuid", winUpgradeUuid);
            addParameter(commandline, "--win-menu-group", winMenuGroup);
            addParameter(commandline, "--win-shortcut", winShortcut);
            addParameter(commandline, "--win-per-user-install", winPerUserInstall);
            addParameter(commandline, "--win-console", winConsole);
        } else if (isLinux()) {
            addParameter(commandline, "--linux-package-name", linuxPackageName);
            addParameter(commandline, "--linux-deb-maintainer", linuxDebMaintainer);
            addParameter(commandline, "--linux-menu-group", linuxMenuGroup);
            addParameter(commandline, "--linux-rpm-license-type", linuxRpmLicenseType);
            addParameter(commandline, "--linux-app-release", linuxAppRelease);
            addParameter(commandline, "--linux-app-category", linuxAppCategory);
            addParameter(commandline, "--linux-shortcut", linuxShortcut);
        }

        return commandline;
    }

    private void addMandatoryParameter(Commandline commandline,
                                       @SuppressWarnings("SameParameterValue") String name,
                                       String value) throws MojoFailureException
    {
        if (value == null || value.isEmpty()) {
            throw new MojoFailureException("Mandatory parameter \"" + name + "\" cannot be null or empty");
        }
        addParameter(commandline, name, value);
    }

    private void addMandatoryParameter(Commandline commandline,
                                       @SuppressWarnings("SameParameterValue") String name,
                                       File value) throws MojoFailureException
    {
        if (value == null) {
            throw new MojoFailureException("Mandatory parameter \"" + name + "\" cannot be null or empty");
        }
        addParameter(commandline, name, value);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, String name, File value) throws MojoFailureException {
        addParameter(commandline, name, value, false);
    }

    private void addParameter(Commandline commandline, String name, File value, boolean checkExistence) throws MojoFailureException {
        if (value == null) {
            return;
        }

        if (checkExistence && !value.exists()) {
            throw new MojoFailureException("File or directory " + value.getAbsolutePath() + " does not exist");
        }

        addParameter(commandline, name, value.getAbsolutePath());
    }

    private void addParameter(Commandline commandline, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }

        getLog().info("  " + name);
        commandline.createArg().setValue(name);
    }

    private void addParameter(Commandline commandline, String name, boolean value) {
        if (!value) {
            return;
        }

        getLog().info("  " + name);
        commandline.createArg().setValue(name);
    }

    private void addParameter(Commandline commandline, EnumParameter value) {
        if (value == null) {
            return;
        }

        addParameter(commandline, value.getParameterName(), value.getValue());
    }
}
