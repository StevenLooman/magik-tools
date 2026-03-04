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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * A formatting walker that handles relative indentation for Magik code.
 *
 * <h2>Magik Indentation Rules</h2>
 *
 * <p>Keywords part of the same structure line up with the first keyword:
 *
 * <ul>
 *   <li>{@code _endblock} lines up with {@code _block}
 *   <li>{@code _endmethod} lines up with {@code _method}
 *   <li>{@code _endproc} lines up with {@code _proc}
 *   <li>{@code _endcatch} lines up with {@code _catch}
 *   <li>{@code _endlock} lines up with {@code _lock}
 *   <li>{@code _endloop} lines up with {@code _loop} (but see special cases below)
 *   <li>{@code _then}/{@code _elif}/{@code _else}/{@code _endif} line up with {@code _if}
 *   <li>{@code _protection}/{@code _endprotect} line up with {@code _protect}
 *   <li>{@code _when}/{@code _endtry} line up with {@code _try}
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * _block
 *         do_something()
 * _endblock
 *
 * _method my_class.my_method()
 *         do_something()
 * _endmethod
 *
 * _proc()
 *         do_something()
 * _endproc
 *
 * _catch :error
 *         handle_error()
 * _endcatch
 *
 * _lock _self
 *         do_something()
 * _endlock
 *
 * _if condition
 * _then
 *         do_something()
 * _elif other_condition
 * _then
 *         do_other()
 * _else
 *         do_default()
 * _endif
 *
 * _protect
 *         do_something()
 * _protection
 *         cleanup()
 * _endprotect
 *
 * _try
 *         do_something()
 * _when error
 *         handle_error()
 * _endtry
 * }</pre>
 *
 * <p>Note that in the AST:
 *
 * <ul>
 *   <li>{@code _elif} and {@code _else} are children of {@code _if}
 *   <li>{@code _for} may have an {@code _over} as a child
 *   <li>{@code _while} may have a {@code _loop} as a child
 *   <li>{@code _over} may have a {@code _loop} as a child
 *   <li>{@code _loop} may exist standalone without {@code _for}, {@code _over}, or {@code _while}
 * </ul>
 *
 * <h2>Special Cases for Lining Up</h2>
 *
 * <p>{@code _loop} lines up with:
 *
 * <ul>
 *   <li>its grandparent {@code _for} (if it exists), otherwise
 *   <li>its parent {@code _over} (if it exists), otherwise
 *   <li>its parent {@code _while} (if it exists), otherwise
 *   <li>it follows regular body indenting rules (as a standalone {@code _loop})
 * </ul>
 *
 * <p>{@code _finally} lines up with the {@code _loop}.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # _loop lines up with _for (grandparent)
 * _for i _over range.elements()
 * _loop
 *         do_something(i)
 * _endloop
 *
 * # _loop lines up with _over (parent, no _for)
 * _over collection.fast_elements()
 * _loop
 *         do_something()
 * _endloop
 *
 * # _loop lines up with _while (parent)
 * _while condition
 * _loop
 *         do_something()
 * _endloop
 *
 * # standalone _loop follows body indentation
 * _method my_class.my_method()
 *         _loop
 *                 do_something()
 *                 _if done?
 *                 _then
 *                         _leave
 *                 _endif
 *         _endloop
 * _endmethod
 *
 * # _finally lines up with _loop
 * _for i _over range.elements()
 * _loop
 *         do_something(i)
 * _finally
 *         cleanup()
 * _endloop
 * }</pre>
 *
 * <h2>Indenting</h2>
 *
 * <p>The top level body has no indent. Each new body indents from its parent body. Bodies start
 * after these keywords:
 *
 * <ul>
 *   <li>{@code _block}
 *   <li>{@code _try}
 *   <li>{@code _when}
 *   <li>{@code _if}
 *   <li>{@code _then}
 *   <li>{@code _elif}
 *   <li>{@code _else}
 *   <li>{@code _loop}
 *   <li>{@code _finally}
 *   <li>{@code _catch}
 *   <li>{@code _method}
 *   <li>{@code _proc}
 *   <li>{@code _lock}
 *   <li>{@code _protect}
 *   <li>{@code _protection}
 *   <li>{@code _handling}
 * </ul>
 *
 * <p>Additional notes:
 *
 * <ul>
 *   <li>{@code _with} indents from {@code _handling}
 *   <li>{@code _default} indents from {@code _handling}
 *   <li>{@code _locking} is on the same line as {@code _protect}
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # _handling with _with and _default
 * _handling error _with
 *         _default
 *
 * # _locking on same line as _protect
 * _protect _locking _self
 *         do_something()
 * _protection
 *         cleanup()
 * _endprotect
 *
 * # Nested bodies
 * _method my_class.complex_method()
 *         _try
 *                 _if condition
 *                 _then
 *                         _for i _over range.elements()
 *                         _loop
 *                                 do_something(i)
 *                         _endloop
 *                 _endif
 *         _when error
 *                 handle_error()
 *         _endtry
 * _endmethod
 * }</pre>
 *
 * <p>Note that in the AST:
 *
 * <ul>
 *   <li>{@code _elif} and {@code _else} are children of {@code _if}
 *   <li>{@code _loop} is a child of {@code _over} or {@code _while}
 *   <li>{@code _over} is a child of {@code _for}
 * </ul>
 *
 * <h2>Top-Level Constructs</h2>
 *
 * <p>Top-level constructs (indent level 0):
 *
 * <ul>
 *   <li>{@code _pragma}
 *   <li>{@code _package}
 *   <li>{@code _private} (when used with {@code _method})
 *   <li>{@code _iter} (when used with {@code _method})
 *   <li>{@code _abstract} (when used with {@code _method})
 *   <li>{@code _method} (and its modifiers)
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * _pragma(classify_level=basic)
 * _method my_class.public_method()
 *         do_something()
 * _endmethod
 *
 * _pragma(classify_level=restricted)
 * _private _method my_class.private_method()
 *         do_something()
 * _endmethod
 *
 * _pragma(classify_level=basic)
 * _iter _method my_class.elements()
 *         _for i _over _self.internal.elements()
 *         _loop
 *                 _loopbody(i)
 *         _endloop
 * _endmethod
 *
 * _pragma(classify_level=advanced)
 * _abstract _method my_class.abstract_method()
 * _endmethod
 *
 * _package my_package
 * }</pre>
 *
 * <h2>Variable Declarations</h2>
 *
 * <p>Variable declarations ({@code _local}, {@code _constant}, {@code _global}, {@code _dynamic},
 * {@code _recursive}):
 *
 * <ul>
 *   <li>The keyword itself follows body indentation rules
 *   <li>Any continuation of the declaration follows other applicable rules (e.g., binary expression
 *       rules, argument rules)
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * _method my_class.my_method()
 *         _local a << 1
 *         _local b << 2
 *         _constant pi << 3.14159
 *         _dynamic !current_value! << 42
 * _endmethod
 *
 * # Multi-variable declaration
 * _method my_class.my_method()
 *         _local a, b, c
 *         _local x << 1,
 *                y << 2,
 *                z << 3
 * _endmethod
 * }</pre>
 *
 * <h2>Other Statements</h2>
 *
 * <p>Other statements ({@code _throw}, {@code _import}, {@code _return}, {@code _leave}, {@code
 * _continue}, {@code _loopbody}):
 *
 * <ul>
 *   <li>Follow body indentation rules
 *   <li>Labels for {@code _leave}/{@code _continue} must be on the same line as the keyword
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * _method my_class.my_method()
 *         _if error?
 *         _then
 *                 _throw :my_error
 *         _endif
 *         _return result
 * _endmethod
 *
 * _method my_class.loop_method()
 *         _for i _over range.elements()
 *         _loop @outer
 *                 _for j _over other.elements()
 *                 _loop @inner
 *                         _if skip_inner?
 *                         _then
 *                                 _continue @inner
 *                         _endif
 *                         _if done?
 *                         _then
 *                                 _leave @outer
 *                         _endif
 *                 _endloop
 *         _endloop
 * _endmethod
 *
 * _method my_class.import_method()
 *         _import x, y
 *         do_something(x, y)
 * _endmethod
 * }</pre>
 *
 * <h2>Simple Vectors and Tuples</h2>
 *
 * <ul>
 *   <li>The first item in a simple_vector/tuple indents from the indentation level of the line
 *       where the simple vector/tuple starts
 *   <li>Any following items line up with the first item
 *   <li>The closing brace is at the indentation level of the line where the simple vector/tuple
 *       starts
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Simple vector on one line
 * _local vec << {1, 2, 3, 4, 5}
 *
 * # Simple vector across multiple lines
 * _local vec << {
 *         :first_item,
 *         :second_item,
 *         :third_item
 * }
 *
 * # Nested vectors
 * _local nested << {
 *         {:a, :b, :c},
 *         {:d, :e, :f},
 *         {:g, :h, :i}
 * }
 *
 * # Tuple (rope)
 * _local tup << {
 *         item_one,
 *         item_two
 * }
 * }</pre>
 *
 * <h2>Binary Expressions</h2>
 *
 * <p>Operator precedence (lowest to highest):
 *
 * <ul>
 *   <li>assignment: {@code <<}, {@code ^}
 *   <li>augmented assignment: {@code operator<<}, {@code operator^}
 *   <li>or: {@code _orif}, {@code _or}
 *   <li>xor: {@code _xor}
 *   <li>and: {@code _andif}, {@code _and}
 *   <li>equality: {@code _isnt}, {@code _is}, {@code _cf}, {@code =}, {@code ~=}, {@code <>}
 *   <li>relational: {@code >}, {@code >=}, {@code <}, {@code <=}
 *   <li>additive: {@code +}, {@code -}
 *   <li>multiplicative: {@code *}, {@code /}, {@code _div}, {@code _mod}
 *   <li>exponential: {@code **}
 *   <li>unary: {@code _allresults}, {@code _scatter}, {@code _not}, {@code ~}, unary {@code +},
 *       unary {@code -}
 *   <li>postfix expression: method invocation, procedure invocation
 * </ul>
 *
 * <p>Binary expressions spread over multiple lines: subsequent lines line up with the left-hand
 * side operand of the same operator.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Assignment spread over multiple lines
 * result << long_expression _andif
 *           another_expression _andif
 *           third_expression
 *
 * # Logical operators - line up with LHS
 * _if condition_one _andif
 *     condition_two _andif
 *     condition_three
 * _then
 *         do_something()
 * _endif
 *
 * # Mixed precedence - each operator lines up with its own LHS
 * result << a + b + c *
 *                   d *
 *                   e
 *
 * # The above parses as: result << ((a + b + (c * d * e)))
 * # So 'b' lines up with 'a' (same + operator)
 * # And 'd' and 'e' line up with 'c' (same * operator)
 *
 * # Boolean expression
 * _if a = b _orif
 *     c = d _orif
 *     e = f
 * _then
 *         do_something()
 * _endif
 *
 * # Relational chain
 * valid? << value >= min _andif
 *           value <= max
 * }</pre>
 *
 * <h2>Parameters and Arguments</h2>
 *
 * <ul>
 *   <li>If the first parameter/argument starts on a new line, it indents from the indentation level
 *       of the line where the definition/invocation starts
 *   <li>Following parameters/arguments line up with the first parameter/argument
 *   <li>If the closing parenthesis is on a new line, it is at the indentation level of the line
 *       where the definition/invocation starts
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Arguments on same line
 * result << object.method(arg1, arg2, arg3)
 *
 * # First argument on same line, rest line up
 * result << object.method(first_argument,
 *                         second_argument,
 *                         third_argument)
 *
 * # First argument on new line, indented from invocation line
 * result << object.method(
 *         first_argument,
 *         second_argument,
 *         third_argument
 * )
 *
 * # Method definition parameters
 * _method my_class.my_method(
 *         p_first_param,
 *         p_second_param,
 *         p_third_param
 * )
 *         do_something()
 * _endmethod
 *
 * # Procedure definition
 * my_proc << _proc(
 *         p_arg1,
 *         p_arg2
 * )
 *         do_something()
 * _endproc
 *
 * # Nested invocations
 * result << outer_call(
 *         inner_call(a, b, c),
 *         another_call(
 *                 x,
 *                 y,
 *                 z
 *         )
 * )
 * }</pre>
 *
 * <h2>Invocations</h2>
 *
 * <p>Chained method calls indent from the beginning of the line where the chain starts.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Chained method calls
 * result << object.first_method()
 *         .second_method()
 *         .third_method()
 *
 * # Chained calls with arguments
 * result << collection.select(predicate)
 *         .map(transformer)
 *         .reduce(initial, reducer)
 *
 * # Chain starting mid-line
 * _local processed << get_object().method_one()
 *         .method_two()
 *         .method_three()
 * }</pre>
 *
 * <h2>Continuations</h2>
 *
 * <ul>
 *   <li>Assignment RHS on a new line indents one level from the assignment line
 *   <li>Return expression ({@code >>}) on a new line indents one level from the return line
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Assignment continuation
 * _local result <<
 *         some_object.method()
 *
 * # Return continuation
 * >>
 *         computed_value
 *
 * # Slot assignment continuation
 * .my_slot <<
 *         compute_value()
 * }</pre>
 *
 * <h2>Comments</h2>
 *
 * <p>Comments follow the surrounding indentation rules.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * # Top-level comment
 * _method my_class.my_method()
 *         # Comment inside method body
 *         _if condition
 *         _then
 *                 # Comment inside if body
 *                 do_something()
 *         _endif
 * _endmethod
 *
 * # Comment in argument list lines up with arguments
 * result << method(
 *         arg1,
 *         # This is a comment about arg2
 *         arg2,
 *         arg3
 * )
 * }</pre>
 *
 * <h2>Blank Lines</h2>
 *
 * <p>Blank lines have no indent.
 *
 * <p>Example:
 *
 * <pre>{@code
 * _method my_class.my_method()
 *         do_first_thing()
 *
 *         do_second_thing()
 *
 *         do_third_thing()
 * _endmethod
 * }</pre>
 *
 * <h2>Formatting Options</h2>
 *
 * <p>Single-line vs expanded: The formatter can optionally keep simple constructs on a single line
 * or expand them fully.
 *
 * <p>Examples (single-line option enabled):
 *
 * <pre>{@code
 * _if done? _then _leave _endif
 *
 * _if x > 0 _then _return x _endif
 * }</pre>
 *
 * <p>Examples (expanded):
 *
 * <pre>{@code
 * _if done?
 * _then
 *         _leave
 * _endif
 *
 * _if x > 0
 * _then
 *         _return x
 * _endif
 * }</pre>
 *
 * <h2>Default Indentation</h2>
 *
 * <p>Default indenting size is 8, using the tab character. For any indent not a multiple of 8, use
 * tabs to indent as far as possible, followed by spaces for the remainder.
 *
 * <p>Example (where [TAB] represents a tab character and [SP] a space):
 *
 * <pre>{@code
 * # Indent of 8 (one tab)
 * [TAB]do_something()
 *
 * # Indent of 16 (two tabs)
 * [TAB][TAB]do_nested()
 *
 * # Indent of 12 (one tab + 4 spaces) - e.g., lining up with argument
 * result << method(first_arg,
 * [TAB][SP][SP][SP][SP]second_arg)
 * }</pre>
 *
 * <p>This walker uses a strategy pattern to determine indentation for different AST node types.
 * Each token is indented based on its parent node type and the applicable indentation strategy.
 *
 * <h2>Strategy Pattern</h2>
 *
 * <p>The walker defines an {@link IndentStrategy} enum with the following strategies:
 *
 * <ul>
 *   <li>{@code TOP_LEVEL} - No indentation (column 0). Used for the top-level MAGIK node.
 *   <li>{@code IF_KEYWORD_OR_BODY} - Special handling for IF constructs where keywords (_then,
 *       _else, _elif, _endif) align with _if, and body content is indented.
 *   <li>{@code BODY_FROM_PARENT_START} - Body content is indented one level from the parent's start
 *       token. Used for methods, procedures, blocks, try, catch, lock.
 *   <li>{@code ARGUMENT_LIST} - Arguments/parameters align with the first argument. Used for method
 *       and procedure arguments.
 *   <li>{@code ALIGN_TO_FIRST_CHILD} - Continuation lines align with the first child token. Used
 *       for AND expressions.
 *   <li>{@code ASSIGNMENT_RIGHT_HAND_SIDE} - Right-hand side of assignments is indented from the
 *       start of the line containing the assignment.
 *   <li>{@code COLLECTION_BODY} - Collection elements are indented from the opening brace; closing
 *       brace aligns with the line start.
 *   <li>{@code INVOCATION_CHAIN} - Method invocation chains are indented from the statement start.
 *       Supports fluent interfaces.
 *   <li>{@code CLAUSE_FROM_PARENT} - Clause keywords (_loop, _protection, _when, etc.) align with
 *       their parent clause. Body is indented from the clause keyword.
 *   <li>{@code EXPRESSION_PRECEDENCE} - Binary expressions respect operator precedence for
 *       alignment.
 * </ul>
 *
 * <h2>Node Type to Strategy Mapping</h2>
 *
 * <p>The {@link #STRATEGY_BY_NODE_TYPE} map defines which strategy applies to each AST node type.
 * When indenting a token, the walker finds the nearest ancestor node that has a strategy mapping
 * and applies that strategy.
 *
 * <h2>Example Indentation</h2>
 *
 * <pre>{@code
 * _method my_class.my_method(arg1,    # BODY_FROM_PARENT_START
 *                            arg2)    # ARGUMENT_LIST - aligns with arg1
 *     _local result <<                # indented from _method
 *         _if condition               # ASSIGNMENT_RIGHT_HAND_SIDE
 *         _then                       # IF_KEYWORD_OR_BODY - aligns with _if
 *             value1                  # indented from _if
 *         _else                       # aligns with _if
 *             value2                  # indented from _if
 *         _endif                      # aligns with _if
 *     _return result
 * _endmethod                          # aligns with _method
 * }</pre>
 *
 * @see FormattingWalker
 * @see IndentStrategy
 */
public abstract class StructuralIndentWalker extends FormattingWalker {

  /**
   * Indentation strategies for different AST constructs.
   *
   * <p>Each strategy defines how tokens within a particular AST node type should be indented
   * relative to their parent or sibling tokens.
   */
  protected enum IndentStrategy {
    /** No indentation - tokens at column 0. Used for top-level constructs. */
    TOP_LEVEL,

    /**
     * Body content is indented one tab stop from the parent's start token. Structural keywords
     * (like _then/_else/_endif for IF, or _endmethod for methods) align with the start keyword.
     * Used for: _method, _proc, _block, _if, _try, _catch, _lock.
     */
    BODY_FROM_PARENT_START,

    /**
     * Arguments or parameters align with the first argument/parameter. If the first
     * argument/parameter is on a new line, it's indented from the line containing the opening
     * parenthesis.
     */
    ARGUMENT_LIST,

    /**
     * Like ARGUMENT_LIST but continuation arguments/parameters always indent one tab from the start
     * of the line containing the opening parenthesis, even when the first argument is on the same
     * line as the opening parenthesis. Used by the block indent walker.
     */
    ARGUMENT_LIST_BLOCK,

    /**
     * Continuation lines align with the first child token of the expression. Primarily used for AND
     * expressions in IF conditions.
     */
    ALIGN_TO_FIRST_CHILD,

    /**
     * Right-hand side of assignments is indented one level from the start of the line containing
     * the assignment operator. Used for both regular assignments (<<) and variable definitions
     * (_local, _constant, etc.).
     */
    ASSIGNMENT_RIGHT_HAND_SIDE,

    /**
     * Multi-variable declarations where continuation variables align with the first variable. E.g.,
     * `_local a,\n b,\n c` where b and c align with a.
     */
    VARIABLE_DEFINITION_LIST,

    /**
     * Collection elements ({...}) are indented from the opening brace. The closing brace aligns
     * with the start of the line containing the opening brace.
     */
    COLLECTION_BODY,

    /**
     * Method/procedure invocation chains are indented from the start of the containing statement.
     * Supports fluent interface patterns like: obj.method1(). method2(). method3()
     */
    INVOCATION_CHAIN,

    /**
     * Clause keywords (_loop, _protection, _finally, _when) align with their parent clause or
     * construct. Body content within the clause is indented one level. End keywords align with the
     * clause keyword.
     */
    CLAUSE_FROM_PARENT,

    /**
     * Binary expressions respect operator precedence for alignment. Higher-precedence sub
     * expressions align with their start token; lower-precedence align with their parent.
     */
    EXPRESSION_PRECEDENCE,

    /**
     * Body content is indented one tab stop from the parent token's starting column. This creates
     * indentation where body content is offset by the standard tab width from where the parent
     * keyword begins. Example:
     *
     * <pre>{@code
     * _local a << _block
     *                 show(:a)
     *             _endblock
     * }</pre>
     *
     * The body content is indented 8 spaces (one tab) from the start of "_block".
     */
    INDENT_FROM_PARENT_START
  }

  /** AST node types that serve as indentation anchors. Derived from strategy map keys. */
  private final AstNodeType[] parentNodeTypes;

  /**
   * AST node types with BODY_FROM_PARENT_START strategy. Derived from strategy map for use in
   * assignment detection.
   */
  private final AstNodeType[] bodyFromParentStartConstructs;

  /**
   * AST node types with INDENT_FROM_PARENT_START strategy. Derived from strategy map for use in
   * assignment detection (e.g. VisualIndentWalker uses this for proc/method/block/if).
   */
  private final AstNodeType[] indentFromParentTokenStartConstructs;

  /**
   * AST node types that can contain body content (BODY_FROM_PARENT_START or CLAUSE_FROM_PARENT
   * strategies, plus MAGIK). Used for finding enclosing constructs for expression indentation.
   */
  private final AstNodeType[] bodyContainingConstructs;

  /**
   * Wrapper node types for arguments and parameters. Includes PROCEDURE_INVOCATION which is
   * semantically equivalent to ARGUMENTS_PAREN but has a different node type.
   */
  private static final AstNodeType[] ARGUMENT_PARAMETER_WRAPPER_TYPES =
      new AstNodeType[] {
        MagikGrammar.ARGUMENTS_PAREN,
        MagikGrammar.ARGUMENTS_SQUARE,
        MagikGrammar.PARAMETERS_PAREN,
        MagikGrammar.PARAMETERS_SQUARE,
        MagikGrammar.PROCEDURE_INVOCATION
      };

  /**
   * Maps AST node types to their indentation strategies.
   *
   * <p>This map defines the indentation behavior for each supported node type. Node types not in
   * this map will delegate to their parent node's strategy.
   */
  private final Map<AstNodeType, IndentStrategy> strategyByNodeType;

  /**
   * Returns the strategy map that defines indentation behavior for each node type.
   *
   * <p>Subclasses override this method to provide their specific indentation strategy
   * configuration.
   *
   * @return Map from AST node types to their indentation strategies.
   */
  protected abstract Map<AstNodeType, IndentStrategy> getStrategyMap();

  /**
   * Returns whether this walker performs visual alignment. When true, expression continuation lines
   * are aligned to the visual column of the first operand, even when that operand follows an
   * assignment operator on the same line.
   *
   * @return true if visual alignment is enabled, false for structural/block indentation.
   */
  protected boolean isVisualAlignment() {
    return false;
  }

  /**
   * Maps keywords to the node type they should unwind to. Used by getParentNode to handle end
   * keywords and clause keywords that need to find their matching construct.
   */
  private static final Map<MagikKeyword, AstNodeType> KEYWORD_TO_NODE_TYPE =
      Map.ofEntries(
          Map.entry(MagikKeyword.THEN, MagikGrammar.IF),
          Map.entry(MagikKeyword.ELSE, MagikGrammar.IF),
          Map.entry(MagikKeyword.ELIF, MagikGrammar.IF),
          Map.entry(MagikKeyword.ENDIF, MagikGrammar.IF),
          Map.entry(MagikKeyword.ENDPROC, MagikGrammar.PROCEDURE_DEFINITION),
          Map.entry(MagikKeyword.ENDMETHOD, MagikGrammar.METHOD_DEFINITION),
          Map.entry(MagikKeyword.ENDBLOCK, MagikGrammar.BLOCK),
          Map.entry(MagikKeyword.FOR, MagikGrammar.FOR),
          Map.entry(MagikKeyword.OVER, MagikGrammar.OVER),
          Map.entry(MagikKeyword.LOOP, MagikGrammar.LOOP),
          Map.entry(MagikKeyword.ENDLOOP, MagikGrammar.LOOP),
          Map.entry(MagikKeyword.PROTECT, MagikGrammar.PROTECT),
          Map.entry(MagikKeyword.ENDPROTECT, MagikGrammar.PROTECT),
          Map.entry(MagikKeyword.PROTECTION, MagikGrammar.PROTECTION),
          Map.entry(MagikKeyword.FINALLY, MagikGrammar.FINALLY),
          Map.entry(MagikKeyword.WHEN, MagikGrammar.WHEN),
          Map.entry(MagikKeyword.ENDTRY, MagikGrammar.TRY),
          Map.entry(MagikKeyword.ENDCATCH, MagikGrammar.CATCH),
          Map.entry(MagikKeyword.ENDLOCK, MagikGrammar.LOCK));

  /**
   * Operator precedence order for binary expressions.
   *
   * <p>Lower index = lower precedence. Used by {@link IndentStrategy#EXPRESSION_PRECEDENCE} to
   * determine alignment when multiple binary operators appear in an expression.
   */
  private static final List<AstNodeType> OPERATOR_PRECEDENCE_NODE_TYPES =
      List.of(
          MagikGrammar.OR_EXPRESSION,
          MagikGrammar.XOR_EXPRESSION,
          MagikGrammar.AND_EXPRESSION,
          MagikGrammar.EQUALITY_EXPRESSION,
          MagikGrammar.RELATIONAL_EXPRESSION,
          MagikGrammar.ADDITIVE_EXPRESSION,
          MagikGrammar.MULTIPLICATIVE_EXPRESSION,
          MagikGrammar.EXPONENTIAL_EXPRESSION,
          MagikGrammar.UNARY_EXPRESSION);

  /** The current AST node being processed during tree traversal. */
  private AstNode currentNode;

  /** The last token processed (including whitespace). */
  private Token lastToken;

  /** The last non-whitespace token processed. Used for line-change detection. */
  private Token lastTextToken;

  /**
   * Constructor.
   *
   * @param options Formatting options including tab size and whether to use spaces.
   * @param tokenEditor Token trivia editor for modifying whitespace tokens.
   */
  protected StructuralIndentWalker(
      final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);

    // Initialize strategy map from subclass
    this.strategyByNodeType = this.getStrategyMap();

    // Derive array fields from strategy map
    this.parentNodeTypes = this.strategyByNodeType.keySet().toArray(AstNodeType[]::new);
    this.bodyFromParentStartConstructs =
        this.strategyByNodeType.entrySet().stream()
            .filter(entry -> entry.getValue() == IndentStrategy.BODY_FROM_PARENT_START)
            .map(Map.Entry::getKey)
            .toArray(AstNodeType[]::new);
    this.indentFromParentTokenStartConstructs =
        this.strategyByNodeType.entrySet().stream()
            .filter(entry -> entry.getValue() == IndentStrategy.INDENT_FROM_PARENT_START)
            .map(Map.Entry::getKey)
            .toArray(AstNodeType[]::new);
    this.bodyContainingConstructs =
        this.strategyByNodeType.entrySet().stream()
            .filter(
                entry ->
                    entry.getValue() == IndentStrategy.BODY_FROM_PARENT_START
                        || entry.getValue() == IndentStrategy.CLAUSE_FROM_PARENT
                        || entry.getValue() == IndentStrategy.TOP_LEVEL)
            .map(Map.Entry::getKey)
            .toArray(AstNodeType[]::new);
  }

  /** {@inheritDoc} Tracks the current node as we descend into the AST. */
  @Override
  protected void walkPreDefault(final AstNode node) {
    this.currentNode = node;
  }

  /** {@inheritDoc} Restores the parent node as we ascend out of the AST. */
  @Override
  protected void walkPostDefault(final AstNode node) {
    this.currentNode = this.currentNode.getParent();
  }

  /**
   * {@inheritDoc} Handles indentation of comment tokens (trivia).
   *
   * <p>Comments on their own line are indented based on the current context. Comments at the end of
   * a line with code are left as-is.
   */
  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (!trivia.getToken().getType().equals(GenericTokenType.COMMENT)) {
      return;
    }

    final Token triviaToken = trivia.getToken();
    if (!triviaToken.isOnSameLineThan(this.lastToken)) {
      this.walkLineCommentToken(this.lastTextToken, triviaToken);
    }

    this.lastTextToken = triviaToken;
  }

  /**
   * {@inheritDoc} Handles indentation of code tokens.
   *
   * <p>Only tokens on new lines (not on the same line as the previous token) are processed for
   * indentation. Tokens on the same line as their predecessor are left as-is.
   */
  @Override
  protected void walkToken(final Token token) {
    if (!token.isOnSameLineThan(this.lastTextToken)) {
      this.handleTokenNode(token, this.currentNode);
    }

    this.lastToken = token;
    this.lastTextToken = token;
  }

  /**
   * Determines the parent node and dispatches to the appropriate indentation handler.
   *
   * @param token The token to indent.
   * @param node The AST node containing the token.
   */
  private void handleTokenNode(final Token token, final AstNode node) {
    final AstNode parentNode = this.getParentNode(token, node);

    this.indentTokenNode(token, parentNode);
  }

  /**
   * Indents a comment token that appears on its own line.
   *
   * @param token The previous non-whitespace token.
   * @param commentToken The comment token to indent.
   */
  private void walkLineCommentToken(final Token token, final Token commentToken) {
    // Find the appropriate node for indenting this comment.
    // Walk up the parent chain until we find a node whose first token is on or before
    // the comment's line. This ensures comments before a statement are indented
    // relative to the containing block, not the statement they precede.
    // Stop at the root node (MAGIK) to ensure top-level comments get indent 0.
    AstNode usedNode = this.currentNode;
    while (usedNode.getParent() != null && commentToken.getLine() < usedNode.getToken().getLine()) {
      usedNode = usedNode.getParent();
    }
    this.indentTokenNode(commentToken, usedNode);
  }

  /**
   * Core indentation dispatcher. Looks up the strategy for the parent node type and delegates to
   * the appropriate indentation method.
   *
   * <p>If no strategy is found for the node type, recursively checks the parent node until a
   * strategy is found or falls back to zero indentation.
   *
   * @param token The token to indent.
   * @param parentNode The AST node that determines the indentation strategy.
   */
  private void indentTokenNode(final Token token, final AstNode parentNode) {
    final IndentStrategy strategy = this.strategyByNodeType.get(parentNode.getType());
    if (strategy == null) {
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
        return;
      }

      this.ensureIndent(token, 0);
      return;
    }

    switch (strategy) {
      case TOP_LEVEL -> this.ensureIndent(token, 0);
      case BODY_FROM_PARENT_START -> this.indentFromParentStart(token, parentNode);
      case ARGUMENT_LIST ->
          this.indentArgumentOrParameter(token, parentNode, IndentStrategy.ARGUMENT_LIST);
      case ARGUMENT_LIST_BLOCK ->
          this.indentArgumentOrParameter(token, parentNode, IndentStrategy.ARGUMENT_LIST_BLOCK);
      case ALIGN_TO_FIRST_CHILD -> this.indentAlignedToFirstChild(token, parentNode);
      case ASSIGNMENT_RIGHT_HAND_SIDE -> this.indentAssignmentRhs(token, parentNode);
      case COLLECTION_BODY -> this.indentCollection(token, parentNode);
      case INVOCATION_CHAIN -> this.indentInvocation(token, parentNode);
      case CLAUSE_FROM_PARENT -> this.indentClauseFromParent(token, parentNode);
      case EXPRESSION_PRECEDENCE -> this.indentExpressionWithPrecedence(token, parentNode);
      case INDENT_FROM_PARENT_START -> this.indentFromParentTokenStart(token, parentNode);
      default -> throw new IllegalStateException("Unhandled indent strategy: " + strategy);
    }
  }

  /**
   * Indents body content one level from the parent's start token.
   *
   * <p>Structural keywords align with the start keyword rather than being indented. This includes:
   *
   * <ul>
   *   <li>IF: _then, _else, _elif, _endif
   *   <li>METHOD/PROC/BLOCK: _endmethod, _endproc, _endblock
   *   <li>Closing parentheses for method/procedure parameters
   * </ul>
   *
   * <p>For procedures in assignments, body content and structural keywords align with the line
   * start rather than _proc.
   *
   * @param token The token to indent.
   * @param parentNode The parent AST node (method, procedure, block, or if).
   */
  private void indentFromParentStart(final Token token, final AstNode parentNode) {
    final Token referenceToken = parentNode.getToken();

    // If this is the first token of the node (including method modifiers like _private, _iter),
    // delegate to the parent's strategy to determine the correct indent level.
    if (token == referenceToken) {
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    // For constructs in assignments, use line start as reference
    final Token effectiveReference =
        this.isConstructInAssignment(parentNode)
            ? this.getFirstTextTokenOnLine(referenceToken)
            : referenceToken;

    // Structural keywords line up with the start keyword, not indented from it.
    if (this.tokenIs(
        token,
        MagikKeyword.THEN,
        MagikKeyword.ELSE,
        MagikKeyword.ELIF,
        MagikKeyword.ENDIF,
        MagikKeyword.ENDPROC,
        MagikKeyword.ENDMETHOD,
        MagikKeyword.ENDBLOCK,
        MagikKeyword.WHEN,
        MagikKeyword.ENDTRY,
        MagikKeyword.ENDCATCH,
        MagikKeyword.ENDLOCK)) {
      this.ensureIndentLinedUpWith(token, effectiveReference);
      return;
    }

    // Closing parentheses for method/procedure parameters align with the line start
    if (this.tokenIs(token, MagikPunctuator.PAREN_R)
        && parentNode.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)) {
      final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(referenceToken);
      this.ensureIndentLinedUpWith(token, firstTextTokenOnLine);
      return;
    }

    this.ensureIndentFrom(token, effectiveReference);
  }

  /**
   * Checks if a construct with BODY_FROM_PARENT_START or INDENT_FROM_PARENT_START strategy is part
   * of an assignment or variable definition.
   *
   * <p>This includes procedures, methods, blocks, if statements, try/catch, lock, handling, etc.
   * when they appear on the right-hand side of an assignment.
   */
  private boolean isConstructInAssignment(final AstNode node) {
    if (!node.is(this.bodyFromParentStartConstructs)
        && !node.is(this.indentFromParentTokenStartConstructs)) {
      return false;
    }
    // These constructs are wrapped in ATOM → EXPRESSION, so check ancestors
    // Path: BLOCK → ATOM → EXPRESSION → VARIABLE_DEFINITION or ASSIGNMENT_EXPRESSION
    final AstNode parent = node.getParent();
    if (parent == null) {
      return false;
    }

    // Search up the ancestry for ASSIGNMENT_EXPRESSION or VARIABLE_DEFINITION
    AstNode ancestor = parent;
    while (ancestor != null) {
      if (ancestor.is(MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.VARIABLE_DEFINITION)) {
        return true;
      }
      // Stop searching if we hit a statement boundary or another construct
      if (ancestor.is(
          MagikGrammar.STATEMENT,
          MagikGrammar.METHOD_DEFINITION,
          MagikGrammar.PROCEDURE_DEFINITION,
          MagikGrammar.BLOCK)) {
        break;
      }
      ancestor = ancestor.getParent();
    }

    return false;
  }

  /**
   * Indents arguments or parameters within a method/procedure call or definition.
   *
   * <p>If the first argument is on a new line, it's indented from the line containing the opening
   * parenthesis. Subsequent arguments align with the first argument (ARGUMENT_LIST) or always
   * indent one tab from the line containing the opening parenthesis (ARGUMENT_LIST_BLOCK). The
   * closing parenthesis aligns with the line where the invocation/definition starts.
   *
   * @param token The token to indent.
   * @param parentNode The ARGUMENTS or PARAMETERS AST node.
   * @param strategy Either ARGUMENT_LIST or ARGUMENT_LIST_BLOCK.
   */
  private void indentArgumentOrParameter(
      final Token token, final AstNode parentNode, final IndentStrategy strategy) {
    // Handle closing parenthesis - should align with the start of the invocation/definition line
    if (this.tokenIs(token, MagikPunctuator.PAREN_R, MagikPunctuator.SQUARE_R)) {
      // Find the wrapper node (ARGUMENTS_PAREN, PARAMETERS_PAREN, etc.) to get the opening paren
      // Check if parentNode itself is the wrapper (when ARGUMENTS/PARAMETERS is skipped in grammar)
      // Note: PROCEDURE_INVOCATION is defined as ARGUMENTS_PAREN in the grammar
      AstNode wrapperNode = null;
      if (parentNode.is(ARGUMENT_PARAMETER_WRAPPER_TYPES)) {
        wrapperNode = parentNode;
      } else {
        wrapperNode = parentNode.getFirstAncestor(ARGUMENT_PARAMETER_WRAPPER_TYPES);
      }
      if (wrapperNode != null) {
        final Token openingToken = wrapperNode.getToken();
        final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(openingToken);
        this.ensureIndentLinedUpWith(token, firstTextTokenOnLine);
      }
      return;
    }

    final AstNode firstNode =
        parentNode.getFirstChild(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER);
    if (firstNode == null) {
      // Empty argument/parameter list - indent from parent
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    final Token firstNodeToken = firstNode.getToken();
    if (token == firstNodeToken) {
      final Token parentToken = parentNode.getToken();
      final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(parentToken);
      // For constructs in assignments, skip indentation (handled by BODY_FROM_PARENT_START)
      final AstNode constructAncestor =
          parentNode.getFirstAncestor(this.bodyFromParentStartConstructs);
      if (constructAncestor != null && this.isConstructInAssignment(constructAncestor)) {
        return;
      }
      // For method call arguments (not procedure definition parameters), we need to bypass the
      // assignment operator check in ensureIndentFrom since "a << obj.method(\n:arg)" should
      // still indent the argument.
      final int referenceColumn = this.getEffectiveIndentSize(firstTextTokenOnLine);
      final int indentSize = referenceColumn + this.getOptions().getTabSize();
      this.ensureIndent(token, indentSize);
    } else {
      // For constructs in assignments, skip indentation (handled by BODY_FROM_PARENT_START)
      final AstNode constructAncestor =
          parentNode.getFirstAncestor(this.bodyFromParentStartConstructs);
      if (constructAncestor != null && this.isConstructInAssignment(constructAncestor)) {
        return;
      }
      if (strategy == IndentStrategy.ARGUMENT_LIST_BLOCK) {
        // Block style: always indent one tab from the line containing the opening paren,
        // regardless of whether the first arg is on the same line.
        final Token parentToken = parentNode.getToken();
        final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(parentToken);
        final int referenceColumn = this.getEffectiveIndentSize(firstTextTokenOnLine);
        final int indentSize = referenceColumn + this.getOptions().getTabSize();
        this.ensureIndent(token, indentSize);
      } else {
        // Visual style: continuation args/params align with the first arg/param's column.
        final int indentSize = this.getEffectiveIndentSize(firstNodeToken);
        this.ensureIndent(token, indentSize);
      }
    }
  }

  /**
   * Aligns continuation lines with the first child of an expression.
   *
   * <p>Primarily used for AND expressions within IF conditions, where continuation lines should
   * align with the first operand.
   *
   * @param token The token to indent.
   * @param parentNode The expression AST node.
   */
  private void indentAlignedToFirstChild(final Token token, final AstNode parentNode) {
    final AstNode firstChild = parentNode.getFirstChild();
    if (firstChild == null) {
      return;
    }

    final Token referenceToken = firstChild.getToken();
    if (token == referenceToken) {
      // First token of this node - delegate to the parent's strategy so that e.g. the first
      // token of a subsequent AND_EXPRESSION inside an OR_EXPRESSION is aligned correctly.
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    this.ensureIndentLinedUpWith(token, referenceToken);
  }

  /**
   * Indents body content one tab stop from the parent token's starting column.
   *
   * <p>This creates indentation where body content is offset by the standard tab width (typically 8
   * spaces) from the parent token's column position. For example:
   *
   * <pre>{@code
   * _local a << _block
   *                 show(:a)
   *             _endblock
   * }</pre>
   *
   * <p>In this example, the body content is indented 8 spaces (one tab) from the start of "_block".
   * End keywords (_endblock, _endmethod, etc.) align with the opening keyword, not with the body.
   *
   * @param token The token to indent.
   * @param parentNode The parent AST node (block, method, procedure, etc.).
   */
  private void indentFromParentTokenStart(final Token token, final AstNode parentNode) {
    final Token parentToken = parentNode.getToken();

    // If this is the first token of the node, delegate to parent's strategy
    if (token == parentToken) {
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    // Check if this is an end keyword or structural clause keyword that should align with the start
    // keyword
    if (this.isEndKeyword(token) || this.isClauseKeyword(token)) {
      final int parentIndent = this.getEffectiveIndentSize(parentToken);
      this.ensureIndent(token, parentIndent);
      return;
    }

    // Calculate indentation: parent token indent + one tab stop
    final int parentIndent = this.getEffectiveIndentSize(parentToken);
    final int indentSize = parentIndent + this.getOptions().getTabSize();

    this.ensureIndent(token, indentSize);
  }

  /**
   * Indents the right-hand side of an assignment expression.
   *
   * <p>The RHS is indented one level from the start of the line containing the assignment. This
   * applies to both regular assignments (<<) and variable definitions (_local, _constant, etc.).
   *
   * <p>For multi-variable declarations (e.g., `_local a, b, c`), continuation variables align with
   * the first variable rather than being indented from the line start.
   *
   * @param token The token to indent.
   * @param parentNode The ASSIGNMENT_EXPRESSION, VARIABLE_DEFINITION_STATEMENT, or EMIT_STATEMENT
   *     AST node.
   */
  private void indentAssignmentRhs(final Token token, final AstNode parentNode) {
    // Handle multi-variable declarations where continuation variables align with the first
    if (parentNode.is(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)) {
      final List<AstNode> varDefs = parentNode.getChildren(MagikGrammar.VARIABLE_DEFINITION);
      if (varDefs.size() > 1) {
        final AstNode firstVarDef = varDefs.get(0);
        final Token firstVarDefToken = firstVarDef.getToken();
        // Check if token is the start of a continuation VARIABLE_DEFINITION
        for (int i = 1; i < varDefs.size(); i++) {
          final AstNode varDef = varDefs.get(i);
          if (varDef.getToken() == token) {
            // This is a continuation variable - align with the first variable
            this.ensureIndentLinedUpWith(token, firstVarDefToken);
            return;
          }
        }
      }
    }

    // For EMIT_STATEMENT (>>), the first token is always >> itself
    final Token parentToken = parentNode.getToken();
    if (token == parentToken) {
      // First token (>>, <<, or variable modifier) - handled by parent strategy
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    // Special handling: preserve existing reasonable indentation for nested constructs
    // within assignments to avoid over-correcting already well-formatted code.
    final int currentIndent = this.getLeadingWhitespaceVisualWidth(token);
    final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(parentToken);

    // Get the reference indent. If it's 0, check if we're inside a body construct.
    // If so, the 0 is likely wrong (token hasn't been indented yet), so calculate
    // what it should be from the parent context.
    int referenceIndent = this.getEffectiveIndentSize(firstTextTokenOnLine);
    if (referenceIndent == 0) {
      // Check if firstTextTokenOnLine is inside a body construct
      AstNode ancestor = parentNode.getParent();
      while (ancestor != null && !ancestor.is(MagikGrammar.MAGIK)) {
        if (ancestor.is(this.bodyContainingConstructs)) {
          // We're inside a body, so referenceIndent==0 is wrong.
          // Calculate the correct body indent from the construct.
          final Token constructToken = ancestor.getToken();
          final int constructIndent = this.getEffectiveIndentSize(constructToken);
          referenceIndent = constructIndent + this.getOptions().getTabSize();
          break;
        }
        ancestor = ancestor.getParent();
      }
    }

    final int expectedIndent = referenceIndent + this.getOptions().getTabSize();

    // Check if token is the first token of a BODY_FROM_PARENT_START construct in an assignment.
    // These constructs (_proc, _if, _block, etc.) should be indented one tab from the assignment
    // line.
    final AstNode tokenNode = this.currentNode;
    if (this.isConstructInAssignment(tokenNode) && token == tokenNode.getToken()) {
      // Use the expectedIndent (one tab from where the assignment LHS starts)
      this.ensureIndent(token, expectedIndent);
      return;
    }

    if (currentIndent == 0 && referenceIndent == this.getOptions().getTabSize()) {
      // referenceIndent is one tab, which means we corrected it from 0 above.
      // This means we're inside a body, so don't preserve column 0 - continue to indent.
    }

    // If current indentation exactly matches expected, preserve it.
    // Use exact match to ensure single-pass convergence.
    if (currentIndent == expectedIndent) {
      // Even though we're preserving the indent, we still need to call ensureIndent
      // to set the trivia on the token. This is important for cloned ASTs where tokens
      // have no trivia initially - child tokens that reference this token will need
      // to read its indent via getLeadingWhitespaceVisualWidth.
      this.ensureIndent(token, currentIndent);
      return;
    }

    this.ensureIndentFrom(token, firstTextTokenOnLine);
  }

  /**
   * Indents elements within a collection (simple vector or tuple).
   *
   * <p>Collection elements are indented one level from the opening brace/paren. The closing
   * brace/paren aligns with the start of the line containing the opening brace/paren.
   *
   * @param token The token to indent.
   * @param parentNode The SIMPLE_VECTOR or TUPLE AST node.
   */
  private void indentCollection(final Token token, final AstNode parentNode) {
    // For non-parenthesized TUPLEs, delegate to parent strategy
    if (parentNode.is(MagikGrammar.TUPLE) && !this.isParenthesizedTuple(parentNode)) {
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
      }
      return;
    }

    final AstNode firstChild = parentNode.getFirstChild();
    final Token referenceToken = firstChild.getToken();
    final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(referenceToken);

    // Closing braces/parens should line up with the start of the line, not the opening brace/paren
    if (this.tokenIs(token, MagikPunctuator.BRACE_R, MagikPunctuator.PAREN_R)) {
      this.ensureIndentLinedUpWith(token, firstTextTokenOnLine);
      return;
    }

    // Nested collections (opening braces inside a collection) indent from line start.
    // This handles patterns like: create_table({ {:col1, :int}, {:col2, :str} })
    // where inner vectors should be indented from line start, not aligned with outer {.
    if (this.tokenIs(token, MagikPunctuator.BRACE_L, MagikPunctuator.PAREN_L)) {
      this.ensureIndentFrom(token, firstTextTokenOnLine);
      return;
    }

    // If the first element is on the same line as the opening brace, align continuation items
    // with it (visual alignment). Example:
    //   {:a, :b,
    //    :c}        <- :c aligns with :a, not one tab from {
    if (this.isVisualAlignment()) {
      final AstNode firstElementNode = firstChild.getNextSibling();
      if (firstElementNode != null
          && !this.tokenIs(
              firstElementNode.getToken(), MagikPunctuator.BRACE_R, MagikPunctuator.PAREN_R)) {
        final Token firstElementToken = firstElementNode.getToken();
        if (firstElementToken.getLine() == referenceToken.getLine()) {
          final int indentSize = this.getEffectiveIndentSize(firstElementToken);
          this.ensureIndent(token, indentSize);
          return;
        }
      }
    }

    // Collection content is always indented from the line where the opening brace/paren is.
    // This handles patterns like: call({ 1 }) where the 1 should be indented from line start.
    this.ensureIndentFrom(token, firstTextTokenOnLine);
  }

  /** Checks if a TUPLE node is parenthesized (starts with PAREN_L). */
  private boolean isParenthesizedTuple(final AstNode tupleNode) {
    final AstNode firstChild = tupleNode.getFirstChild();
    if (firstChild == null) {
      return false;
    }
    final Token firstToken = firstChild.getToken();
    return firstToken != null && this.tokenIs(firstToken, MagikPunctuator.PAREN_L);
  }

  /**
   * Indents method/procedure invocation chains.
   *
   * <p>Invocation chains (fluent interfaces) are indented one level deeper than the line where the
   * chain starts. This supports patterns like:
   *
   * <pre>
   * _local result <<
   *   obj.method1().
   *     method2().
   *     method3()
   * </pre>
   *
   * @param token The token to indent.
   * @param invocationNode The METHOD_INVOCATION or PROCEDURE_INVOCATION AST node.
   */
  private void indentInvocation(final Token token, final AstNode invocationNode) {
    // Find the containing postfix expression (which holds the object and all method invocations)
    final AstNode postfixExpr =
        invocationNode.getFirstAncestor(MagikGrammar.POSTFIX_EXPRESSION, MagikGrammar.ATOM);
    if (postfixExpr == null) {
      // Fallback: indent from enclosing statement
      final AstNode statement = invocationNode.getFirstAncestor(MagikGrammar.STATEMENT);
      if (statement != null) {
        final Token statementToken = statement.getToken();
        this.ensureIndentFrom(token, this.getFirstTextTokenOnLine(statementToken));
      }
      return;
    }

    final Token chainStartToken = postfixExpr.getToken();
    final Token firstTextTokenOnChainLine = this.getFirstTextTokenOnLine(chainStartToken);

    // Chained methods are always indented one level from the line where the chain starts
    this.ensureIndentFrom(token, firstTextTokenOnChainLine);
  }

  /**
   * Indents clause constructs (loop, protect, try/when, etc.).
   *
   * <p>Clause keywords (_loop, _protection, _finally, _when) align with their parent clause or the
   * root construct. End keywords (_endloop, _endprotect, _endtry, _endcatch, _endlock) align with
   * the root clause keyword. Body content is indented one level from the clause keyword.
   *
   * @param token The token to indent.
   * @param clauseNode The clause AST node (LOOP, PROTECTION, FINALLY, WHEN, etc.).
   */
  private void indentClauseFromParent(final Token token, final AstNode clauseNode) {
    final Token clauseToken = clauseNode.getToken();
    final Token rootClauseToken = this.findRootClauseToken(clauseNode);

    if (this.tokenIs(
        token,
        MagikKeyword.ENDLOOP,
        MagikKeyword.ENDPROTECT,
        MagikKeyword.ENDTRY,
        MagikKeyword.ENDCATCH,
        MagikKeyword.ENDLOCK)) {
      // End keywords line up with the root clause keyword (e.g., _endloop with _for in _for _over
      // _loop).
      if (token != rootClauseToken) {
        this.ensureIndentLinedUpWith(token, rootClauseToken);
      }
      return;
    }

    if (token == clauseToken) {
      // Clause keyword itself - line up with root clause token.
      if (token != rootClauseToken) {
        this.ensureIndentLinedUpWith(token, rootClauseToken);
      } else {
        // Standalone clause keyword is the root itself - delegate to enclosing context
        // for proper indentation (e.g., inner _loop nested inside outer _loop's body).
        final AstNode parent = clauseNode.getParent();
        if (parent != null) {
          this.indentTokenNode(token, parent);
        }
      }
      return;
    }

    this.ensureIndentFrom(token, clauseToken);
  }

  /**
   * Finds the root clause token by walking up through nested clause constructs.
   *
   * <p>For example, in {@code a << _for _over _loop}, this finds {@code _for} as the root. This
   * ensures that nested loop constructs align properly.
   *
   * @param clauseNode The starting clause node.
   * @return The root clause token for alignment.
   */
  private Token findRootClauseToken(final AstNode clauseNode) {
    AstNode current = clauseNode;
    Token rootToken = current.getToken();

    while (current.getParent() != null) {
      final AstNode parent = current.getParent();
      final IndentStrategy parentStrategy = this.strategyByNodeType.get(parent.getType());
      if (parentStrategy == IndentStrategy.CLAUSE_FROM_PARENT) {
        // Parent is also a clause node, so keep walking up
        current = parent;
        rootToken = current.getToken();
      } else if (parentStrategy == IndentStrategy.BODY_FROM_PARENT_START) {
        // Parent is a body-providing node (e.g., TRY for WHEN), use its token as root
        rootToken = parent.getToken();
        break;
      } else {
        // Parent is not a clause node, stop here
        break;
      }
    }

    // Return the root clause token directly.
    // For `a << _for _over _loop`, this returns `_for`, not `a`.
    return rootToken;
  }

  /**
   * Indents binary expressions respecting operator precedence.
   *
   * <p>Higher-precedence sub-expressions align with their start token. Lower-precedence expressions
   * align with their parent. Special handling exists for logical expressions within IF conditions
   * to preserve user formatting.
   *
   * @param token The token to indent.
   * @param expressionNode The binary expression AST node.
   */
  private void indentExpressionWithPrecedence(final Token token, final AstNode expressionNode) {
    final Token nodeToken = expressionNode.getToken();
    final AstNode parentNode = expressionNode.getParent();

    // First token of expression - check if we should align with parent or indent from construct
    if (token == nodeToken) {
      // If this expression is nested inside a logical expression (AND/OR/XOR), align with the
      // parent's first token instead of indenting from the enclosing construct
      if (parentNode != null && this.isLogicalExpression(parentNode)) {
        // Delegate to parent to get proper alignment
        this.indentTokenNode(token, parentNode);
        return;
      }

      // Otherwise, indent from enclosing construct
      final AstNode enclosingConstruct = this.findEnclosingConstruct(expressionNode);
      if (enclosingConstruct != null && !enclosingConstruct.is(MagikGrammar.MAGIK)) {
        final Token constructToken = enclosingConstruct.getToken();
        final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(constructToken);
        this.ensureIndentFrom(token, firstTextTokenOnLine);
      }
      return;
    }

    // Handle operator precedence for nested expressions
    if (parentNode != null && OPERATOR_PRECEDENCE_NODE_TYPES.contains(parentNode.getType())) {
      final Token parentToken = parentNode.getToken();
      final int precedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(expressionNode.getType());
      final int parentPrecedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(parentNode.getType());

      // If this expression has higher precedence and starts on the same line as parent,
      // align continuation with this expression's start token.
      // Otherwise, align with the parent's left operand.
      if (precedence > parentPrecedence && nodeToken.getLine() == parentToken.getLine()) {
        this.ensureIndentLinedUpWith(token, nodeToken);
      } else {
        this.ensureIndentLinedUpWith(token, parentToken);
      }
      return;
    }

    // For binary expressions, continuation lines should align with the left-hand side (first
    // operand) which is the first token of the expression.
    // In block-based mode when the expression follows an assignment, indent one tab from the
    // assignment line rather than visually aligning.
    if (!this.isVisualAlignment() && this.hasAssignmentOperatorBeforeOnSameLine(nodeToken)) {
      final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(nodeToken);
      this.ensureIndentFrom(token, firstTextTokenOnLine);
    } else {
      this.ensureIndentLinedUpWith(token, nodeToken);
    }
  }

  /**
   * Checks if a node is a logical expression (AND, OR, or XOR).
   *
   * @param node The node to check.
   * @return true if the node is a logical expression.
   */
  private boolean isLogicalExpression(final AstNode node) {
    return node.is(
        MagikGrammar.AND_EXPRESSION, MagikGrammar.OR_EXPRESSION, MagikGrammar.XOR_EXPRESSION);
  }

  /**
   * Checks if a token is an end keyword that should align with its opening keyword.
   *
   * <p>End keywords include: _endmethod, _endproc, _endblock, _endif, _endloop, _endtry, _endcatch,
   * _endlock, _endprotect.
   *
   * @param token The token to check.
   * @return true if the token is an end keyword.
   */
  private boolean isEndKeyword(final Token token) {
    final String value = token.getValue();
    return value.equals("_endmethod")
        || value.equals("_endproc")
        || value.equals("_endblock")
        || value.equals("_endif")
        || value.equals("_endloop")
        || value.equals("_endtry")
        || value.equals("_endcatch")
        || value.equals("_endlock")
        || value.equals("_endprotect");
  }

  /**
   * Checks if a token is a structural clause keyword.
   *
   * <p>Clause keywords are intermediate keywords in constructs that should align with the opening
   * keyword. Examples: _then, _elif, _else in IF constructs; _when in TRY constructs.
   *
   * @param token The token to check.
   * @return true if the token is a clause keyword.
   */
  private boolean isClauseKeyword(final Token token) {
    final String value = token.getValue();
    return value.equals("_then")
        || value.equals("_elif")
        || value.equals("_else")
        || value.equals("_when");
  }

  /** Finds the enclosing construct (method, block, if, loop, etc.) for indentation. */
  private AstNode findEnclosingConstruct(final AstNode node) {
    return node.getFirstAncestor(this.bodyContainingConstructs);
  }

  /**
   * Determines the appropriate parent node for indentation purposes.
   *
   * <p>This method handles special cases where the default ancestor lookup doesn't produce the
   * correct indentation anchor. For example, end keywords need to find their matching start
   * keyword, and certain tokens need to skip past intermediate nodes.
   *
   * @param token The token being indented.
   * @param node The AST node containing the token.
   * @return The AST node to use as the indentation anchor.
   */
  private AstNode getParentNode(final Token token, final AstNode node) {
    if (node == null) {
      throw new IllegalStateException("No parent node available for token: " + token);
    }

    if (node.is(MagikGrammar.MAGIK)) {
      return node;
    }

    // For collection closing braces/parens, use the collection node directly.
    if (this.tokenIs(token, MagikPunctuator.BRACE_R) && node.is(MagikGrammar.SIMPLE_VECTOR)) {
      return node;
    }
    // For TUPLE closing parens, use the TUPLE node directly.
    if (this.tokenIs(token, MagikPunctuator.PAREN_R) && node.is(MagikGrammar.TUPLE)) {
      return node;
    }

    // For method/procedure invocations, use the invocation node directly.
    // This handles chained method calls like .method_one().method_two()
    // But NOT for closing parens - those need to go through argument list handling.
    if (node.is(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION)
        && !this.tokenIs(token, MagikPunctuator.PAREN_R, MagikPunctuator.SQUARE_R)) {
      return node;
    }

    // For SLOT nodes (like .method_name), check if this is part of a method invocation chain.
    // SLOT is used for both slot access (object.slot_name) and as part of method chains.
    // Only treat this as part of an invocation chain if the SLOT is immediately followed by
    // a METHOD_INVOCATION in the AST structure (i.e., .method() not just .slot).
    // IMPORTANT: Only apply this for the DOT token, not for the IDENTIFIER token within SLOT.
    if (node.is(MagikGrammar.SLOT) && token.getValue().equals(".")) {
      final AstNode atomNode = node.getParent();
      if (atomNode != null && atomNode.is(MagikGrammar.ATOM)) {
        // Check if this ATOM has a METHOD_INVOCATION child (which means .method() pattern)
        final AstNode methodInv = atomNode.getFirstChild(MagikGrammar.METHOD_INVOCATION);
        if (methodInv != null) {
          return methodInv;
        }
      }
    }

    // Check if this keyword maps to a specific node type
    final AstNodeType keywordNodeType = this.getKeywordNodeType(token);
    if (keywordNodeType != null && node.is(keywordNodeType)) {
      return node;
    }

    // Handle comma inside arguments/parameters
    if (this.tokenIs(token, MagikPunctuator.COMMA)
        && node.is(MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS)) {
      return node;
    }

    // Handle closing parenthesis/bracket for arguments/parameters
    // Use ARGUMENTS/PARAMETERS node for proper indentation (align with opening paren's line)
    if (this.tokenIs(token, MagikPunctuator.PAREN_R, MagikPunctuator.SQUARE_R)) {
      // Find the wrapper node (_PAREN or _SQUARE) if we're inside one
      // Note: PROCEDURE_INVOCATION is defined as ARGUMENTS_PAREN in the grammar,
      // so they're semantically equivalent but have different node types.
      AstNode wrapperNode = node;
      if (!node.is(ARGUMENT_PARAMETER_WRAPPER_TYPES)) {
        wrapperNode = node.getFirstAncestor(ARGUMENT_PARAMETER_WRAPPER_TYPES);
      }
      if (wrapperNode != null) {
        // For closing parens, find the ARGUMENTS/PARAMETERS node inside the wrapper
        // Use getFirstDescendant since ARGUMENTS may be nested due to grammar structure
        // Note: ARGUMENTS/PARAMETERS may be skipped in the grammar, so the node might not exist
        final AstNode argsOrParams =
            wrapperNode.getFirstDescendant(MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS);
        if (argsOrParams != null) {
          return argsOrParams;
        }
        // ARGUMENTS/PARAMETERS is skipped in grammar - use wrapper node directly for closing parens
        // This ensures proper alignment with the opening paren's line
        return wrapperNode;
      }
      // Also check if ARGUMENTS/PARAMETERS is an ancestor
      final AstNode argsOrParams =
          node.getFirstAncestor(MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS);
      if (argsOrParams != null) {
        return argsOrParams;
      }
    }

    // For HANDLING nodes, use the node itself if it has a strategy.
    // This handles cases like `_default` inside a HANDLING node where
    // getFirstAncestor would skip past HANDLING to find METHOD_DEFINITION.
    // Note: We don't do this for SIMPLE_VECTOR/TUPLE to preserve nested collection behavior.
    if (node.is(MagikGrammar.HANDLING) && this.strategyByNodeType.containsKey(node.getType())) {
      return node;
    }

    AstNode parentNode = node.getFirstAncestor(this.parentNodeTypes);

    // Handle first token of assignment - use the parent of parentNode
    if (parentNode != null
        && parentNode.is(MagikGrammar.ASSIGNMENT_EXPRESSION)
        && parentNode.getToken() == token) {
      return this.getParentNode(token, parentNode);
    }

    // Handle first token of variable definition (_local, _global, etc.)
    // Only skip past the modifier when processing the modifier token itself,
    // not for tokens inside blocks/constructs within the variable definition
    if (node.is(MagikGrammar.VARIABLE_DEFINITION_MODIFIER) && node.getToken() == token) {
      return this.getParentNode(token, parentNode);
    }

    // Find node with a defined strategy
    while (parentNode != null && !this.strategyByNodeType.containsKey(parentNode.getType())) {
      parentNode = parentNode.getParent();
    }

    return parentNode == null ? node : parentNode;
  }

  /**
   * Gets the node type that a keyword token should unwind to.
   *
   * @param token The token to check.
   * @return The node type, or null if token is not a mapped keyword.
   */
  private AstNodeType getKeywordNodeType(final Token token) {
    for (final Map.Entry<MagikKeyword, AstNodeType> entry : KEYWORD_TO_NODE_TYPE.entrySet()) {
      if (this.tokenIs(token, entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Tests if a token matches any of the given Magik keywords.
   *
   * @param token The token to test.
   * @param keywords The keywords to match against.
   * @return True if the token matches any keyword.
   */
  private boolean tokenIs(final Token token, final MagikKeyword... keywords) {
    final String[] keywordValues =
        Stream.of(keywords).map(MagikKeyword::getValue).toArray(String[]::new);
    return AstQuery.tokenIs(token, keywordValues);
  }

  /**
   * Tests if a token matches any of the given Magik punctuators.
   *
   * @param token The token to test.
   * @param punctuators The punctuators to match against.
   * @return True if the token matches any punctuator.
   */
  private boolean tokenIs(final Token token, final MagikPunctuator... punctuators) {
    final String[] punctuatorValues =
        Stream.of(punctuators).map(MagikPunctuator::getValue).toArray(String[]::new);
    return AstQuery.tokenIs(token, punctuatorValues);
  }

  /**
   * Finds the first non-whitespace token on the same line as the given token.
   *
   * <p>This is used to determine the start of a line for alignment purposes, ignoring leading
   * whitespace.
   *
   * @param token The token to start from.
   * @return The first non-whitespace token on the same line.
   */
  private Token getFirstTextTokenOnLine(final Token token) {
    Token currentToken = token;
    Token lastTextToken = token;
    while (currentToken != null) {
      final Token tokenBefore = this.getTokenBeforeOnSameLine(currentToken);
      if (tokenBefore == null) {
        // No more tokens before.
        break;
      }

      final TokenType tokenBeforeType = tokenBefore.getType();
      if (!tokenBeforeType.equals(GenericTokenType.WHITESPACE)) {
        lastTextToken = tokenBefore;
      }

      currentToken = tokenBefore;
    }

    return lastTextToken;
  }

  /**
   * Indent from the reference token.
   *
   * @param token The token to indent.
   * @param referenceToken The token to indent from.
   */
  private void ensureIndentFrom(final Token token, final Token referenceToken) {
    // Try getIndentSize first (works for normal tokens)
    int referenceColumn = this.getIndentSize(referenceToken);

    // If getIndentSize returns 0 but the reference token has leading whitespace,
    // it's likely a cloned token (column=0 but valid indent). Use the whitespace width instead.
    if (referenceColumn == 0) {
      final int leadingWhitespace = this.getLeadingWhitespaceVisualWidth(referenceToken);
      if (leadingWhitespace > 0) {
        referenceColumn = leadingWhitespace;
      }
    }

    final int indentSize = referenceColumn + this.getOptions().getTabSize();

    // If referenceToken has an assignment operator before it on the same line, it's likely in
    // a position like "my_var << _proc(" where we don't want to align the body with _proc.
    // In this case, skip alignment to preserve proper indentation from line start.
    if (this.hasAssignmentOperatorBeforeOnSameLine(referenceToken)) {
      return;
    }

    this.ensureIndent(token, indentSize);
  }

  /**
   * Ensures that the given token is indented to the same column as the reference token.
   *
   * @param token The token to line up.
   * @param referenceToken The token to line up with.
   */
  private void ensureIndentLinedUpWith(final Token token, final Token referenceToken) {
    if (token == referenceToken) {
      throw new IllegalArgumentException("Token and reference token are the same");
    }

    // Try getIndentSize first (works for normal tokens)
    int indentSize = this.getIndentSize(referenceToken);

    // If getIndentSize returns 0 but the reference token has leading whitespace,
    // it's likely a cloned token (column=0 but valid indent). Use the whitespace width instead.
    if (indentSize == 0) {
      final int leadingWhitespace = this.getLeadingWhitespaceVisualWidth(referenceToken);
      if (leadingWhitespace > 0) {
        indentSize = leadingWhitespace;
      }
    }

    // If referenceToken has an assignment operator before it on the same line, it's likely in
    // a position like "my_var << _proc(" where we don't want to align the body with _proc.
    // In this case, skip alignment to preserve proper indentation from line start.
    if (!this.isVisualAlignment() && this.hasAssignmentOperatorBeforeOnSameLine(referenceToken)) {
      return;
    }

    this.ensureIndent(token, indentSize);
  }

  /**
   * Calculates the effective indent size for a token, accounting for tabs and leading whitespace.
   *
   * <p>Tabs are expanded to their configured width. This ensures correct alignment regardless of
   * whether the source uses tabs or spaces.
   *
   * @param referenceToken The token to calculate the indent size from.
   * @return The visual column position.
   */
  private int getIndentSize(final Token referenceToken) {
    // Calculate the indent size based on text before the referenceToken and configured tab size.
    // Determine all the text before the referenceToken on the same line.
    final StringBuilder stringBuilder = new StringBuilder();
    Token tokenBefore = this.getTokenBeforeOnSameLine(referenceToken);
    while (tokenBefore != null) {
      final String tokenBeforeValue = tokenBefore.getOriginalValue();
      stringBuilder.append(tokenBeforeValue);
      tokenBefore = this.getTokenBeforeOnSameLine(tokenBefore);
    }

    // Note: This that assumes tabs are only used at the start of each line.
    final int tabSize = this.getOptions().getTabSize();
    final String untabbedString = " ".repeat(tabSize);
    return stringBuilder.toString().replaceAll("\t", untabbedString).length();
  }

  /**
   * Gets the effective indent size for a reference token, accounting for leading whitespace.
   *
   * <p>This method first tries getIndentSize. If that returns 0 but the reference token has leading
   * whitespace, it uses the whitespace width instead. This handles cloned tokens (column=0 but
   * valid indent).
   *
   * @param referenceToken The token to get the effective indent size for.
   * @return The effective indent size.
   */
  private int getEffectiveIndentSize(final Token referenceToken) {
    int indentSize = this.getIndentSize(referenceToken);

    if (indentSize == 0) {
      final int leadingWhitespace = this.getLeadingWhitespaceVisualWidth(referenceToken);
      if (leadingWhitespace > 0) {
        indentSize = leadingWhitespace;
      }
    }

    return indentSize;
  }

  /**
   * Checks if there is non-whitespace content before the token on the same line.
   *
   * @param token The token to check.
   * @return True if there is non-whitespace content before the token on the same line.
   */
  private boolean hasContentBeforeOnSameLine(final Token token) {
    Token tokenBefore = this.getTokenBeforeOnSameLine(token);
    while (tokenBefore != null) {
      final String value = tokenBefore.getOriginalValue();
      // Check if this token is not just whitespace
      if (!value.matches("^[ \\t]*$")) {
        return true;
      }
      tokenBefore = this.getTokenBeforeOnSameLine(tokenBefore);
    }
    return false;
  }

  /**
   * Checks if there is an assignment operator before the token on the same line.
   *
   * @param token The token to check.
   * @return True if there is an assignment operator (<<) before the token on the same line.
   */
  private boolean hasAssignmentOperatorBeforeOnSameLine(final Token token) {
    Token tokenBefore = this.getTokenBeforeOnSameLine(token);
    while (tokenBefore != null) {
      final String value = tokenBefore.getOriginalValue();
      if ("<<".equals(value)) {
        return true;
      }
      tokenBefore = this.getTokenBeforeOnSameLine(tokenBefore);
    }
    return false;
  }

  /**
   * Ensures the token has the specified indentation.
   *
   * <p>If the token already has the correct indentation (visually), no changes are made. Otherwise,
   * the leading whitespace is added, removed, or modified as needed.
   *
   * <p>For alignment purposes, we preserve existing whitespace if it produces the correct visual
   * column, even if it uses a different mix of tabs and spaces.
   *
   * @param token The token to indent.
   * @param indentSize The desired indent size in columns.
   */
  private void ensureIndent(final Token token, final int indentSize) {
    final String indentString = this.indentString(indentSize);
    final Token whitespaceToken = this.getTokenBeforeOnSameLine(token);

    if (whitespaceToken != null && whitespaceToken.getType().equals(GenericTokenType.WHITESPACE)) {
      this.handleExistingWhitespace(token, whitespaceToken, indentSize, indentString);
    } else {
      this.handleMissingWhitespace(token, indentSize, indentString);
    }
  }

  /**
   * Handles the case where there's existing whitespace before the token.
   *
   * @param token The token to indent.
   * @param whitespaceToken The existing whitespace token.
   * @param indentSize The desired indent size.
   * @param indentString The desired indent string.
   */
  private void handleExistingWhitespace(
      final Token token,
      final Token whitespaceToken,
      final int indentSize,
      final String indentString) {
    final String currentWhitespace = whitespaceToken.getValue();
    final int currentVisualWidth = this.calculateVisualWidth(currentWhitespace);

    if (currentVisualWidth == indentSize) {
      // Visual width is correct, but check if we need to normalize the whitespace format
      // For line-starting indentation, enforce canonical formatting
      if (!currentWhitespace.equals(indentString)) {
        final boolean isLineStartWhitespace = !this.hasContentBeforeOnSameLine(token);
        if (isLineStartWhitespace) {
          this.setTokenOriginalValue(whitespaceToken, indentString);
        }
      }
      // Note: For alignment whitespace after other content, preserve the format
      // even if it uses spaces, as long as visual width is correct.
    } else if (indentSize == 0) {
      // No indent, so remove the whitespace.
      this.removeWhitespaceToken(whitespaceToken);
    } else {
      // Has whitespace, but not the correct indent, so we need to replace it.
      this.setTokenOriginalValue(whitespaceToken, indentString);
    }
  }

  /** Helper to print string with escape sequences visible. */
  /**
   * Handles the case where there's no whitespace before the token.
   *
   * @param token The token to indent.
   * @param indentSize The desired indent size.
   * @param indentString The desired indent string.
   */
  private void handleMissingWhitespace(
      final Token token, final int indentSize, final String indentString) {
    // No whitespace token found in lineTokens. Check if there's leading whitespace
    // in the token's trivia (handles case where trivia token has wrong line number).
    int currentVisualIndent = this.getLeadingWhitespaceVisualWidth(token);

    if (currentVisualIndent == indentSize) {
      // Already has the correct visual indent from trivia or column, nothing to do.
      return;
    }

    // Conservative approach: if the token already has reasonable indentation
    // (multiple of tab size) and we're trying to set it to 0, preserve existing indentation
    final int tabSize = this.getOptions().getTabSize();
    if (indentSize == 0 && currentVisualIndent > 0 && currentVisualIndent % tabSize == 0) {
      return;
    }

    if (indentSize != 0) {
      // No correct indent, so add whitespace.
      this.ensureWhitespaceBefore(token, indentString);
    }
  }

  /**
   * Gets the visual width of leading whitespace from a token's trivia.
   *
   * <p>This method looks through the token's trivia for whitespace that appears after the last
   * newline (EOL) trivia, and calculates its visual width. This handles the case where the
   * whitespace trivia token's line number matches the preceding newline's line rather than the
   * current token's line.
   *
   * @param token The token to check.
   * @return The visual width of the leading whitespace, or 0 if none found.
   */
  private int getLeadingWhitespaceVisualWidth(final Token token) {
    StringBuilder whitespaceAfterNewline = new StringBuilder();
    for (final Trivia trivia : token.getTrivia()) {
      // Each trivia can contain multiple tokens, so iterate through all of them
      for (final Token triviaToken : trivia.getTokens()) {
        final String value = triviaToken.getValue();
        // Check by value since trivia token types may vary
        if (this.containsNewline(value)) {
          // Reset - start accumulating whitespace after this newline
          whitespaceAfterNewline = new StringBuilder();
          // Add any whitespace that follows the newline in this token
          final int lastNewlineIndex = Math.max(value.lastIndexOf('\n'), value.lastIndexOf('\r'));
          if (lastNewlineIndex < value.length() - 1) {
            whitespaceAfterNewline.append(value.substring(lastNewlineIndex + 1));
          }
        } else if (this.isWhitespace(value)) {
          // Accumulate whitespace - might be the leading whitespace after the last newline
          whitespaceAfterNewline.append(value);
        }
      }
    }
    return this.calculateVisualWidth(whitespaceAfterNewline.toString());
  }

  /** Checks if a string contains newline characters. */
  private boolean containsNewline(final String value) {
    return value.contains("\n") || value.contains("\r");
  }

  /** Checks if a string contains only whitespace (no newlines). */
  private boolean isWhitespace(final String value) {
    if (value.isEmpty()) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      if (c != ' ' && c != '\t') {
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates the visual width of a whitespace string, expanding tabs.
   *
   * @param whitespace The whitespace string to measure.
   * @return The visual column width.
   */
  private int calculateVisualWidth(final String whitespace) {
    final int tabSize = this.getOptions().getTabSize();
    int width = 0;
    for (int i = 0; i < whitespace.length(); i++) {
      if (whitespace.charAt(i) == '\t') {
        // Tab advances to next tab stop
        width = ((width / tabSize) + 1) * tabSize;
      } else {
        width++;
      }
    }
    return width;
  }

  /**
   * Get the indent string for a given size.
   *
   * @param indentSize Number of white spaces.
   * @return Indent string.
   */
  private String indentString(final int indentSize) {
    if (indentSize == 0) {
      return "";
    }

    final int tabSize = this.getOptions().getTabSize();
    final String tabText = this.getOptions().isInsertSpaces() ? " ".repeat(tabSize) : "\t";
    final int indent1 = indentSize / tabSize;
    final int indent2 = indentSize % tabSize;
    return tabText.repeat(indent1) + " ".repeat(indent2);
  }
}
