/*
 * Magik Tools - Tools for Magik programming language
 * Copyright © 2020 - 2026 StevenLooman (see AUTHORS file)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNodeType;
import java.util.Map;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Visual alignment structural indent walker.
 *
 * <p>This walker uses visual alignment indentation where body content is indented one tab from the
 * parent construct's token column position (not from the line start). For example:
 *
 * <pre>{@code
 * _local a << _block
 *                 show(:a)
 *             _endblock
 * }</pre>
 *
 * <p>The body content is indented 8 spaces (one tab) from the "_block" token position, and end
 * keywords align with the opening keyword position.
 *
 * <p>This style is useful when you want body content to visually align relative to where the
 * construct keyword appears, especially in assignments where the construct is indented from the
 * line start.
 */
public class VisualIndentWalker extends StructuralIndentWalker {

  /** Strategy name for configuration and identification. */
  public static final String STRATEGY_NAME = "visual";

  /**
   * Constructor.
   *
   * @param options Formatting options including tab size and whether to use spaces.
   * @param tokenEditor Token trivia editor for modifying whitespace tokens.
   */
  VisualIndentWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected boolean isVisualAlignment() {
    return true;
  }

  /**
   * Returns the visual alignment indentation strategy map.
   *
   * <p>Uses INDENT_FROM_PARENT_START for construct bodies (_method, _proc, _block, _if, _try,
   * _catch, _lock, _when, _handling) where body content is indented one tab from the parent token's
   * column position. End keywords align with the opening keyword's column.
   *
   * @return Map from AST node types to their indentation strategies.
   */
  @Override
  protected Map<AstNodeType, IndentStrategy> getStrategyMap() {
    return Map.ofEntries(
        Map.entry(MagikGrammar.MAGIK, IndentStrategy.TOP_LEVEL),
        Map.entry(MagikGrammar.IF, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.METHOD_DEFINITION, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.PROCEDURE_DEFINITION, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.BLOCK, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.PROTECT, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.PROTECTION, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.FINALLY, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.LOOP, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.OVER, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.FOR, IndentStrategy.CLAUSE_FROM_PARENT),
        Map.entry(MagikGrammar.TRY, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.WHEN, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.CATCH, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.LOCK, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.HANDLING, IndentStrategy.INDENT_FROM_PARENT_START),
        Map.entry(MagikGrammar.ARGUMENTS, IndentStrategy.ARGUMENT_LIST),
        Map.entry(MagikGrammar.PARAMETERS, IndentStrategy.ARGUMENT_LIST),
        // Wrapper nodes for when ARGUMENTS/PARAMETERS is skipped in the grammar
        Map.entry(MagikGrammar.ARGUMENTS_PAREN, IndentStrategy.ARGUMENT_LIST),
        Map.entry(MagikGrammar.ARGUMENTS_SQUARE, IndentStrategy.ARGUMENT_LIST),
        Map.entry(MagikGrammar.PARAMETERS_PAREN, IndentStrategy.ARGUMENT_LIST),
        Map.entry(MagikGrammar.PARAMETERS_SQUARE, IndentStrategy.ARGUMENT_LIST),
        Map.entry(MagikGrammar.AND_EXPRESSION, IndentStrategy.ALIGN_TO_FIRST_CHILD),
        Map.entry(MagikGrammar.OR_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.XOR_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.EQUALITY_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.RELATIONAL_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.ADDITIVE_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.MULTIPLICATIVE_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.EXPONENTIAL_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.UNARY_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
        Map.entry(MagikGrammar.ASSIGNMENT_EXPRESSION, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
        Map.entry(
            MagikGrammar.MULTIPLE_ASSIGNMENT_STATEMENT, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
        Map.entry(
            MagikGrammar.VARIABLE_DEFINITION_STATEMENT, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
        Map.entry(
            MagikGrammar.VARIABLE_DEFINITION_MULTI, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
        Map.entry(MagikGrammar.EMIT_STATEMENT, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
        Map.entry(MagikGrammar.SIMPLE_VECTOR, IndentStrategy.COLLECTION_BODY),
        Map.entry(MagikGrammar.TUPLE, IndentStrategy.COLLECTION_BODY),
        Map.entry(MagikGrammar.METHOD_INVOCATION, IndentStrategy.INVOCATION_CHAIN),
        Map.entry(MagikGrammar.PROCEDURE_INVOCATION, IndentStrategy.INVOCATION_CHAIN));
  }
}
