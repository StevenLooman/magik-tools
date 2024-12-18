package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.ProductUsage;

public class ProductDefinitionDeserializer extends BaseDeserializer<ProductDefinition> {
  public ProductDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ProductDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    final JsonObject obj = json.getAsJsonObject();

    String name = getStringField(obj, "name");
    String version = nullableString(obj, "ver");
    String versionComment = nullableString(obj, "ver_com");
    String title = nullableString(obj, "title");
    String description = nullableString(obj, "desc");
    String parent = nullableString(obj, "parent");
    List<ProductUsage> usages = getList(context, obj, "usage", ProductUsage.class);

    Location location = getLocation(obj);

    return new ProductDefinition(
        location,
        getTimestamp(location),
        name,
        parent,
        version,
        versionComment,
        title,
        description,
        usages);
  }
}
