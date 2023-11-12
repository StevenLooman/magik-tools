package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionReader;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader.InstructionType;
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
    private final Map<CommentInstructionReader.InstructionType, Map<Integer, Map<String, String>>> lineInstructions
        = new HashMap<>();
    private final Map<CommentInstructionReader.InstructionType, Map<Scope, Map<String, String>>> scopeInstructions
        = new HashMap<>();

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
            this.astNode = parser.parseSafe(magikSource, this.uri);
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

    /**
     * Get all the line instructions for {@link CommentInstructionReader.InstructionType}.
     * @param instructionType Instruction type to get.
     * @return Map with all instructions, keyed by line number.
     */
    public synchronized Map<Integer, Map<String, String>> getLineInstructions(
            final CommentInstructionReader.InstructionType instructionType) {
        if (!this.lineInstructions.containsKey(instructionType)) {
            final Map<Integer, Map<String, String>> instructions = this.readLineInstructions(instructionType);
            this.lineInstructions.put(instructionType, instructions);
        }

        return Collections.unmodifiableMap(this.lineInstructions.get(instructionType));
    }

    private Map<Integer, Map<String, String>> readLineInstructions(final InstructionType instructionType) {
        final CommentInstructionReader instructionReader = new CommentInstructionReader(this, Set.of(instructionType));

        final String[] sourceLines = this.getSourceLines();
        return IntStream.range(0, sourceLines.length)
            .mapToObj(line -> {
                final String instrAtLine = instructionReader.getInstructionsAtLine(line, instructionType);
                final String lineInstrsStr = Objects.requireNonNullElse(instrAtLine, "");
                final Map<String, String> lineInstrs = CommentInstructionReader.parseInstructions(lineInstrsStr);
                return Map.entry(line, lineInstrs);
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * Get all the Scope instructions for {@link CommentInstructionReader.InstructionType}.
     * @param instructionType Instruction type to get.
     * @return Map with all instructions, keyed by {@link Scope}.
     */
    public synchronized Map<Scope, Map<String, String>> getScopeInstructions(
            final CommentInstructionReader.InstructionType instructionType) {
        if (!this.scopeInstructions.containsKey(instructionType)) {
            final Map<Scope, Map<String, String>> instructions = this.readScopeInstructions(instructionType);
            this.scopeInstructions.put(instructionType, instructions);
        }

        return Collections.unmodifiableMap(this.scopeInstructions.get(instructionType));
    }

    private Map<Scope, Map<String, String>> readScopeInstructions(final InstructionType instructionType) {
        final Map<Scope, Map<String, String>> instructions = new HashMap<>();
        final GlobalScope glblScope = this.getGlobalScope();
        Objects.requireNonNull(glblScope);

        final CommentInstructionReader instructionReader = new CommentInstructionReader(this, Set.of(instructionType));
        glblScope.getSelfAndDescendantScopes().stream()
            .forEach(scope -> {
                final Map<String, String> scopeInstrs =
                    instructionReader.getScopeInstructions(scope, instructionType).stream()
                        .map(instr -> Objects.requireNonNullElse(instr, ""))
                        .map(CommentInstructionReader::parseInstructions)
                        .flatMap(instrs -> instrs.entrySet().stream())
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue));
                instructions.put(scope, scopeInstrs);
            });

        return instructions;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getUri());
    }

}
