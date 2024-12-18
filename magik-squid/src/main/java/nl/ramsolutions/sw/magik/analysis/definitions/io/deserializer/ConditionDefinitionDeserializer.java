package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;

public class ConditionDefinitionDeserializer extends DefinitionDeserializer<ConditionDefinition> {
  public ConditionDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ConditionDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    String name = getStringField(obj, "name");
    String parent = nullableString(obj, "par");
    List<String> dataNames = getList(context, obj, "d_names", String.class);

    return new ConditionDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        base.getNode(),
        name,
        parent,
        dataNames);
  }
}
