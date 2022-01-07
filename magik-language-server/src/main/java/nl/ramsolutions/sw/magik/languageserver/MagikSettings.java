package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;

/**
 * Magik settings.
 */
public class MagikSettings {

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
        return magik.get("smallworldGis").getAsString();
    }

    /**
     * Get magik.typing.typeDatabasePath.
     * @return magik.typing.typeDatabasePath.
     */
    public List<String> getTypingTypeDatabasePaths() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        final JsonObject typing = magik.getAsJsonObject("typing");
        final JsonArray typesDatabasePaths = typing.getAsJsonArray("typeDatabasePaths");

        final List<String> paths = new ArrayList<>();
        typesDatabasePaths.forEach(jsonElement -> {
            final String path = jsonElement.getAsString();
            paths.add(path);
        });
        return paths;
    }

    /**
     * Get magik.typing.enableChecks.
     * @return magik.typing.enableChecks.
     */
    @CheckForNull
    public Boolean getTypingEnableChecks() {
        final JsonObject magik = this.settings.getAsJsonObject(TOP_LEVEL);
        final JsonObject typing = magik.getAsJsonObject("typing");
        return typing.get("enableChecks").getAsBoolean();
    }

}
