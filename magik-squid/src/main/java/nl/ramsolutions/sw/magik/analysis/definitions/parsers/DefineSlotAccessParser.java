package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.*;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.FlagFlavor;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code define_slot_access()} parser. */
public class DefineSlotAccessParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefineSlotAccessParser.class);

  private static final String DEFINE_SLOT_ACCESS = "define_slot_access()";
  private static final String DEFINE_SLOT_EXTERNALLY_READABLE = "define_slot_externally_readable()";
  private static final String DEFINE_SLOT_EXTERNALLY_WRITABLE = "define_slot_externally_writable()";

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
    final AstNode argument2Node =
        argumentsHelper.getArgument(2, MagikGrammar.SYMBOL, MagikGrammar.TRUE, MagikGrammar.FALSE);
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
        argument2Node != null
            ? argument2Node.getTokenValue()
            : FlagFlavor.FLAVOR_PUBLIC; // Default is public.

    final String flag;
    if (helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_READABLE)) {
      flag = FlagFlavor.FLAG_READABLE;
    } else if (helper.isMethodInvocationOf(
        DefineSlotAccessParser.DEFINE_SLOT_EXTERNALLY_WRITABLE)) {
      flag = FlagFlavor.FLAG_WRITABLE;
    } else if (helper.isMethodInvocationOf(DefineSlotAccessParser.DEFINE_SLOT_ACCESS)) {
      flag = argument1Node.getTokenValue(); // NOSONAR: argument1Node cannot be null in this case.
    } else {
      throw new IllegalStateException();
    }

    final TypeString exemplarName = TypeString.ofIdentifier(identifier, pakkage);
    final TypeDocParser typeDocParser = new TypeDocParser(parentNode);
    final TypeString slotType = typeDocParser.getReturnTypes().stream().findFirst().orElse(null);

    final List<MethodDefinition> methodDefinitions =
        this.generateSlotMethods(
            timestamp,
            moduleName,
            statementNode,
            exemplarName,
            slotName,
            flag,
            flavor,
            doc,
            slotType);
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
      final String doc,
      @Nullable TypeString slotType) {
    final List<MethodDefinition> methodDefinitions = new ArrayList<>();

    if (slotType == null) {
      slotType = TypeString.UNDEFINED;
    }

    // Figure location.
    final URI uri = definitionNode.getToken().getURI();
    final Location location = new Location(uri, definitionNode);

    final Set<MethodDefinition.Modifier> defaultModifiers =
        new HashSet<>(Set.of(MethodDefinition.Modifier.SLOT));
    if (FlagFlavor.isPrivate(flavor)) {
      defaultModifiers.add(MethodDefinition.Modifier.PRIVATE);
    }

    if (FlagFlavor.isReadable(flag) || FlagFlavor.isWritable(flag)) {
      // get
      final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>(defaultModifiers);
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
              Collections.emptyList(),
              null,
              Collections.emptySet(),
              new ExpressionResultString(slotType),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(getMethod);
    }

    if (FlagFlavor.isWritable(flag)) {
      // set
      final String setName = slotName + " " + MagikOperator.CHEVRON.getValue();
      final Set<MethodDefinition.Modifier> setModifiers = new HashSet<>(defaultModifiers);
      if (FlagFlavor.isReadOnly(flavor)) {
        setModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }

      final ParameterDefinition assignmentParam =
          new ParameterDefinition(
              null,
              timestamp,
              moduleName,
              null,
              definitionNode,
              "val",
              ParameterDefinition.Modifier.NONE,
              slotType);
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
              Collections.emptyList(),
              assignmentParam,
              Collections.emptySet(),
              new ExpressionResultString(slotType),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(setMethod);

      // boot
      final String bootName = slotName + " " + MagikOperator.BOOT_CHEVRON.getValue();
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
              Collections.emptyList(),
              assignmentParam,
              Collections.emptySet(),
              new ExpressionResultString(slotType),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(bootMethod);
    }
    return methodDefinitions;
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
