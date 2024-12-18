package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;

public class ModuleDefinitionDeserializer extends BaseDeserializer<ModuleDefinition> {
  public ModuleDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ModuleDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    String name = getStringField(obj, "name");
    String baseVersion = getStringField(obj, "base_ver");
    String currentVersion = nullableString(obj, "cur_ver");
    String product = nullableString(obj, "prod");
    List<ModuleUsage> usages = getList(context, obj, "usgs", ModuleUsage.class);

    Location location = getLocation(obj);

    return new ModuleDefinition(
        location, getTimestamp(location), name, product, baseVersion, currentVersion, null, usages);
  }
}
