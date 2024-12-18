package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class GlobalDefinitionDeserializer extends DefinitionDeserializer<GlobalDefinition> {
  public GlobalDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public GlobalDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    TypeString typeName = getTypeString(context, obj, "type_n");
    TypeString aliasedTypeName = getTypeString(context, obj, "alias_type_n");

    return new GlobalDefinition(
        base.getLocation(),
        getTimestamp(base.getLocation()),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        typeName,
        aliasedTypeName);
  }
}
