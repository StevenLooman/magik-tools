package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;

/**
 * Magik settings.
 */
public final class MagikSettings {  // NOSONAR

    /**
     * Singleton instance.
     */
    public static final MagikSettings INSTANCE = new MagikSettings();

    private static final String TOP_LEVEL = "magik";
    private static final String TYPING = "typing";
    private static final String LINT = "lint";
    private static final String SMALLWORLD_GIS = "smallworldGis";
    private static final String LIBS_DIRS = "libsDirs";
    private static final String TYPE_DATABASE_PATHS = "typeDatabasePaths";
    private static final String SHOW_ATOM_INLAY_HINTS = "showAtomInlayHints";
    private static final String SHOW_ARGUMENT_INLAY_HINTS = "showArgumentInlayHints";
    private static final String ENABLE_CHECKS = "enableChecks";
    private static final String OVERRIDE_CONFIG_FILE = "overrideConfigFile";

    private JsonObject settings = new JsonObject();

    /**
     * Private constructor.
     */
    private MagikSettings() {
    }

    /**
     * Set new settings.
     * @param settings
     */
    public void setSettings(final JsonObject settings) {
        this.settings = settings;
    }

    /**
     * Get magik.smallworldGis.
     * @return magik.smallworldGis.
     */
    @CheckForNull
    public String getSmallworldGis() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return null;
        }

        final JsonElement smallworldGis = magik.get(SMALLWORLD_GIS);
        if (smallworldGis == null) {
            return null;
        }

        return smallworldGis.getAsString();
    }

    /**
     * Get magik.libsDirs.
     * @return magik.libsDirs.
     */
    public List<String> getLibsDirs() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return Collections.emptyList();
        }

        final JsonArray libsDirs = magik.getAsJsonArray(LIBS_DIRS);
        if (libsDirs == null) {
            return Collections.emptyList();
        }

        final List<String> paths = new ArrayList<>();
        libsDirs.forEach(jsonElement -> {
            final String path = jsonElement.getAsString();
            paths.add(path);
        });
        return paths;
    }

    /**
     * Get magik.typing.typeDatabasePath.
     * @return magik.typing.typeDatabasePath.
     */
    public List<String> getTypingTypeDatabasePaths() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return Collections.emptyList();
        }

        final JsonObject typing = magik.getAsJsonObject(TYPING);
        if (typing == null) {
            return Collections.emptyList();
        }

        final JsonArray typesDatabasePaths = typing.getAsJsonArray(TYPE_DATABASE_PATHS);
        if (typesDatabasePaths == null) {
            return Collections.emptyList();
        }

        final List<String> paths = new ArrayList<>();
        typesDatabasePaths.forEach(jsonElement -> {
            final String path = jsonElement.getAsString();
            paths.add(path);
        });
        return paths;
    }

    /**
     * Get magik.typing.showAtomInlayHints.
     * @return magik.typing.showAtomInlayHints
     */
    public boolean getTypingShowAtomInlayHints() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return false;
        }

        final JsonObject typing = magik.getAsJsonObject(TYPING);
        if (typing == null) {
            return false;
        }

        final JsonElement showAtomInlayHints = typing.get(SHOW_ATOM_INLAY_HINTS);
        if (showAtomInlayHints == null) {
            return false;
        }

        return showAtomInlayHints.getAsBoolean();
    }

    /**
     * Get magik.typing.showArgumentInlayHints.
     * @return magik.typing.showArgumentInlayHints
     */
    public boolean getTypingShowArgumentInlayHints() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return true;
        }

        final JsonObject typing = magik.getAsJsonObject(TYPING);
        if (typing == null) {
            return true;
        }

        final JsonElement showArgumentInlayHints = typing.get(SHOW_ARGUMENT_INLAY_HINTS);
        if (showArgumentInlayHints == null) {
            return true;
        }

        return showArgumentInlayHints.getAsBoolean();
    }

    /**
     * Get magik.typing.enableChecks, defaults to false if no config is provided.
     * @return magik.typing.enableChecks.
     */
    @CheckForNull
    public Boolean getTypingEnableChecks() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return false;
        }

        final JsonObject typing = magik.getAsJsonObject(TYPING);
        if (typing == null) {
            return false;
        }

        final JsonElement enableChecks = typing.get(ENABLE_CHECKS);
        if (enableChecks == null) {
            return false;
        }

        return enableChecks.getAsBoolean();
    }

    /**
     * Get magik.lint.overrideConfigFile.
     * @return magik.lint.overrideConfigFile
     */
    @CheckForNull
    public Path getChecksOverrideSettingsPath() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return null;
        }

        final JsonObject lint = magik.getAsJsonObject(LINT);
        if (lint == null) {
            return null;
        }

        final JsonElement overrideConfigFile = lint.get(OVERRIDE_CONFIG_FILE);
        if (overrideConfigFile == null) {
            return null;
        }

        final String overrideConfigFileStr = overrideConfigFile.getAsString();
        if (overrideConfigFileStr == null
            || overrideConfigFileStr.isEmpty()) {
            return null;
        }

        return Path.of(overrideConfigFileStr);
    }

}
