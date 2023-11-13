package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Definition of a global.
 */
public class GlobalDefinition extends Definition {

    private final TypeString typeName;
    private final TypeString aliasedTypeName;

    /**
     * Constructor.
     * @param moduleName Module name.
     * @param node Node.
     * @param typeName Type name.
     * @param aliasedTypeName Aliased type name.
     */
    public GlobalDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final TypeString typeName,
            final @Nullable TypeString aliasedTypeName,
            final @Nullable String doc) {
        super(location, moduleName, node, doc);
        this.typeName = typeName;
        this.aliasedTypeName = aliasedTypeName;
    }

    public TypeString getTypeString() {
        return this.typeName;
    }

    @CheckForNull
    public TypeString getAliasedTypeName() {
        return this.aliasedTypeName;
    }

    @Override
    public String getName() {
        return this.typeName.getFullString();
    }

    @Override
    public String getPackage() {
        return this.typeName.getPakkage();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getTypeString().getFullString(), this.getAliasedTypeName().getFullString());
    }

}
