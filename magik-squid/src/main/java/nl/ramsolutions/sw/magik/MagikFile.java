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
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionReader;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader.Instruction;
import nl.ramsolutions.sw.magik.parser.MagikParser;

/**
 * Magik file.
 */
public class MagikFile {

    private final MagikAnalysisConfiguration configuration;
    private final URI uri;
    private final String source;
    private AstNode astNode;
    private GlobalScope globalScope;
    private List<Definition> definitions;
    private final Map<CommentInstructionReader.Instruction, Map<Integer, Map<String, String>>> statementInstructions
        = new HashMap<>();
    private final Map<CommentInstructionReader.Instruction, Map<Scope, Map<String, String>>> scopeInstructions
        = new HashMap<>();

    /**
     * Constructor.
     * @param uri URI.
     * @param source Source.
     */
    public MagikFile(final URI uri, final String source) {
        this(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION, uri, source);
    }

    /**
     * Constructor.
     * @param uri URI.
     * @param source Source.
     */
    public MagikFile(final MagikAnalysisConfiguration configuration, final URI uri, final String source) {
        this.configuration = configuration;
        this.uri = uri;
        this.source = source;
    }

    /**
     * Constructor. Read file at path.
     * @param path File to read.
     * @throws IOException -
     */
    public MagikFile(final MagikAnalysisConfiguration configuration, final Path path) throws IOException {
        this.configuration = configuration;
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
            final DefinitionReader definitionReader = new DefinitionReader(this.configuration);
            final AstNode topNode = this.getTopNode();
            definitionReader.walkAst(topNode);
            this.definitions = definitionReader.getDefinitions();
        }

        return Collections.unmodifiableList(this.definitions);
    }

    /**
     * Get all the statement instructions for {@link CommentInstructionReader.Instruction}.
     * @param instruction Instruction to get.
     * @return Map with all instructions, keyed by line number.
     */
    public synchronized Map<Integer, Map<String, String>> getStatementInstructions(
            final CommentInstructionReader.Instruction instruction) {
        if (instruction.getSort() != CommentInstructionReader.Instruction.Sort.STATEMENT) {
            throw new IllegalArgumentException("Excepted Statement instruction");
        }

        if (!this.statementInstructions.containsKey(instruction)) {
            final Map<Integer, Map<String, String>> instructions = this.readStatementInstructions(instruction);
            this.statementInstructions.put(instruction, instructions);
        }

        return Collections.unmodifiableMap(this.statementInstructions.get(instruction));
    }

    private Map<Integer, Map<String, String>> readStatementInstructions(final Instruction instruction) {
        final CommentInstructionReader instructionReader = new CommentInstructionReader(this, Set.of(instruction));

        final String[] sourceLines = this.getSourceLines();
        return IntStream.range(0, sourceLines.length)
            .mapToObj(line -> {
                final String instrAtLine = instructionReader.getInstructionsAtLine(line, instruction);
                final String lineInstrsStr = Objects.requireNonNullElse(instrAtLine, "");
                final Map<String, String> lineInstrs = CommentInstructionReader.parseInstructions(lineInstrsStr);
                return Map.entry(line, lineInstrs);
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * Get all the Scope instructions for {@link CommentInstructionReader.Instruction}.
     * @param instruction Instruction to get.
     * @return Map with all instructions, keyed by {@link Scope}.
     */
    public synchronized Map<Scope, Map<String, String>> getScopeInstructions(
            final CommentInstructionReader.Instruction instruction) {
        if (instruction.getSort() != CommentInstructionReader.Instruction.Sort.SCOPE) {
            throw new IllegalArgumentException("Excepted Scope instruction");
        }

        if (!this.scopeInstructions.containsKey(instruction)) {
            final Map<Scope, Map<String, String>> instructions = this.readScopeInstructions(instruction);
            this.scopeInstructions.put(instruction, instructions);
        }

        return Collections.unmodifiableMap(this.scopeInstructions.get(instruction));
    }

    private Map<Scope, Map<String, String>> readScopeInstructions(final Instruction instruction) {
        final Map<Scope, Map<String, String>> instructions = new HashMap<>();
        final GlobalScope glblScope = this.getGlobalScope();
        Objects.requireNonNull(glblScope);

        final CommentInstructionReader instructionReader = new CommentInstructionReader(this, Set.of(instruction));
        glblScope.getSelfAndDescendantScopes().stream()
            .forEach(scope -> {
                final Map<String, String> scopeInstrs =
                    instructionReader.getScopeInstructions(scope, instruction).stream()
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
