package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;

public class ExemplarDefinitionSerializer extends BaseSerializer<ExemplarDefinition> {
  @Override
  public JsonElement serialize(
      ExemplarDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "type_n", src.getTypeString());
    field(obj, context, "sort", src.getSort());
    field(obj, context, "slots", src.getSlots());
    field(obj, context, "par", src.getParents());

    return obj;
  }
}
