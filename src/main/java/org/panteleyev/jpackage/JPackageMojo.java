package org.panteleyev.jpackage;

/*
 * Copyright (c) Petr Panteleyev. All rights reserved.
 * Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import static org.panteleyev.jpackage.OsUtil.isMac;
import static org.panteleyev.jpackage.OsUtil.isWindows;

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
    private ImageType type;

    @Parameter(required = true)
    private String name;

    @Parameter
    private String appVersion;

    @Parameter(defaultValue = "")
    private String vendor;

    @Parameter
    private String icon;

    @Parameter
    private String runtimeImage;

    @Parameter
    private String input;

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

    private String getJPackageExecutable() throws MojoExecutionException {
        Toolchain jdk = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (jdk == null) {
            throw new MojoExecutionException("Toolchain not configured");
        }

        String jpackage = jdk.findTool("jpackage");
        if (jpackage == null) {
            throw new MojoExecutionException("jpackage is not part of configured toolchain");
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
        parameters.add(cmd);

        buildParameters(parameters);
        processBuilder.command(parameters);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
        }

        int status = process.waitFor();
        if (status != 0) {
            throw new MojoExecutionException("Error while executing jpackage");
        }
    }

    private void buildParameters(List<String> parameters) {
        getLog().info("=== Parameters:");

        if (type != null) {
            getLog().info("type = " + type);
            parameters.add("--type");
            parameters.add(type.getValue());
        }

        getLog().info("name = " + name);
        parameters.add("--name");
        parameters.add(name);

        getLog().info("appVersion = " + appVersion);
        parameters.add("--app-version");
        parameters.add(appVersion);

        getLog().info("destination = " + destination);
        parameters.add("--dest");
        parameters.add(destination);

        if (copyright != null) {
            getLog().info("copyright = " + copyright);
            parameters.add("--copyright");
            parameters.add(copyright);
        }

        if (description != null) {
            getLog().info("description = " + description);
            parameters.add("--description");
            parameters.add(description);
        }

        if (runtimeImage != null) {
            getLog().info("runtimeImage = " + runtimeImage);
            parameters.add("--runtime-image");
            parameters.add(runtimeImage);
        }

        if (input != null) {
            getLog().info("input = " + input);
            parameters.add("--input");
            parameters.add(input);
        }

        if (vendor != null) {
            getLog().info("vendor = " + vendor);
            parameters.add("--vendor");
            parameters.add(vendor);
        }

        if (module != null) {
            getLog().info("module = " + module);
            parameters.add("--module");
            parameters.add(module);
        }

        if (mainClass != null) {
            getLog().info("mainClass = " + mainClass);
            parameters.add("--main-class");
            parameters.add(mainClass);
        }

        if (mainJar != null) {
            getLog().info("mainJar = " + mainJar);
            parameters.add("--main-jar");
            parameters.add(mainJar);
        }

        if (modulePath != null) {
            getLog().info("modulePath = " + modulePath);
            parameters.add("--module-path");
            parameters.add(modulePath);
        }

        if (icon != null) {
            getLog().info("icon = " + icon);
            parameters.add("--icon");
            parameters.add(icon);
        }

        if (javaOptions != null && javaOptions.length > 0) {
            String options = String.join(" ", javaOptions);
            getLog().info("javaOptions = " + options);
            parameters.add("--java-options");
            parameters.add(options);
        }

        if (isMac()) {

        } else if (isWindows()) {
            buildWindowsParameters(parameters);
        }

        getLog().info("===");
    }

    private void buildWindowsParameters(List<String> parameters) {
        if (winMenu) {
            getLog().info("winMenu = " + winMenu);
            parameters.add("--win-menu");
        }

        if (winDirChooser) {
            getLog().info("winDirChooser = " + winDirChooser);
            parameters.add("--win-dir-chooser");
        }

        if (winUpgradeUuid != null) {
            getLog().info("winUpgradeUuid = " + winUpgradeUuid);
            parameters.add("--win-upgrade-uuid");
            parameters.add(winUpgradeUuid);
        }

        if (winMenuGroup != null) {
            getLog().info("winMenuGroup = " + winMenuGroup);
            parameters.add("--win-menu-group");
            parameters.add(winMenuGroup);
        }

        if (winShortcut) {
            getLog().info("winShortcut = " + winShortcut);
            parameters.add("--win-shortcut");
        }

        if (winPerUserInstall) {
            getLog().info("winPerUserInstall = " + winPerUserInstall);
            parameters.add("--win-per-user-install");
        }
    }
}
