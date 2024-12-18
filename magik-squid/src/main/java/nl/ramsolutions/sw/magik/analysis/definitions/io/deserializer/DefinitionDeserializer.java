package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonObject;
import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;

public abstract class DefinitionDeserializer<T> extends BaseDeserializer<T> {
  public DefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  public static class DeserializedDefinition extends MagikDefinition {
    /**
     * Constructor.
     *
     * @param location Location.
     * @param moduleName Name of the module this definition resides in.
     * @param doc Doc.
     * @param node Node.
     */
    protected DeserializedDefinition(
        @Nullable Location location,
        @Nullable Instant timestamp,
        @Nullable String moduleName,
        @Nullable String doc,
        @Nullable AstNode node) {
      super(location, timestamp, moduleName, doc, node);
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public IDefinition getBareDefinition() {
      return new DeserializedDefinition(
          this.getLocation(), this.getTimestamp(), this.getModuleName(), this.getDoc(), null);
    }
  }

  public MagikDefinition getDefinition(JsonObject obj) {
    Location location = getLocation(obj);
    return new DeserializedDefinition(
        location,
        getTimestamp(location),
        nullableString(obj, "mod_n"),
        nullableString(obj, "doc"),
        null);
  }
}
