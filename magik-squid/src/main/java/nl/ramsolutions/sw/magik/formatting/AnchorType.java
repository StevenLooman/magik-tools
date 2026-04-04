package nl.ramsolutions.sw.magik.formatting;

/**
 * Anchor types for indentation rules.
 *
 * <p>Each anchor type defines how to determine the reference column for indentation:
 *
 * <ul>
 *   <li>{@link #PARENT} - Use the parent node's token column
 *   <li>{@link #PARENT_BOL} - Use the beginning of the parent's line (first non-whitespace token)
 *   <li>{@link #FIRST_CHILD} - Align with the first child of the parent node
 *   <li>{@link #CHAIN_START} - Use the start of an invocation chain (for fluent APIs)
 *   <li>{@link #ROOT_CLAUSE} - Find the root clause token (for loop/protect constructs)
 *   <li>{@link #PREV_SIBLING} - Align with the previous sibling node
 * </ul>
 */
public enum AnchorType {
  /**
   * Use the parent node's token column as the reference. The offset is applied relative to this
   * column.
   */
  PARENT,

  /**
   * Use the beginning of the parent's line (first non-whitespace token) as the reference. This is
   * useful for block-style indentation where all elements indent from the line start.
   */
  PARENT_BOL,

  /**
   * Align with the first child of the parent node. Used for expressions where continuation lines
   * should align with the first operand.
   */
  FIRST_CHILD,

  /**
   * Use the start of an invocation chain as the reference. This finds the outermost postfix
   * expression and uses the beginning of that line for fluent API style indentation.
   */
  CHAIN_START,

  /**
   * Find the root clause token by walking up through nested clause constructs (FOR, OVER, WHILE,
   * LOOP, PROTECT, etc.). Used to align _loop with _for, _endloop with _for, etc.
   */
  ROOT_CLAUSE,

  /**
   * Align with the previous sibling node's token. Used as a fallback when no other rule matches.
   */
  PREV_SIBLING
}
