package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;

public class BinaryOperatorDefinitionSerializer extends BaseSerializer<BinaryOperatorDefinition> {
  @Override
  public JsonElement serialize(
      BinaryOperatorDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "op", src.getOperator());
    field(obj, context, "lhs_type_n", src.getLhsTypeName());
    field(obj, context, "rhs_type_n", src.getRhsTypeName());
    field(obj, context, "res_type_n", src.getResultTypeName());

    return obj;
  }
}
