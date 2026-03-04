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

import nl.ramsolutions.sw.TokenTriviaEditor;

/** Null indent walker. */
public class NullIndentWalker extends FormattingWalker {

  public static final String STRATEGY_NAME = "null";

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @param tokenEditor Token trivia editor.
   */
  NullIndentWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }
}
