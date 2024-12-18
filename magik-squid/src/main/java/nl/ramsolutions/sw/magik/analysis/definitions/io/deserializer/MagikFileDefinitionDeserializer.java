package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;

public class MagikFileDefinitionDeserializer extends DefinitionDeserializer<MagikFileDefinition> {
  public MagikFileDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public MagikFileDefinition deserialize(
      JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    JsonObject srcObj = obj.getAsJsonObject("src");
    if (srcObj == null || !srcObj.isJsonObject() || !srcObj.has("uri")) {
      throw new JsonParseException("src.uri for magik file is missing");
    }
    Location loc = parseUri(getStringField(srcObj, "uri"));
    if (loc == null) {
      throw new JsonParseException("src.uri for a magik file can't be parsed");
    }

    return new MagikFileDefinition(loc, null);
  }
}
