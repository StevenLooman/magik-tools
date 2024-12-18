package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;

public class MagikFileDefinitionSerializer extends BaseSerializer<MagikFileDefinition> {
  @Override
  public JsonElement serialize(
      MagikFileDefinition def, Type type, JsonSerializationContext context) {
    final JsonObject obj = new JsonObject();

    final JsonObject src = new JsonObject();
    field(src, context, "uri", def.getUri());
    field(obj, context, "src", src);

    return obj;
  }
}
