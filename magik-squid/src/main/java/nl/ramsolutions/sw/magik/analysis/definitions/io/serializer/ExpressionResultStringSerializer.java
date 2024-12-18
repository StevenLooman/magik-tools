package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class ExpressionResultStringSerializer extends BaseSerializer<ExpressionResultString> {
  @Override
  public JsonElement serialize(
      ExpressionResultString src, Type typeOfSrc, JsonSerializationContext context) {
    JsonElement element;

    if (src.equals(ExpressionResultString.EMPTY)) {
      element = new JsonPrimitive("");
    } else if (src.equals(ExpressionResultString.UNDEFINED)) {
      element = new JsonPrimitive(ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
    } else if (src.size() > 1) {
      JsonArray array = new JsonArray(src.size());

      src.stream()
          .forEach(typeString -> array.add(context.serialize(typeString, TypeString.class)));

      element = array;
    } else {
      element = new JsonPrimitive(src.get(0, TypeString.UNDEFINED).getFullString());
    }

    return element;
  }
}
