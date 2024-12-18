package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class BinaryOperatorDefinitionDeserializer
    extends DefinitionDeserializer<BinaryOperatorDefinition> {
  public BinaryOperatorDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public BinaryOperatorDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    String operator = getStringField(obj, "op");

    TypeString lhsTypeName = getTypeString(context, obj, "lhs_type_n");
    TypeString rhsTypeName = getTypeString(context, obj, "rhs_type_n");
    TypeString resultTypeName = getTypeString(context, obj, "res_type_n");

    return new BinaryOperatorDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        operator,
        lhsTypeName,
        rhsTypeName,
        resultTypeName);
  }
}
