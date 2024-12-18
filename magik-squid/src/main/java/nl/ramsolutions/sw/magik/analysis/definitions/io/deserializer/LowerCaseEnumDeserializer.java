package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;

public class LowerCaseEnumDeserializer<E extends Enum<?>> extends BaseDeserializer<E> {
  private final E[] constants;

  public LowerCaseEnumDeserializer(List<PathMapping> mappings, Class<E> clazz) {
    super(mappings);
    if (!clazz.isEnum()) {
      throw new IllegalArgumentException("Class must be an enum");
    }

    this.constants = clazz.getEnumConstants();
  }

  @Override
  public E deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    final String value = json.getAsString().toUpperCase();
    return Arrays.stream(constants)
        .filter(c -> c.toString().equals(value))
        .findFirst()
        .orElse(null);
  }
}
