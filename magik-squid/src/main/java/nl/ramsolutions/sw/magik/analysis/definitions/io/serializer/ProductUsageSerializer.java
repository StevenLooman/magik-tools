package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.productdef.ProductUsage;

public class ProductUsageSerializer extends BaseSerializer<ProductUsage> {
  @Override
  public JsonElement serialize(ProductUsage src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    field(obj, context, "name", src.getName());

    return obj;
  }
}
