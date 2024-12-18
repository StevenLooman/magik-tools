package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class TypeStringSerializer extends BaseSerializer<TypeString> {
  @Override
  public JsonElement serialize(TypeString src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getFullString());
  }
}
