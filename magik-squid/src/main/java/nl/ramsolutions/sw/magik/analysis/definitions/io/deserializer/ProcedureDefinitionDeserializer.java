package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class ProcedureDefinitionDeserializer extends DefinitionDeserializer<ProcedureDefinition> {
  public ProcedureDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ProcedureDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    final JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    Set<ProcedureDefinition.Modifier> modifiers =
        getSet(context, obj, "mods", ProcedureDefinition.Modifier.class);
    TypeString typeName = getTypeString(context, obj, "type_n");

    String procedureName = nullableString(obj, "proc_name");

    List<ParameterDefinition> parameters =
        getList(context, obj, "params", ParameterDefinition.class);
    ExpressionResultString returnTypes = get(context, obj, "ret", ExpressionResultString.class);
    ExpressionResultString loopTypes = get(context, obj, "loop", ExpressionResultString.class);

    return new ProcedureDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        modifiers,
        typeName,
        procedureName,
        parameters,
        returnTypes,
        loopTypes);
  }
}
