package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code define_slot_access()} parser. */
public class DefineSlotAccessParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefineSlotAccessParser.class);

  private static final String DEFINE_SLOT_ACCESS = "define_slot_access()";
  private static final String DEFINE_SLOT_EXTERNALLY_READABLE = "define_slot_externally_readable()";
  private static final String DEFINE_SLOT_EXTERNALLY_WRITABLE = "define_slot_externally_writable()";
  private static final String FLAG_READ = ":read";
  private static final String FLAG_READABLE = ":readable";
  private static final String FLAG_WRITE = ":write";
  private static final String FLAG_WRITABLE = ":writable";
  private static final String FLAVOR_PUBLIC = ":public";
  private static final String FLAVOR_READ_ONLY = ":read_only";

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code define_slot_access()} node.
   */
  public DefineSlotAccessParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Test if node is a {@code define_slot_access()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_slot_access()}, false otherwise.
   */
  public static boolean isDefineSlotAccess(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      return false;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    if (!helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_ACCESS)) {
      return false;
    }

    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      return false;
    }
    final String exemplarName = atomNode.getTokenValue();
    if (exemplarName == null) {
      return false;
    }

    // Arguments: name, flag, optional flavour, owner_name
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
    return argument0Node != null && argument1Node != null;
  }

  /**
   * Test if node is a {@code define_slot_externally_readable()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_slot_externally_readable()}, false otherwise.
   */
  public static boolean isDefineSlotExternallyReadable(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      return false;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    if (!helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_READABLE)) {
      return false;
    }

    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      return false;
    }
    final String exemplarName = atomNode.getTokenValue();
    if (exemplarName == null) {
      return false;
    }

    // Arguments: name, optional private?, owner_name
    // `private?` is actually `flavour`.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    return argument0Node != null;
  }

  /**
   * Test if node is a {@code define_slot_externally_writable()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_slot_externally_writable()}, false otherwise.
   */
  public static boolean isDefineSlotExternallyWritable(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      return false;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    if (!helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_WRITABLE)) {
      return false;
    }

    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      return false;
    }
    final String exemplarName = atomNode.getTokenValue();
    if (exemplarName == null) {
      return false;
    }

    // Arguments: name, optional flavour, owner_name
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    return argument0Node != null;
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);

    // Some sanity.
    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      LOGGER.warn(
          "Unable to read slot access: {}, at line: {}", // NOSONAR
          helper.getMethodName(),
          this.node.getTokenLine());
      return Collections.emptyList();
    }
    final String identifier = atomNode.getTokenValue();
    if (identifier == null) {
      LOGGER.warn(
          "Unable to read slot access: {}, at line: {}", // NOSONAR
          helper.getMethodName(),
          this.node.getTokenLine());
      return Collections.emptyList();
    }

    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
    final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      return Collections.emptyList();
    }
    if (helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_ACCESS)
        && argument1Node == null) {
      LOGGER.warn(
          "Unable to read slot access: {}, at line: {}", // NOSONAR
          helper.getMethodName(),
          this.node.getTokenLine());
      return Collections.emptyList();
    }

    // Figure timestamp.
    final Instant timestamp = this.magikFile.getTimestamp();

    // Figure module name.
    final URI uri = this.node.getToken().getURI();
    final String moduleName = ModuleDefFile.getModuleNameForUri(uri);

    // Figure statement node.
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure pakkage.
    final String pakkage = this.getCurrentPakkage();

    // Figure doc.
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Build methods.
    final String slotNameSymbol = argument0Node.getTokenValue();
    final String slotName = slotNameSymbol.substring(1);
    final String flavor =
        argument2Node != null ? argument2Node.getTokenValue() : FLAVOR_PUBLIC; // Default is public.
    final String flag;
    if (helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_READABLE)) {
      flag = DefineSlotAccessParser.FLAG_READABLE;
    } else if (helper.isMethodInvocationOf(
        DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_WRITABLE)) {
      flag = DefineSlotAccessParser.FLAG_WRITABLE;
    } else if (helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_ACCESS)) {
      flag = argument1Node.getTokenValue(); // NOSONAR: argument1Node cannot be null in this case.
    } else {
      throw new IllegalStateException();
    }
    final TypeString exemplarName = TypeString.ofIdentifier(identifier, pakkage);
    final List<MethodDefinition> methodDefinitions =
        this.generateSlotMethods(
            timestamp, moduleName, statementNode, exemplarName, slotName, flag, flavor, doc);
    return List.copyOf(methodDefinitions);
  }

  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  private List<MethodDefinition> generateSlotMethods(
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final AstNode definitionNode,
      final TypeString exemplarName,
      final String slotName,
      final String flag,
      final String flavor,
      final String doc) {
    final List<MethodDefinition> methodDefinitions = new ArrayList<>();

    // Figure location.
    final URI uri = definitionNode.getToken().getURI();
    final Location location = new Location(uri, definitionNode);

    if (flag.equals(FLAG_READ) || flag.equals(FLAG_READABLE)) {
      // get
      final String getName = slotName;
      final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC)) {
        getModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> getParameters = Collections.emptyList();
      final MethodDefinition getMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              doc,
              definitionNode,
              exemplarName,
              getName,
              getModifiers,
              getParameters,
              null,
              Collections.emptySet(),
              ExpressionResultString.UNDEFINED,
              ExpressionResultString.UNDEFINED);
      methodDefinitions.add(getMethod);
    } else if (flag.equals(FLAG_WRITE) || flag.equals(FLAG_WRITABLE)) {
      // get
      final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC) && !flavor.equals(FLAVOR_READ_ONLY)) {
        getModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> getParameters = Collections.emptyList();
      final MethodDefinition getMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              doc,
              definitionNode,
              exemplarName,
              slotName,
              getModifiers,
              getParameters,
              null,
              Collections.emptySet(),
              ExpressionResultString.UNDEFINED,
              ExpressionResultString.UNDEFINED);
      methodDefinitions.add(getMethod);

      // set
      final String setName = slotName + MagikOperator.CHEVRON.getValue();
      final Set<MethodDefinition.Modifier> setModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC)) {
        setModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> setParameters = Collections.emptyList();
      final ParameterDefinition assignmentParam =
          new ParameterDefinition(
              location,
              timestamp,
              moduleName,
              null,
              definitionNode,
              "val",
              ParameterDefinition.Modifier.NONE,
              TypeString.UNDEFINED);
      final MethodDefinition setMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              doc,
              definitionNode,
              exemplarName,
              setName,
              setModifiers,
              setParameters,
              assignmentParam,
              Collections.emptySet(),
              ExpressionResultString.UNDEFINED,
              ExpressionResultString.UNDEFINED);
      methodDefinitions.add(setMethod);

      // boot
      final String bootName = slotName + MagikOperator.BOOT_CHEVRON.getValue();
      final MethodDefinition bootMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              doc,
              definitionNode,
              exemplarName,
              bootName,
              setModifiers,
              setParameters,
              assignmentParam,
              Collections.emptySet(),
              ExpressionResultString.UNDEFINED,
              ExpressionResultString.UNDEFINED);
      methodDefinitions.add(bootMethod);
    }
    return methodDefinitions;
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
