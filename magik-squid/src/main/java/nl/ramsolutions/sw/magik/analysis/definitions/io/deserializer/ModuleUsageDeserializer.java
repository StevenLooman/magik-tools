package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.moduledef.ModuleUsage;

public class ModuleUsageDeserializer extends BaseDeserializer<ModuleUsage> {
  public ModuleUsageDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ModuleUsage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();
    String name = getStringField(obj, "name");
    return new ModuleUsage(name, null);
  }
}
