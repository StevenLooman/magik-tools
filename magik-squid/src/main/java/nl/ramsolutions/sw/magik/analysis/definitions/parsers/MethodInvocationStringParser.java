package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/** Utility class for parsing method invocation strings into MethodUsage objects. */
public final class MethodInvocationStringParser {

  private MethodInvocationStringParser() {
    // Utility class.
  }

  /**
   * Parse a method invocation string like "rope.new()" or "sw:rope.new()" or "rope[]" into a
   * MethodUsage.
   *
   * @param invocationString The method invocation string.
   * @param location Location for the method usage.
   * @param currentPackage Current package for parsing types without explicit package qualifiers.
   * @return MethodUsage or null if parsing fails.
   */
  @CheckForNull
  public static MethodUsage parseInvocationString(
      final String invocationString, final Location location, final String currentPackage) {
    // Expected formats:
    // - "type.method()" or "package:type.method()" or "type.method".
    // - "type[]" or "package:type[]" (indexing method).
    // - "type[,]" or "package:type[,]" (multi-dimensional indexing).
    final String cleanedCall = invocationString.trim();

    // Check for square bracket notation (indexing methods).
    final int bracketIndex = cleanedCall.indexOf('[');
    if (bracketIndex != -1) {
      // Square bracket notation: "type[...]".
      final String typeString = cleanedCall.substring(0, bracketIndex);
      final String methodName = cleanedCall.substring(bracketIndex);

      if (typeString.isEmpty() || methodName.isEmpty()) {
        return null;
      }

      // Parse the type string using TypeStringParser with the current package.
      final TypeString typeName = TypeStringParser.parseTypeString(typeString, currentPackage);

      return new MethodUsage(typeName, methodName, location, null);
    }

    // Standard dot notation: "type.method".
    final int lastDotIndex = cleanedCall.lastIndexOf('.');
    if (lastDotIndex == -1) {
      // No dot or bracket found, invalid format.
      return null;
    }

    final String typeString = cleanedCall.substring(0, lastDotIndex);
    final String methodName = cleanedCall.substring(lastDotIndex + 1);

    if (typeString.isEmpty() || methodName.isEmpty()) {
      return null;
    }

    // Parse the type string using TypeStringParser with the current package.
    final TypeString typeName = TypeStringParser.parseTypeString(typeString, currentPackage);

    return new MethodUsage(typeName, methodName, location, null);
  }
}
