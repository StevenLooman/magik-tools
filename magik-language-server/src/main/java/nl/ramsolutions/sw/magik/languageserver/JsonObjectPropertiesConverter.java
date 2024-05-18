package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nl.ramsolutions.sw.MagikToolsProperties;

/** Helper class to convert JsonObject into Properties key/values. */
final class JsonObjectPropertiesConverter {

  private JsonObjectPropertiesConverter() {}

  static Properties convert(final JsonObject jsonObject) {
    final Properties properties = new Properties();
    JsonObjectPropertiesConverter.convert("", jsonObject, properties);
    return properties;
  }

  static void convert(
      final String path, final JsonElement jsonElement, final Properties properties) {
    if (jsonElement.isJsonArray()) {
      final JsonArray jsonArray = (JsonArray) jsonElement;

      // Ensure all values in jsonArray are primitive.
      // final Iterable<JsonElement> iterable = () -> jsonArray.iterator();
      final boolean containsNonPrimitives =
          StreamSupport.stream(jsonArray.spliterator(), false)
              .anyMatch(childJsonEl -> !childJsonEl.isJsonPrimitive());
      if (containsNonPrimitives) {
        throw new IllegalStateException("Cannot convert JsonArray with non-JsonPrimitives!");
      }

      // Convert all values.
      final String value =
          StreamSupport.stream(jsonArray.spliterator(), false)
              .map(childJsonEl -> childJsonEl.getAsString())
              .collect(Collectors.joining(MagikToolsProperties.LIST_SEPARATOR));
      properties.put(path, value);
    } else if (jsonElement.isJsonObject()) {
      final JsonObject jsonObject = (JsonObject) jsonElement;
      jsonObject
          .entrySet()
          .forEach(
              entry -> {
                final String key = entry.getKey();
                final JsonElement value = entry.getValue();
                final String childPath = path.isEmpty() ? key : path + "." + key;
                JsonObjectPropertiesConverter.convert(childPath, value, properties);
              });
    } else if (jsonElement.isJsonNull()) {
      properties.put(path, null);
    } else if (jsonElement.isJsonPrimitive()) {
      final JsonPrimitive jsonPrimitive = (JsonPrimitive) jsonElement;
      final String value = jsonPrimitive.getAsString();
      properties.put(path, value);
    }
  }
}
