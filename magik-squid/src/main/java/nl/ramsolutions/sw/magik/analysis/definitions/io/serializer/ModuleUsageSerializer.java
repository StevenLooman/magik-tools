package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.moduledef.ModuleUsage;

public class ModuleUsageSerializer extends BaseSerializer<ModuleUsage> {
  @Override
  public JsonElement serialize(ModuleUsage src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    field(obj, context, "name", src.getName());
    return obj;
  }
}
