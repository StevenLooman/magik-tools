package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;

public class SlotDefinitionSerializer extends BaseSerializer<SlotDefinition> {
  @Override
  public JsonElement serialize(
      SlotDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "name", src.getName());
    field(obj, context, "type_n", src.getTypeName());

    return obj;
  }
}
