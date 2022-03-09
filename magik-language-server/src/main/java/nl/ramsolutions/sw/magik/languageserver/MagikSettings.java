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
public class MagikSettings {

    /**
     * Default MagikSettings, to prevent NPEs.
     */
    public static final MagikSettings DEFAULT = new MagikSettings(new JsonObject());

    private static final String TOP_LEVEL = "magik";

    private final JsonObject settings;

    /**
     * Constructor.
     * @param settings Settings from client.
     */
    public MagikSettings(final JsonObject settings) {
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

        final JsonElement smallworldGis = magik.get("smallworldGis");
        if (smallworldGis == null) {
            return null;
        }

        return smallworldGis.getAsString();
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

        final JsonObject typing = magik.getAsJsonObject("typing");
        if (typing == null) {
            return Collections.emptyList();
        }

        final JsonArray typesDatabasePaths = typing.getAsJsonArray("typeDatabasePaths");
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
     * Get magik.typing.enableChecks, defaults to false if no config is provided.
     * @return magik.typing.enableChecks.
     */
    @CheckForNull
    public Boolean getTypingEnableChecks() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        if (magik == null) {
            return false;
        }

        final JsonObject typing = magik.getAsJsonObject("typing");
        if (typing == null) {
            return false;
        }

        final JsonElement enableChecks = typing.get("enableChecks");
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

        final JsonObject lint = magik.getAsJsonObject("lint");
        if (lint == null) {
            return null;
        }

        final JsonElement overrideConfigFile = lint.get("overrideConfigFile");
        if (overrideConfigFile == null) {
            return null;
        }

        final String overrideConfigFileStr = overrideConfigFile.getAsString();
        if (overrideConfigFileStr == null) {
            return null;
        }

        return Path.of(overrideConfigFileStr);
    }

}
