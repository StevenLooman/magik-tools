package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;

public class ModuleDefinitionSerializer extends BaseSerializer<ModuleDefinition> {
  @Override
  public JsonElement serialize(
      ModuleDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    field(obj, context, "name", src.getName());
    field(obj, context, "base_ver", src.getBaseVersion());
    field(obj, context, "cur_ver", src.getBaseVersion());
    field(obj, context, "prod", src.getProduct());
    field(obj, context, "usgs", src.getUsages());
    location(obj, context, src.getLocation());

    return obj;
  }
}
