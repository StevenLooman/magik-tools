package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class ParameterDefinitionDeserializer extends DefinitionDeserializer<ParameterDefinition> {
  public ParameterDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ParameterDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    final JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    String name = getStringField(obj, "name");

    ParameterDefinition.Modifier modifier =
        get(context, obj, "mod", ParameterDefinition.Modifier.class);
    TypeString typeName = getTypeString(context, obj, "type_n");

    return new ParameterDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        name,
        modifier,
        typeName);
  }
}
