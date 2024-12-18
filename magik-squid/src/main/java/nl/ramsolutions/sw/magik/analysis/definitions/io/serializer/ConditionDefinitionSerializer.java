package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;

public class ConditionDefinitionSerializer extends BaseSerializer<ConditionDefinition> {
  @Override
  public JsonElement serialize(
      ConditionDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "name", src.getName());
    field(obj, context, "par", src.getParent());
    field(obj, context, "d_names", src.getDataNames());

    return obj;
  }
}
