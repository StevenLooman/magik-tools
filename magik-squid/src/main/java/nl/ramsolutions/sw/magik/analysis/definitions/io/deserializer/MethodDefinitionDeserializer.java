package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.*;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class MethodDefinitionDeserializer extends DefinitionDeserializer<MethodDefinition> {
  public MethodDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public MethodDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    TypeString typeName = getTypeString(context, obj, "type_n");
    String methodName = getStringField(obj, "m_name");

    Location location = base.getLocation();
    if (location != null) {
      MagikDefinition parsed = getParsedDefinition(location, methodName, MethodDefinition.class);
      if (parsed != null) {
        location = parsed.getLocation();
      }
    }

    Set<MethodDefinition.Modifier> modifiers =
        getSet(context, obj, "mods", MethodDefinition.Modifier.class);
    List<ParameterDefinition> parameters =
        getList(context, obj, "params", ParameterDefinition.class);

    ParameterDefinition assignmentParameter =
        get(context, obj, "a_param", ParameterDefinition.class);

    Set<String> topics = getSet(context, obj, "top", String.class);

    // if no return type is defined check for new* or init*
    //   -> assume the method returns an object of the same exemplar
    ExpressionResultString returnTypes = get(context, obj, "ret", ExpressionResultString.class);
    if ((methodName.startsWith("new") || methodName.startsWith("init"))
        && returnTypes.equals(ExpressionResultString.UNDEFINED)) {
      returnTypes = new ExpressionResultString(typeName);
    }

    ExpressionResultString loopTypes = get(context, obj, "loop", ExpressionResultString.class);

    List<GlobalUsage> usedGlobals = getList(context, obj, "u_globals", GlobalUsage.class);
    List<MethodUsage> usedMethods = getList(context, obj, "u_methods", MethodUsage.class);
    List<SlotUsage> usedSlots = getList(context, obj, "u_slots", SlotUsage.class);
    List<ConditionUsage> usedConditions = getList(context, obj, "u_conds", ConditionUsage.class);

    return new MethodDefinition(
        location,
        getTimestamp(location),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        typeName,
        methodName,
        modifiers,
        parameters,
        assignmentParameter,
        topics,
        returnTypes,
        loopTypes,
        usedGlobals,
        usedMethods,
        usedSlots,
        usedConditions);
  }
}
