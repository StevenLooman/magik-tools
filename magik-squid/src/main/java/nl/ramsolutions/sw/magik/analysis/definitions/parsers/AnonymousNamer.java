package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** Creates names for anonymous constructs, such as procedures. */
public final class AnonymousNamer {

  private static final URI DEFAULT_URI = MagikFile.DEFAULT_URI;

  private AnonymousNamer() {}

  /**
   * Get a name for a PROCEDURE_DEFINITION.
   *
   * <p>The name is keyed on the enclosing definition plus the procedure's index within it, so it
   * does not depend on where the file is checked out, and editing one method renames no procedure
   * in another:
   *
   * <ul>
   *   <li>in a method: {@code _anon:_proc__<package>__<exemplar>__<method>__<n>};
   *   <li>in a procedure bound to a global: {@code _anon:_proc__<package>__<global>__<n>};
   *   <li>otherwise: {@code _anon:_proc__<module>__<file>__<n>}, or {@code
   *       _anon:_proc__<file>__<n>} outside any module.
   * </ul>
   *
   * <p>Each part is made identifier-safe, so the name parses back as a {@link TypeString}.
   *
   * @param node PROCEDURE_DEFINITION node.
   * @return Name.
   */
  public static TypeString getNameForProcedure(final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    final AstNode methodNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    if (methodNode != null) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
      final TypeString exemplarName = helper.getExemplarTypeString();
      final String methodName = helper.getMethodName();
      final List<AstNode> procedureNodes =
          methodNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION);
      final int index = procedureNodes.indexOf(node);
      return AnonymousNamer.createProcedureName(
          index, exemplarName.getPakkage(), exemplarName.getIdentifier(), methodName);
    }

    final AstNode globalProcedureNode = AnonymousNamer.getGlobalProcedureAncestor(node);
    if (globalProcedureNode != null) {
      final String globalName = ProcedureNamer.getTopLevelAssignedName(globalProcedureNode);
      final PackageNodeHelper helper = new PackageNodeHelper(globalProcedureNode);
      final String pakkage = helper.getCurrentPackage();
      final List<AstNode> procedureNodes =
          globalProcedureNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION);
      final int index = procedureNodes.indexOf(node);
      return AnonymousNamer.createProcedureName(index, pakkage, globalName);
    }

    final URI uri = node.getToken().getURI();
    final String fileName = AnonymousNamer.getFileName(uri);
    final int index = AnonymousNamer.getFileLevelProcedureNodes(node).indexOf(node);
    final ModuleDefFile moduleDefFile =
        DEFAULT_URI.equals(uri) ? null : ModuleDefFile.getModuleDefFileForUri(uri, null);
    if (moduleDefFile == null) {
      return AnonymousNamer.createProcedureName(index, fileName);
    }

    final String moduleName = moduleDefFile.getModuleDefinition().getName();
    return AnonymousNamer.createProcedureName(index, moduleName, fileName);
  }

  private static TypeString createProcedureName(final int index, final String... parts) {
    final StringBuilder builder = new StringBuilder("_proc");
    for (final String part : parts) {
      final String safePart = part.replaceAll("[^A-Za-z0-9_]", "_"); // Safe for TypeString.
      builder.append("__").append(safePart);
    }
    builder.append("__").append(index);
    final String name = builder.toString();
    return TypeString.ofIdentifier(name, TypeString.ANONYMOUS_PACKAGE);
  }

  /** A procedure is only bound to a global at top level, so only the outermost can be. */
  @CheckForNull
  private static AstNode getGlobalProcedureAncestor(final AstNode node) {
    AstNode outermostNode = null;
    AstNode ancestorNode = node.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION);
    while (ancestorNode != null) {
      outermostNode = ancestorNode;
      ancestorNode = ancestorNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION);
    }

    if (outermostNode == null || ProcedureNamer.getTopLevelAssignedName(outermostNode) == null) {
      return null;
    }

    return outermostNode;
  }

  private static List<AstNode> getFileLevelProcedureNodes(final AstNode node) {
    AstNode rootNode = node;
    while (rootNode.getParent() != null) {
      rootNode = rootNode.getParent();
    }

    return rootNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION).stream()
        .filter(
            procedureNode -> procedureNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION) == null)
        .filter(procedureNode -> AnonymousNamer.getGlobalProcedureAncestor(procedureNode) == null)
        .toList();
  }

  private static String getFileName(final URI uri) {
    if (DEFAULT_URI.equals(uri)) {
      return "in_memory";
    }

    final String path = uri.getPath();
    if (path == null) {
      return uri.toString();
    }

    final int separatorIndex = path.lastIndexOf('/');
    return path.substring(separatorIndex + 1);
  }
}
