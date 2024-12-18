package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;

public abstract class BaseSerializer<T> implements JsonSerializer<T> {
  public static void field(
      JsonObject obj, JsonSerializationContext ctx, String fieldName, @Nullable Object value) {
    obj.add(fieldName, ctx.serialize(value));
  }

  public static void location(JsonObject obj, JsonSerializationContext ctx, Location location) {
    field(obj, ctx, "src", location);
  }

  public static void addDefinition(
      JsonObject obj, JsonSerializationContext ctx, MagikDefinition definition) {
    location(obj, ctx, definition.getLocation());
    field(obj, ctx, "mod_n", definition.getModuleName());
    field(obj, ctx, "doc", definition.getDoc());
  }
}
