package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.productdef.ProductUsage;

public class ProductUsageDeserializer extends BaseDeserializer<ProductUsage> {
  public ProductUsageDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ProductUsage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    final JsonObject obj = json.getAsJsonObject();
    String name = getStringField(obj, "name");
    return new ProductUsage(name, null);
  }
}
