package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class SlotDefinitionDeserializer extends DefinitionDeserializer<SlotDefinition> {
  public SlotDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public SlotDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);

    final String name = getStringField(obj, "name");
    final TypeString typeName = getTypeString(context, obj, "type_n");

    return new SlotDefinition(
        base.getLocation(),
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        null,
        name,
        typeName);
  }
}
