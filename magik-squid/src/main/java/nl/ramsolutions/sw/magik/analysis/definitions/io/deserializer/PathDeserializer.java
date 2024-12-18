package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;

public class PathDeserializer extends BaseDeserializer<Path> {

  public PathDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public Path deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(json + " is not a string");
    }

    final String value = json.getAsJsonPrimitive().getAsString();
    return Paths.get(value);
  }
}
