package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.productdef.ProductDefinition;

public class ProductDefinitionSerializer extends BaseSerializer<ProductDefinition> {
  @Override
  public JsonElement serialize(
      ProductDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    location(obj, context, src.getLocation());

    field(obj, context, "name", src.getName());
    field(obj, context, "ver", src.getVersion());
    field(obj, context, "ver_com", src.getVersionComment());
    field(obj, context, "parent", src.getParent());
    field(obj, context, "usage", src.getUsages());

    return obj;
  }
}
