package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

public class ExpressionResultStringDeserializer extends BaseDeserializer<ExpressionResultString> {
  public ExpressionResultStringDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ExpressionResultString deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    String str = asString(json);
    if (str != null) {
      if (str.equals(ExpressionResultString.UNDEFINED_SERIALIZED_NAME)) {
        return ExpressionResultString.UNDEFINED;
      }

      return new ExpressionResultString(TypeStringParser.parseTypeString(str));
    } else if (json.isJsonArray()) {
      JsonArray jsonArray = json.getAsJsonArray();
      final List<TypeString> types =
          jsonArray.asList().stream()
              .map(BaseDeserializer::asString)
              .filter(Objects::nonNull)
              .map(TypeStringParser::parseTypeString)
              .toList();
      return new ExpressionResultString(types);
    } else {
      return ExpressionResultString.EMPTY;
    }
  }
}
