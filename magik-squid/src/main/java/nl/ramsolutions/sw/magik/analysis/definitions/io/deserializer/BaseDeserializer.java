package nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.Memoizer;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

public abstract class BaseDeserializer<T> implements JsonDeserializer<T> {
  private static final Memoizer<Path, IndexedFile> parsedFiles =
      new Memoizer<>(BaseDeserializer::computeParsedFile);

  private final List<PathMapping> mappings;
  private static MagikToolsProperties properties = new MagikToolsProperties();

  private static final Long EMPTY_INDEXED_AT = -1L;
  private static final IndexedFile EMPTY_INDEXED_FILE =
      new IndexedFile(Collections.emptyList(), EMPTY_INDEXED_AT);

  public static void setProperties(MagikToolsProperties properties) {
    BaseDeserializer.properties = properties;
  }

  private record IndexedFile(List<MagikDefinition> definitions, long indexedAt) {}

  public BaseDeserializer(List<PathMapping> mappings) {
    this.mappings = Collections.unmodifiableList(mappings);
  }

  @Nullable
  public static String nullableString(JsonObject el, String field) {
    JsonElement strNode = el.get(field);
    return asString(strNode);
  }

  @Nullable
  public static String asString(JsonElement el) {
    if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
      return null;
    }
    return el.getAsString();
  }

  public static Stream<JsonElement> getStream(JsonObject node, String field) {
    JsonElement arrNode = node.get(field);
    if (arrNode == null || !arrNode.isJsonArray()) {
      return Stream.empty();
    }
    JsonArray arr = arrNode.getAsJsonArray();

    return arr.asList().stream();
  }

  public static <X> List<X> getList(
      JsonDeserializationContext context, JsonObject node, String field, Class<X> clazz) {
    return getStream(node, field)
        .map(e -> context.deserialize(e, clazz))
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .toList();
  }

  public static <X> Set<X> getSet(
      JsonDeserializationContext context, JsonObject node, String field, Class<X> clazz) {
    return getStream(node, field)
        .map(e -> context.deserialize(e, clazz))
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .collect(Collectors.toSet());
  }

  @CheckForNull
  public Location getLocation(JsonObject node) {
    String source = nullableString(node, "src");
    if (source != null) {
      return parseLocation(source);
    }
    return null;
  }

  @CheckForNull
  public Location parseLocation(String pathStr) {
    Location location = null;

    if (pathStr != null) {
      Path path = Path.of(pathStr);
      location = Location.validLocation(new Location(path.toUri()), this.mappings);
    }

    return location;
  }

  @CheckForNull
  public Location parseUri(String uriStr) {
    Location location = null;

    if (uriStr != null) {
      final URI uri = URI.create(uriStr);
      location = Location.validLocation(new Location(uri), this.mappings);
    }

    return location;
  }

  public static String getStringField(JsonObject node, String field) {
    JsonElement strNode = node.get(field);
    try {
      return getString(strNode);
    } catch (IllegalStateException ex) {
      throw new RuntimeException("Missing required field " + field);
    }
  }

  public static String getString(JsonElement node) {
    String str = asString(node);
    if (str == null) {
      throw new IllegalStateException("Missing required string node " + node);
    }
    return str;
  }

  public static TypeString getTypeString(
      JsonDeserializationContext context, JsonObject node, String field) {
    return get(context, node, field, TypeString.class);
  }

  public static <X> X get(
      JsonDeserializationContext context, JsonObject node, String field, Class<X> clazz) {
    return context.deserialize(node.get(field), clazz);
  }

  public static List<MagikDefinition> getDefinitions(Location location) {
    if (location == null) {
      return Collections.emptyList();
    }

    Path path = location.getPath();
    try {
      return parsedFiles.compute(path).definitions;
    } catch (InterruptedException e) {
      return Collections.emptyList();
    }
  }

  public static <X> MagikDefinition getParsedDefinition(
      Location location, String name, Class<X> clazz) {
    return getDefinitions(location).stream()
        .filter(def -> def.getName().endsWith(name) && clazz.isInstance(def))
        .findFirst()
        .orElse(null);
  }

  public static void clearParsedFiles() {
    parsedFiles.clear();
  }

  public static Instant getTimestamp(@Nullable Location location) {
    if (location == null) {
      return null;
    }

    Path path = location.getPath();
    if (Files.exists(path)) {
      try {
        return Files.getLastModifiedTime(path).toInstant();
      } catch (IOException e) {
        return null;
      }
    }

    return null;
  }

  private static IndexedFile computeParsedFile(Path path) {
    if (Files.notExists(path)) {
      return EMPTY_INDEXED_FILE;
    }

    long now = System.currentTimeMillis();

    try {
      MagikFile file = new MagikFile(BaseDeserializer.properties, path);
      return new IndexedFile(file.getMagikDefinitions(), now);
    } catch (Exception e) {
      return EMPTY_INDEXED_FILE;
    }
  }
}
