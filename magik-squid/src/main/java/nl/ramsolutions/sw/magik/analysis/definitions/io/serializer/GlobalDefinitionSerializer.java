package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;

public class GlobalDefinitionSerializer extends BaseSerializer<GlobalDefinition> {
  @Override
  public JsonElement serialize(
      GlobalDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "type_n", src.getTypeString());
    field(obj, context, "alias_type_n", src.getAliasedTypeName());

    return obj;
  }
}
