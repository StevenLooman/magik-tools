package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionReader;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.parser.MagikParser;

/**
 * Magik file.
 */
public class MagikFile {

    private final URI uri;
    private final String source;
    private AstNode astNode;
    private GlobalScope globalScope;
    private List<Definition> definitions;

    /**
     * Constructor.
     * @param uri URI.
     * @param source Source.
     */
    public MagikFile(final URI uri, final String source) {
        this.uri = uri;
        this.source = source;
    }

    /**
     * Constructor.
     * @param uri URI.
     * @param source Source.
     */
    public MagikFile(final String uri, final String source) {
        this(URI.create(uri), source);
    }

    /**
     * Constructor. Read file at path.
     * @param path File to read.
     * @throws IOException -
     */
    public MagikFile(final Path path) throws IOException {
        this.uri = path.toUri();
        final Charset charset = FileCharsetDeterminer.determineCharset(path);
        this.source = Files.readString(path, charset);
    }

    /**
     * Get the URI.
     * @return The URI.
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Get the source text.
     * @return Source text.
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Get the source lines.
     * @return Source lines.
     */
    public String[] getSourceLines() {
        return this.source.split("\r\n|\n|\r");
    }

    /**
     * Parse the text for this file and return the top level {@link AstNode}.
     * @return Top level {@link AstNode}.
     */
    public synchronized AstNode getTopNode() {
        if (this.astNode == null) {
            final MagikParser parser = new MagikParser();
            final String magikSource = this.getSource();
            this.astNode = parser.parseSafe(magikSource);

            // Update URI.
            MagikParser.updateUri(this.astNode, this.uri);
        }

        return this.astNode;
    }

    /**
     * Get the {@link GlobalScope} for this file.
     * @return {@link GlobalScope} for this file.
     */
    public synchronized GlobalScope getGlobalScope() {
        if (this.globalScope == null) {
            final ScopeBuilderVisitor scopeBuilderVisitor = new ScopeBuilderVisitor();
            final AstNode topNode = this.getTopNode();
            scopeBuilderVisitor.walkAst(topNode);
            this.globalScope = scopeBuilderVisitor.getGlobalScope();
        }

        return this.globalScope;
    }

    /**
     * Get {@link Definition}s in this file.
     * @return {@link Definition}s in this file.
     */
    public synchronized List<Definition> getDefinitions() {
        if (this.definitions == null) {
            final DefinitionReader definitionReader = new DefinitionReader();
            final AstNode topNode = this.getTopNode();
            definitionReader.walkAst(topNode);
            this.definitions = definitionReader.getDefinitions();
        }

        return Collections.unmodifiableList(this.definitions);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getUri());
    }

}
