package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads class info from generated libraries. E.g.,
 * core/sw_core/libs/sw_core.emailer.1.jar/class_info
 */
public final class ClassInfoDefinitionReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassInfoDefinitionReader.class);

  private static final String DOC_PREFIX = "## ";
  private static final URI TYPE_DOC_URI = URI.create("magik://type-doc");

  private static final Map<String, MethodDefinition.Modifier> METHOD_MODIFIER_MAPPING =
      Map.of(
          "private", MethodDefinition.Modifier.PRIVATE,
          "iter", MethodDefinition.Modifier.ITER);
  private static final Map<String, ProcedureDefinition.Modifier> GLOBAL_MODIFIER_MAPPING =
      Map.of("iter", ProcedureDefinition.Modifier.ITER);
  private static final Map<String, ParameterDefinition.Modifier> PARAMETER_MODIFIER_MAPPING =
      Map.of(
          "", ParameterDefinition.Modifier.NONE,
          "_optional", ParameterDefinition.Modifier.OPTIONAL,
          "_gather", ParameterDefinition.Modifier.GATHER);
  private static final List<String> METHOD_PRAGMA_SKIP_LIST = List.of("classconst", "classvar");
  private static final String NEXT_NOT_SLASH_PATTERN = "[^/]+";
  private static final String FILE_URI_PREFIX = "file://";

  private final Path path;
  private final IDefinitionKeeper definitionKeeper;

  /**
   * Constructor.
   *
   * @param path Path to jar file.
   * @param typeKeeper Typekeeper to fill.
   */
  private ClassInfoDefinitionReader(final Path path, final IDefinitionKeeper definitionKeeper) {
    this.path = path;
    this.definitionKeeper = definitionKeeper;
  }

  private Instant getTimestamp() throws IOException {
    return Files.getLastModifiedTime(this.path).toInstant();
  }

  static Location locationFromSourceFile(final String sourceFile) {
    if (sourceFile.isBlank()) {
      return null;
    }

    return new Location(URI.create(FILE_URI_PREFIX + "/" + sourceFile));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void run() throws IOException {
    final String[] parts = this.path.getFileName().toString().split("\\.");
    if (parts.length != 4) { // <product_name>.<module_name>.<version>.jar
      // Must be some other jar.
      return;
    }

    final String moduleName = parts[1];
    final File file = this.path.toFile();
    try (ZipFile zipFile = new ZipFile(file)) {
      final ZipEntry zipEntry = zipFile.getEntry("class_info");
      if (zipEntry == null) {
        return;
      }

      try (InputStream stream = zipFile.getInputStream(zipEntry);
          InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.ISO_8859_1)) {
        this.parseClassInfo(moduleName, reader);
      }
    }
  }

  private void parseClassInfo(final String moduleName, final InputStreamReader streamReader)
      throws IOException {
    try (LineNumberReader reader = new LineNumberReader(streamReader)) {
      String line = reader.readLine();
      while (line != null) {
        final String[] tokens = line.split(" ");
        final String token0 = tokens.length > 0 ? tokens[0] : "";
        final String token1 = tokens.length > 1 ? tokens[1] : "";
        switch (token0) {
          case "method":
            switch (token1) {
              case "<global>":
                this.readGlobal(moduleName, line, reader);
                break;

              case "<condition>":
                this.readCondition(moduleName, line, reader);
                break;

              default:
                this.readMethod(moduleName, line, reader);
                break;
            }
            break;

          case "slotted_class":
            this.readSlottedClass(moduleName, line, reader);
            break;

          case "indexed_class":
            this.readIndexedClass(moduleName, line, reader);
            break;

          case "enumerated_class":
            this.readEnumeratedClass(moduleName, line, reader);
            break;

          case "delete_class":
            this.readDeleteClass(moduleName, line, reader);
            break;

          case "mixin":
            this.readMixin(moduleName, line, reader);
            break;

          default:
            LOGGER.warn("Unknown token: {}", token0);
            throw new UnsupportedOperationException("Unknown token: " + token0);
        }

        if (!token0.equals("delete_class")) {
          reader.readLine(); // NOSONAR: Skip last part of line.
        }

        line = reader.readLine();
      }
    }
  }

  private void readGlobal(final String moduleName, final String line, final BufferedReader reader)
      throws IOException {
    // 1 : "method" "<global>" <global_name> <parameters>
    // 2 : n ["iter"]* ["basic"/"restricted"/"internal"/pragma]* source_file
    // 3+: <n lines of comments>
    // Line 1
    final String globalName;
    final TypeString typeString;
    final List<ParameterDefinition> parameters;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "method"

      scanner.next(); // "<global>"

      globalName = scanner.next();
      typeString = TypeString.ofIdentifier(globalName, "sw");

      parameters = this.readParameterDefinitions(moduleName, scanner);
    }

    // Line 2
    final String line2 = reader.readLine();
    final Set<ProcedureDefinition.Modifier> modifiers = new HashSet<>();
    final List<String> pragmas = new ArrayList<>();
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line2)) {
      commentLineCount = scanner.nextInt();

      // Read modifiers/pragmas.
      while (scanner.hasNext(ClassInfoDefinitionReader.NEXT_NOT_SLASH_PATTERN)) {
        final String item = scanner.next();
        final ProcedureDefinition.Modifier modifier =
            ClassInfoDefinitionReader.GLOBAL_MODIFIER_MAPPING.get(item);
        if (modifier != null) {
          modifiers.add(modifier);
        }

        pragmas.add(item);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 3+
    final StringBuilder docBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      docBuilder.append(commentLine);
      docBuilder.append('\n');
    }
    final String doc = docBuilder.toString();

    final Instant timestamp = this.getTimestamp();

    // A record carries parameters or an iter marker only when the global is bound to a procedure
    // (a global cannot iterate), so index it as one; the procedure claims the global's name, so no
    // GlobalDefinition may sit beside it.
    if (!parameters.isEmpty() || !modifiers.isEmpty()) {
      final ProcedureDefinition procedureDefinition =
          new ProcedureDefinition(
              location,
              timestamp,
              moduleName,
              doc,
              null,
              modifiers,
              typeString,
              globalName,
              parameters,
              pragma,
              ExpressionResultString.UNDEFINED,
              ExpressionResultString.UNDEFINED);
      final ProcedureDefinition typedProcedure =
          ClassInfoDefinitionReader.applyTypeDoc(procedureDefinition);
      this.definitionKeeper.add(typedProcedure);
      return;
    }

    final TypeDocParser docParser = ClassInfoDefinitionReader.createTypeDocParser(doc);
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    final TypeString aliasedTypeName =
        returnTypes.stream().findFirst().orElse(TypeString.UNDEFINED);

    final GlobalDefinition definition =
        new GlobalDefinition(
            location, timestamp, moduleName, doc, null, typeString, aliasedTypeName, pragma);
    this.definitionKeeper.add(definition);
  }

  private List<ParameterDefinition> readParameterDefinitions(
      final String moduleName, final Scanner scanner) {
    final List<ParameterDefinition> paramDefs = new ArrayList<>();
    ParameterDefinition.Modifier paramModifier = ParameterDefinition.Modifier.NONE;
    while (scanner.hasNext()) {
      final String next = scanner.next();
      if (next.startsWith("_")) {
        paramModifier = ClassInfoDefinitionReader.PARAMETER_MODIFIER_MAPPING.get(next);
        continue;
      }

      final ParameterDefinition paramDef =
          new ParameterDefinition(
              null, null, moduleName, null, null, next, paramModifier, TypeString.UNDEFINED);
      paramDefs.add(paramDef);
    }
    return paramDefs;
  }

  private void readCondition(
      final String moduleName, final String line, final BufferedReader reader) throws IOException {
    // 1 : "method" "<condition>" <condition_name> <data_name_list>
    // 2 : n ["basic"/"restricted"/"internal"/pragma]* source_file
    // 3+: <n lines of comments>
    // Line 1
    final String name;
    final List<String> dataNames;
    final String parent = null; // Parent is not registered in class_info files.
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "method"

      scanner.next(); // "<condition>"

      name = scanner.next();

      // Data names.
      final Spliterator<String> scannerSpliterator =
          Spliterators.spliteratorUnknownSize(scanner, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      dataNames = StreamSupport.stream(scannerSpliterator, false).toList();
    }

    // Line 2
    final String line2 = reader.readLine();
    final List<String> pragmas = new ArrayList<>();
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line2)) {
      commentLineCount = scanner.nextInt();

      // Read modifiers/pragmas.
      while (scanner.hasNext(ClassInfoDefinitionReader.NEXT_NOT_SLASH_PATTERN)) {
        final String item = scanner.next();
        pragmas.add(item);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 3+
    final StringBuilder docBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      docBuilder.append(commentLine);
      docBuilder.append('\n');
    }
    final String doc = docBuilder.toString();

    final Instant timestamp = this.getTimestamp();
    final ConditionDefinition definition =
        new ConditionDefinition(
            location, timestamp, moduleName, doc, null, name, parent, dataNames, pragma);
    this.definitionKeeper.add(definition);
  }

  private void readMethod(final String moduleName, final String line, final BufferedReader reader)
      throws IOException {
    // 1 : "method" <class name> <method name> <parameters>
    // 2 : n ["private"/"classconst"/"classvar"/"iter"]*
    // ["basic"/"restricted"/"internal"/pragma]* source_file
    // 3+: <n lines of comments>
    // Line 1
    final String methodName;
    final TypeString typeString;
    final List<ParameterDefinition> parameters;
    final ParameterDefinition assignmentParameter;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "method"

      final String className = scanner.next();
      typeString = TypeString.ofIdentifier(className, TypeString.DEFAULT_PACKAGE);

      methodName = scanner.next();

      // Parameters.
      final List<ParameterDefinition> paramDefs =
          this.readParameterDefinitions(moduleName, scanner);
      parameters =
          methodName.contains("<<") && !paramDefs.isEmpty()
              ? paramDefs.subList(1, paramDefs.size())
              : paramDefs;
      assignmentParameter =
          methodName.contains("<<") && !paramDefs.isEmpty() ? paramDefs.get(0) : null;
    }

    // Line 2
    final String line2 = reader.readLine();
    final Set<MethodDefinition.Modifier> modifiers = new HashSet<>();
    final List<String> pragmas = new ArrayList<>();
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line2)) {
      commentLineCount = scanner.nextInt();

      // Read modifiers/pragmas.
      while (scanner.hasNext(ClassInfoDefinitionReader.NEXT_NOT_SLASH_PATTERN)) {
        final String item = scanner.next();
        if (ClassInfoDefinitionReader.METHOD_MODIFIER_MAPPING.containsKey(item)) {
          final MethodDefinition.Modifier modifier =
              ClassInfoDefinitionReader.METHOD_MODIFIER_MAPPING.get(item);
          modifiers.add(modifier);
        } else if (ClassInfoDefinitionReader.METHOD_PRAGMA_SKIP_LIST.contains(item)) {
          continue;
        }

        pragmas.add(item);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 3+
    final StringBuilder docBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      docBuilder.append(commentLine);
      docBuilder.append('\n');
    }
    final String doc = docBuilder.toString();

    final MethodDefinition definition =
        new MethodDefinition(
            location,
            this.getTimestamp(),
            moduleName,
            doc,
            null,
            typeString,
            methodName,
            modifiers,
            parameters,
            assignmentParameter,
            pragma,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.UNDEFINED);
    final MethodDefinition typedDefinition = ClassInfoDefinitionReader.applyTypeDoc(definition);
    this.definitionKeeper.add(typedDefinition);
  }

  private static MethodDefinition applyTypeDoc(final MethodDefinition definition) {
    final String doc = definition.getDoc();
    final TypeDocParser docParser = ClassInfoDefinitionReader.createTypeDocParser(doc);
    final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
    final List<ParameterDefinition> parameters = definition.getParameters();
    final List<ParameterDefinition> typedParameters =
        parameters.stream()
            .map(parameter -> ClassInfoDefinitionReader.applyTypeDoc(parameter, parameterTypes))
            .toList();
    final ParameterDefinition assignmentParameter = definition.getAssignmentParameter();
    final ParameterDefinition typedAssignmentParameter =
        assignmentParameter != null
            ? ClassInfoDefinitionReader.applyTypeDoc(assignmentParameter, parameterTypes)
            : null;
    // An empty list means the doc declared nothing, which is not the same as an empty result.
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    final ExpressionResultString returnResult =
        returnTypes.isEmpty()
            ? ExpressionResultString.UNDEFINED
            : new ExpressionResultString(returnTypes);
    final List<TypeString> loopTypes = docParser.getLoopTypes();
    final ExpressionResultString loopResult =
        loopTypes.isEmpty()
            ? ExpressionResultString.UNDEFINED
            : new ExpressionResultString(loopTypes);

    return new MethodDefinition(
        definition.getLocation(),
        definition.getTimestamp(),
        definition.getModuleName(),
        doc,
        definition.getNode(),
        definition.getTypeName(),
        definition.getMethodName(),
        definition.getModifiers(),
        typedParameters,
        typedAssignmentParameter,
        definition.getPragma(),
        returnResult,
        loopResult);
  }

  private static ProcedureDefinition applyTypeDoc(final ProcedureDefinition definition) {
    final String doc = definition.getDoc();
    final TypeDocParser docParser = ClassInfoDefinitionReader.createTypeDocParser(doc);
    final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
    final List<ParameterDefinition> parameters = definition.getParameters();
    final List<ParameterDefinition> typedParameters =
        parameters.stream()
            .map(parameter -> ClassInfoDefinitionReader.applyTypeDoc(parameter, parameterTypes))
            .toList();
    // An empty list means the doc declared nothing, which is not the same as an empty result.
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    final ExpressionResultString returnResult =
        returnTypes.isEmpty()
            ? ExpressionResultString.UNDEFINED
            : new ExpressionResultString(returnTypes);
    final List<TypeString> loopTypes = docParser.getLoopTypes();
    final ExpressionResultString loopResult =
        loopTypes.isEmpty()
            ? ExpressionResultString.UNDEFINED
            : new ExpressionResultString(loopTypes);

    return new ProcedureDefinition(
        definition.getLocation(),
        definition.getTimestamp(),
        definition.getModuleName(),
        doc,
        definition.getNode(),
        definition.getModifiers(),
        definition.getTypeString(),
        definition.getProcedureName(),
        typedParameters,
        definition.getPragma(),
        returnResult,
        loopResult);
  }

  private static ParameterDefinition applyTypeDoc(
      final ParameterDefinition definition, final Map<String, TypeString> parameterTypes) {
    final String name = definition.getName();
    final TypeString typeString = parameterTypes.get(name);
    if (typeString == null) {
      return definition;
    }

    return new ParameterDefinition(
        definition.getLocation(),
        definition.getTimestamp(),
        definition.getModuleName(),
        definition.getDoc(),
        definition.getNode(),
        name,
        definition.getModifier(),
        typeString);
  }

  private static SlotDefinition applyTypeDoc(
      final SlotDefinition definition, final Map<String, TypeString> slotTypes) {
    final String name = definition.getName();
    final TypeString typeString = slotTypes.get(name);
    if (typeString == null) {
      return definition;
    }

    return new SlotDefinition(
        definition.getLocation(),
        definition.getTimestamp(),
        definition.getModuleName(),
        definition.getDoc(),
        definition.getNode(),
        definition.getOwnerTypeName(),
        name,
        typeString);
  }

  /** Create a {@link TypeDocParser} over a doc whose {@code ##} prefixes were already stripped. */
  private static TypeDocParser createTypeDocParser(final String doc) {
    if (doc == null) {
      return new TypeDocParser(List.of(), TypeString.DEFAULT_PACKAGE);
    }

    // One token per line: TypeDocParser remaps token positions by line index.
    final String[] lines = doc.split("\n", -1);
    final List<Token> docTokens = new ArrayList<>(lines.length);
    for (int index = 0; index < lines.length; ++index) {
      final Token token =
          Token.builder()
              .setType(GenericTokenType.COMMENT)
              .setValueAndOriginalValue(ClassInfoDefinitionReader.DOC_PREFIX + lines[index])
              .setLine(index + 1)
              .setColumn(0)
              .setURI(ClassInfoDefinitionReader.TYPE_DOC_URI)
              .build();
      docTokens.add(token);
    }
    return new TypeDocParser(docTokens, TypeString.DEFAULT_PACKAGE);
  }

  private void readSlottedClass(
      final String moduleName, final String line, final BufferedReader reader) throws IOException {
    // 1 : "slotted_class" <class name> <slots> <-- This also includes inherited
    // slots!
    // 2 : <base classes>
    // 3 : n pragma source_file
    // 4+: <n lines of comments>
    // Line 1
    final TypeString typeString;
    final List<SlotDefinition> slots;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "slotted_class"

      final String identifier = scanner.next();
      typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);

      // Slots.
      final Spliterator<String> slotsSpliterator =
          Spliterators.spliteratorUnknownSize(scanner, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      slots =
          StreamSupport.stream(slotsSpliterator, false)
              .map(
                  slotName ->
                      new SlotDefinition(
                          null,
                          null,
                          moduleName,
                          null,
                          null,
                          typeString,
                          slotName,
                          TypeString.UNDEFINED))
              .toList();
    }

    // Line 2
    final String line2 = reader.readLine();
    final List<TypeString> parents =
        Arrays.stream(line2.split(" ")).map(TypeStringParser::parseTypeString).toList();

    // Line 3
    final String line3 = reader.readLine();
    final List<String> pragmas = new ArrayList<>();
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line3)) {
      commentLineCount = scanner.nextInt();

      // Pragmas.
      while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
        final String pragma = scanner.next();
        pragmas.add(pragma);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 4+
    final StringBuilder commentBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      commentBuilder.append(commentLine);
      commentBuilder.append('\n');
    }
    final String doc = commentBuilder.toString();

    // TODO: What if type is already defined?
    final Instant timestamp = this.getTimestamp();
    final ExemplarDefinition definition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            null,
            ExemplarDefinition.Sort.UNDEFINED,
            typeString,
            pragma);
    this.definitionKeeper.add(definition);
    parents.forEach(
        parentTypeString ->
            this.definitionKeeper.add(
                new InheritanceDefinition(
                    location, timestamp, moduleName, null, null, typeString, parentTypeString)));
    final TypeDocParser docParser = ClassInfoDefinitionReader.createTypeDocParser(doc);
    final Map<String, TypeString> slotTypes = docParser.getSlotTypes();
    slots.forEach(
        slot -> this.definitionKeeper.add(ClassInfoDefinitionReader.applyTypeDoc(slot, slotTypes)));
  }

  private void readIndexedClass(
      final String moduleName, final String line, final BufferedReader reader) throws IOException {
    // 1 : "indexed_class" <class name>
    // 2 : <base classes>
    // 3 : n pragma source_file
    // 4+: <n lines of comments>
    // Line 1
    final TypeString typeString;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "indexed_class"

      final String identifier = scanner.next();
      typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);
    }

    // Line 2
    final String line2 = reader.readLine();
    final List<TypeString> parents =
        Arrays.stream(line2.split(" ")).map(TypeStringParser::parseTypeString).toList();

    // Line 3
    final String line3 = reader.readLine();
    final List<String> pragmas = new ArrayList<>();
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line3)) {
      commentLineCount = scanner.nextInt();

      // Pragmas.
      while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
        final String pragma = scanner.next();
        pragmas.add(pragma);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 4+
    final StringBuilder commentBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      commentBuilder.append(commentLine);
      commentBuilder.append('\n');
    }
    final String doc = commentBuilder.toString();

    // TODO: What if type is already defined?
    final Instant timestamp = this.getTimestamp();
    final ExemplarDefinition definition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            null,
            ExemplarDefinition.Sort.UNDEFINED,
            typeString,
            pragma);
    this.definitionKeeper.add(definition);
    parents.forEach(
        parentTypeString ->
            this.definitionKeeper.add(
                new InheritanceDefinition(
                    location, timestamp, moduleName, null, null, typeString, parentTypeString)));
  }

  @SuppressWarnings("java:S4144")
  private void readEnumeratedClass(
      final String moduleName, final String line, final BufferedReader reader) throws IOException {
    // 1 : "enumerated_class" <class name>
    // 2 : <base classes>
    // 3 : n pragma source_file
    // 4+: <n lines of comments>
    // Line 1
    final TypeString typeString;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "enumerated_class"

      final String identifier = scanner.next();
      typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);
    }

    // Line 2
    final String line2 = reader.readLine();
    final List<TypeString> parents =
        Arrays.stream(line2.split(" ")).map(TypeStringParser::parseTypeString).toList();

    // Line 3
    final String line3 = reader.readLine();
    final List<String> pragmas = new ArrayList<>();
    ;
    final int commentLineCount;
    final Location location;
    try (Scanner scanner = new Scanner(line3)) {
      commentLineCount = scanner.nextInt();

      // Pragmas.
      while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
        final String pragma = scanner.next();
        pragmas.add(pragma);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 4+
    final StringBuilder commentBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      commentBuilder.append(commentLine);
      commentBuilder.append('\n');
    }
    final String doc = commentBuilder.toString();

    // TODO: What if type is already defined?
    final Instant timestamp = this.getTimestamp();
    final ExemplarDefinition definition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            null,
            ExemplarDefinition.Sort.UNDEFINED,
            typeString,
            pragma);
    this.definitionKeeper.add(definition);
    parents.forEach(
        parentTypeString ->
            this.definitionKeeper.add(
                new InheritanceDefinition(
                    location, timestamp, moduleName, null, null, typeString, parentTypeString)));
  }

  @SuppressWarnings("java:S1172")
  private void readDeleteClass(
      final String moduleName, final String line, final BufferedReader reader) {
    // 1 : "delete_class" <class name>
    // Line 1.
    // Ignore this.
  }

  private void readMixin(final String moduleName, final String line, final BufferedReader reader)
      throws IOException {
    // 1 : "mixin" <class name>
    // 2 : "."
    // 3 : n pragma source_file
    // 4+: <n lines of comments>
    // Line 1
    final TypeString typeString;
    try (Scanner scanner = new Scanner(line)) {
      scanner.next(); // "mixin"

      final String identifier = scanner.next();
      typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);
    }

    // Line 2
    reader.readLine(); // NOSONAR: Unused.

    // Line 3
    final String line3 = reader.readLine();
    final int commentLineCount;
    final Location location;
    final List<String> pragmas = new ArrayList<>();
    try (Scanner scanner = new Scanner(line3)) {
      commentLineCount = scanner.nextInt();

      // Read pragmas.
      while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
        final String pragma = scanner.next();
        pragmas.add(pragma);
      }

      // Source file.
      location = ClassInfoDefinitionReader.locationFromSourceFile(scanner.nextLine().trim());
    }
    final Pragma pragma = new Pragma(null, pragmas);

    // Line 4+
    final StringBuilder docBuilder = new StringBuilder();
    for (int i = 0; i < commentLineCount; ++i) {
      final String commentLine = reader.readLine();
      docBuilder.append(commentLine);
      docBuilder.append('\n');
    }
    final String doc = docBuilder.toString();

    final ExemplarDefinition definition =
        new ExemplarDefinition(
            location,
            this.getTimestamp(),
            moduleName,
            doc,
            null,
            ExemplarDefinition.Sort.MIXIN,
            typeString,
            pragma);
    this.definitionKeeper.add(definition);
  }

  /**
   * Read types from a jar/class_info file.
   *
   * @param path Path to jar file.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   * @throws IOException -
   */
  public static void readTypes(final Path path, final IDefinitionKeeper definitionKeeper)
      throws IOException {
    final ClassInfoDefinitionReader reader = new ClassInfoDefinitionReader(path, definitionKeeper);
    reader.run();
  }

  /**
   * Read libs directory.
   *
   * @param productPath Path to libs directory.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   * @throws IOException -
   */
  public static void readProductDirectory(
      final Path productPath, final IDefinitionKeeper definitionKeeper) throws IOException {
    final Path libsPath = productPath.resolve("libs");
    try (Stream<Path> paths = Files.list(libsPath)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
          .forEach(
              libPath -> {
                LOGGER.trace("Reading lib: {}", libPath);
                try {
                  ClassInfoDefinitionReader.readTypes(libPath, definitionKeeper);
                } catch (final IOException exception) {
                  LOGGER.error("Error reading file: " + libPath, exception);
                }
              });
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }
  }
}
