package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;

public class PackageDefinitionSerializer extends BaseSerializer<PackageDefinition> {
  @Override
  public JsonElement serialize(
      PackageDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "name", src.getName());
    field(obj, context, "uses", src.getUses());

    return obj;
  }
}
