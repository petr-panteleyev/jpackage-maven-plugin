/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.jpackage;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
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

@Mojo(name = "jpackage", defaultPhase = LifecyclePhase.NONE)
@Execute(goal = "jpackage", phase = LifecyclePhase.PACKAGE)
public class JPackageMojo extends AbstractMojo {
    private static final String TOOLCHAIN = "jdk";

    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    @Parameter
    private boolean verbose;
    @Parameter
    private ImageType type;
    @Parameter(required = true)
    private String name;
    @Parameter(required = true)
    private String appVersion;
    @Parameter
    private String vendor;
    @Parameter
    private String icon;
    @Parameter
    private String runtimeImage;
    @Parameter
    private String input;
    @Parameter
    private String installDir;
    @Parameter(required = true)
    private String destination;
    @Parameter
    private String module;
    @Parameter
    private String mainClass;
    @Parameter
    private String mainJar;
    @Parameter
    private String copyright;
    @Parameter
    private String description;
    @Parameter
    private String modulePath;
    @Parameter
    private String[] javaOptions;
    @Parameter
    private String[] arguments;

    // Windows specific parameters
    @Parameter
    private boolean winMenu;
    @Parameter
    private boolean winDirChooser;
    @Parameter
    private String winUpgradeUuid;
    @Parameter
    private String winMenuGroup;
    @Parameter
    private boolean winShortcut;
    @Parameter
    private boolean winPerUserInstall;

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

    // Linux specific parameters
    @Parameter
    private String linuxPackageName;
    @Parameter
    private String linuxDebMaintainer;
    @Parameter
    private String linuxMenuGroup;
    @Parameter
    private String linuxRpmLicenseType;
    @Parameter
    private String linuxAppRelease;
    @Parameter
    private String linuxAppCategory;
    @Parameter
    private boolean linuxShortcut;

    public void execute() throws MojoExecutionException {
        String jpackage = getJPackageExecutable();
        getLog().info("Using: " + jpackage);

        validateParameters();

        try {
            execute(jpackage);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MojoExecutionException(ex.getMessage());
        }
    }

    private String getJPackageFromJavaHome() throws MojoExecutionException {
        getLog().warn("Getting jpackage from java.home");

        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new MojoExecutionException("java.home is not set");
        }

        String jpackage = javaHome + File.separator + "bin" + File.separator + "jpackage";
        if (isWindows()) {
            jpackage = jpackage + ".exe";
        }
        return jpackage;
    }

    private String getJPackageExecutable() throws MojoExecutionException {
        Toolchain jdk = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (jdk == null) {
            getLog().warn("Toolchain not configured");
            return getJPackageFromJavaHome();
        }

        String jpackage = jdk.findTool("jpackage");
        if (jpackage == null) {
            getLog().warn("jpackage is not part of configured toolchain");
            return getJPackageFromJavaHome();
        }

        return jpackage;
    }

    private void validateParameters() {
        if (appVersion == null) {
            appVersion = project.getVersion();
        }
    }

    private void execute(String cmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> parameters = new ArrayList<>();
        parameters.add(cmd.contains(" ") ? ("\"" + cmd + "\"") : cmd);

        buildParameters(parameters);
        processBuilder.command(parameters);

        Process process = processBuilder.start();

        getLog().info("jpackage output:");

        int status = process.waitFor();

        logCmdOutput(process.getInputStream());
        logCmdOutput(process.getErrorStream());

        if (status != 0) {
            throw new MojoExecutionException("Error while executing jpackage");
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
        addParameter(parameters, "--vendor", vendor);
        addParameter(parameters, "--module", module);
        addParameter(parameters, "--main-class", mainClass);
        addPathParameter(parameters, "--main-jar", mainJar);
        addPathParameter(parameters, "--module-path", modulePath);
        addPathParameter(parameters, "--icon", icon);

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

        Path path = new File(value).toPath();
        if (!path.isAbsolute()) {
            String oldValue = value;
            value = project.getBasedir().getAbsolutePath() + File.separator + value;
            getLog().debug("Resolving path " + oldValue + " to " + value);
        }

        addParameter(params, name, value);
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
