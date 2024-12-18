package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;

public class ProcedureDefinitionSerializer extends BaseSerializer<ProcedureDefinition> {
  @Override
  public JsonElement serialize(
      ProcedureDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "mods", src.getModifiers());
    field(obj, context, "type_n", src.getTypeString());
    field(obj, context, "proc_name", src.getProcedureName());
    field(obj, context, "params", src.getParameters());
    field(obj, context, "ret", src.getReturnTypes());
    field(obj, context, "loop", src.getLoopTypes());

    return obj;
  }
}
