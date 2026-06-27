// Copyright © 2026 Petr Panteleyev
// SPDX-License-Identifier: BSD-2-Clause
package org.panteleyev.jpackage;

/**
 * Convenience class to define {@code jlink} options.
 */
public class JLink {
    private boolean bindServices;
    private boolean noHeaderFiles;
    private boolean noManPages;
    private boolean stripDebug;
    private boolean stripNativeCommands;
    private boolean generateCdsArchive;

    /**
     * Creates new instance of the class.
     */
    public JLink() {
    }

    JLink(boolean bindServices, boolean noHeaderFiles, boolean noManPages, boolean stripDebug,
            boolean stripNativeCommands, boolean generateCdsArchive)
    {
        this.bindServices = bindServices;
        this.noHeaderFiles = noHeaderFiles;
        this.noManPages = noManPages;
        this.stripDebug = stripDebug;
        this.stripNativeCommands = stripNativeCommands;
        this.generateCdsArchive = generateCdsArchive;
    }

    /**
     * <p>Defines if {@code --bind-services option} is added.</p>
     *
     * @param bindServices --bind-services
     */
    public void setBindServices(boolean bindServices) {
        this.bindServices = bindServices;
    }

    /**
     * <p>Defines if {@code --no-header-files} is added.</p>
     *
     * @param noHeaderFiles --no-header-files
     */
    public void setNoHeaderFiles(boolean noHeaderFiles) {
        this.noHeaderFiles = noHeaderFiles;
    }

    /**
     * <p>Defines if {@code --no-man-pages} is added.</p>
     *
     * @param noManPages --no-man-pages
     */
    public void setNoManPages(boolean noManPages) {
        this.noManPages = noManPages;
    }

    /**
     * <p>Defines if {@code --strip-debug} is added.</p>
     *
     * @param stripDebug --strip-debug
     */
    public void setStripDebug(boolean stripDebug) {
        this.stripDebug = stripDebug;
    }

    /**
     * <p>Defines if {@code --strip-native-commands} is added.</p>
     *
     * @param stripNativeCommands --strip-native-commands
     */
    public void setStripNativeCommands(boolean stripNativeCommands) {
        this.stripNativeCommands = stripNativeCommands;
    }

    /**
     * <p>Defines if {@code --generate-cds-archive} is added.</p>
     *
     * @param generateCdsArchive --generate-cds-archive
     */
    public void setGenerateCdsArchive(boolean generateCdsArchive) {
        this.generateCdsArchive = generateCdsArchive;
    }

    String build() {
        return (
                (bindServices ? "--bind-services " : "")
                        + (noHeaderFiles ? "--no-header-files " : "")
                        + (noManPages ? "--no-man-pages " : "")
                        + (stripDebug ? "--strip-debug " : "")
                        + (stripNativeCommands ? "--strip-native-commands " : "")
                        + (generateCdsArchive ? "--generate-cds-archive " : "")
        ).trim();
    }
}
