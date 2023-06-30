package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type Hierarchy Provider.
 */
public class TypeHierarchyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeHierarchyProvider.class);
    private static final Location NO_LOCATION = new Location(URI.create("file:///"));

    private final ITypeKeeper typeKeeper;

    /**
     * Constructor.
     * @param typeKeeper Type keeper.
     */
    public TypeHierarchyProvider(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Set capabilities.
     * @param capabilities
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setTypeHierarchyProvider(true);
    }

    /**
     * Prepare type hierarchy items.
     * @param magikFile File to work on.
     * @param position Position in file.
     * @return List of type hierarchy items.
     */
    public List<TypeHierarchyItem> prepareTypeHierarchy(final MagikTypedFile magikFile, final Position position) {
        LOGGER.info("prepareTypeHierarchy: {}", magikFile);
        // Parse magik.
        final AstNode topNode = magikFile.getTopNode();

        // Should always be on an identifier.
        final AstNode tokenNode = AstQuery.nodeAt(
            topNode,
            Lsp4jConversion.positionFromLsp4j(position),
            MagikGrammar.IDENTIFIER);
        if (tokenNode == null) {
            return null;  // NOSONAR: LSP4J requires null.
        }

        // Ensure it is on a class identifier, or a variable.
        final ITypeKeeper fileTypeKeeper = magikFile.getTypeKeeper();
        final AstNode methodDefinitionNode = AstQuery.getParentFromChain(
            tokenNode,
            MagikGrammar.IDENTIFIER,
            MagikGrammar.EXEMPLAR_NAME,
            MagikGrammar.METHOD_DEFINITION);
        final AstNode atomNode = AstQuery.getParentFromChain(
            tokenNode,
            MagikGrammar.IDENTIFIER,
            MagikGrammar.ATOM);
        if (methodDefinitionNode != null) {
            final MethodDefinitionNodeHelper methodDefinitionNodeHelper =
                new MethodDefinitionNodeHelper(methodDefinitionNode);
            final TypeString typeStr = methodDefinitionNodeHelper.getTypeString();
            final AbstractType type = fileTypeKeeper.getType(typeStr);
            final TypeHierarchyItem item = this.toTypeHierarchyItem(type);
            return List.of(item);
        } else if (atomNode != null) {
            // Get type from node.
            final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
            final ExpressionResult expressionResult = reasoner.getNodeType(atomNode);
            final AbstractType type = expressionResult.get(0, null);
            if (type != null
                && type != UndefinedType.INSTANCE) {
                final TypeString typeStr = type.getTypeString();
                final TypeHierarchyItem item = this.toTypeHierarchyItem(fileTypeKeeper.getType(typeStr));
                return List.of(item);
            }
        }

        return null;  // NOSONAR: LSP4J requires null.
    }

    /**
     * Get sub types.
     * @param item Item to get sub types for.
     * @return List of sub types.
     */
    public List<TypeHierarchyItem> typeHierarchySubtypes(final TypeHierarchyItem item) {
        final String itemName = item.getName();
        final TypeString typeString = TypeString.ofIdentifier(itemName, "sw");
        final AbstractType type = this.typeKeeper.getType(typeString);
        if (type == UndefinedType.INSTANCE) {
            return null;  // NOSONAR: LSP4J requires null.
        }

        // Find children.
        final Comparator<TypeHierarchyItem> byName = Comparator.comparing(TypeHierarchyItem::getName);
        return this.typeKeeper.getTypes().stream()
            .filter(t -> t.getParents().contains(type))
            .map(this::toTypeHierarchyItem)
            .sorted(byName)
            .toList();
    }

    /**
     * Get super types.
     * @param item Item to get super types for.
     * @return List of super types.
     */
    public List<TypeHierarchyItem> typeHierarchySupertypes(final TypeHierarchyItem item) {
        final String itemName = item.getName();
        final TypeString typeString = TypeString.ofIdentifier(itemName, "sw");
        final AbstractType type = this.typeKeeper.getType(typeString);
        if (type == UndefinedType.INSTANCE) {
            return null;  // NOSONAR: LSP4J requires null.
        }

        final Comparator<TypeHierarchyItem> byName = Comparator.comparing(TypeHierarchyItem::getName);
        return type.getParents().stream()
            .map(this::toTypeHierarchyItem)
            .sorted(byName)
            .toList();
    }

    private TypeHierarchyItem toTypeHierarchyItem(final AbstractType type) {
        final TypeString typeStr = type.getTypeString();
        final Location typeLocation = type.getLocation();
        final Location location = typeLocation != null
            ? typeLocation
            : NO_LOCATION;
        return new TypeHierarchyItem(
            typeStr.getFullString(),
            SymbolKind.Class,
            location.getUri().toString(),
            Lsp4jConversion.rangeToLsp4j(location.getRange()),
            Lsp4jConversion.rangeToLsp4j(location.getRange()));
    }

}
