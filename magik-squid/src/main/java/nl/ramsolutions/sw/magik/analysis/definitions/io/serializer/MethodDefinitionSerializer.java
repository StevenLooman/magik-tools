package nl.ramsolutions.sw.magik.analysis.definitions.io.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;

public class MethodDefinitionSerializer extends BaseSerializer<MethodDefinition> {
  @Override
  public JsonElement serialize(
      MethodDefinition src, Type typeOfSrc, JsonSerializationContext context) {
    final JsonObject obj = new JsonObject();

    addDefinition(obj, context, src);

    field(obj, context, "type_n", src.getTypeName());
    field(obj, context, "m_name", src.getMethodName());
    field(obj, context, "mods", src.getModifiers());
    field(obj, context, "params", src.getParameters());
    field(obj, context, "a_param", src.getAssignmentParameter());
    field(obj, context, "top", src.getTopics());
    field(obj, context, "loop", src.getLoopTypes());
    field(obj, context, "u_globals", src.getUsedGlobals());
    field(obj, context, "u_methods", src.getUsedMethods());
    field(obj, context, "u_slots", src.getUsedSlots());
    field(obj, context, "u_conds", src.getUsedConditions());

    return obj;
  }
}
