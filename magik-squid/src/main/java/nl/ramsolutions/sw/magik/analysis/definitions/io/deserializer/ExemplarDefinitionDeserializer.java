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
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public class ExemplarDefinitionDeserializer extends DefinitionDeserializer<ExemplarDefinition> {
  public ExemplarDefinitionDeserializer(List<PathMapping> mappings) {
    super(mappings);
  }

  @Override
  public ExemplarDefinition deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    MagikDefinition base = getDefinition(obj);
    TypeString typeName = getTypeString(context, obj, "type_n");

    ExemplarDefinition.Sort sort = get(context, obj, "sort", ExemplarDefinition.Sort.class);
    List<SlotDefinition> slots = getList(context, obj, "slots", SlotDefinition.class);
    List<TypeString> parents = getList(context, obj, "par", TypeString.class);
    Set<String> topics = getSet(context, obj, "top", String.class);

    Location location = base.getLocation();
    if (location != null) {
      MagikDefinition parsed =
          getParsedDefinition(location, typeName.getFullString(), ExemplarDefinition.class);
      if (parsed != null) {
        location = parsed.getLocation();
      }
    }

    return new ExemplarDefinition(
        location,
        base.getTimestamp(),
        base.getModuleName(),
        base.getDoc(),
        null,
        sort,
        typeName,
        slots,
        parents,
        topics);
  }
}
