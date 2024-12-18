package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;

public class PackageDefinitionDeserializer extends DefinitionDeserializer<PackageDefinition> {
  public PackageDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public PackageDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    String name = getStringField(obj, "name");
    List<String> uses = getList(context, obj, "uses", String.class);

    return new PackageDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        name,
        uses);
  }
}
