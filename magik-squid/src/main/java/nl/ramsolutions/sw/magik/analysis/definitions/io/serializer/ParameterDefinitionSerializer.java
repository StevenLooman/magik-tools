package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;

public class ParameterDefinitionSerializer extends BaseSerializer<ParameterDefinition> {
  @Override
  public JsonElement serialize(
      ParameterDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "name", src.getName());
    field(obj, context, "mod", src.getModifier());
    field(obj, context, "type_n", src.getTypeName());

    return obj;
  }
}
