package nl.ramsolutions.sw.checks.output;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import nl.ramsolutions.sw.MagikToolsProperties;

/** Registry for reporter factories. */
public final class ReporterRegistry {

  @FunctionalInterface
  public interface ReporterFactory {
    Reporter create(MagikToolsProperties properties, ReporterContext context);
  }

  private static final Map<String, ReporterFactory> FACTORIES = new ConcurrentHashMap<>();

  private ReporterRegistry() {}

  public static void register(final String format, final ReporterFactory factory) {
    final String normalizedFormat = normalize(format);
    FACTORIES.put(normalizedFormat, Objects.requireNonNull(factory, "factory"));
  }

  public static Reporter createReporter(
      final String format, final MagikToolsProperties properties, final ReporterContext context) {
    final String normalizedFormat = normalize(format);
    final ReporterFactory factory = FACTORIES.get(normalizedFormat);
    if (factory == null) {
      throw new IllegalArgumentException("Unknown output format: " + format);
    }

    final MagikToolsProperties validatedProperties =
        Objects.requireNonNull(properties, "properties");
    final ReporterContext validatedContext = Objects.requireNonNull(context, "context");
    return factory.create(validatedProperties, validatedContext);
  }

  private static String normalize(final String format) {
    return Objects.requireNonNull(format, "format").trim().toLowerCase(Locale.ROOT);
  }
}
