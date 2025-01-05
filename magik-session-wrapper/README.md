# Magik session wrapper

A wrapper around `runalias` which provides a better prompt than the standard Magik prompt.

The standard Magik/CLI prompt is a bit bare bones. On Windows there is a bit of additional functionality such as previous/next command, but on Linux the prompt is minimal. As a result, the prompt is barely usable. This wrapper provides a better experience by providing functionality which is standard in regular REPL prompts.

## Supported functionality

* Parsing of Magik
* Syntax error detection/detection of incomplete statements
* Completion of:
  * Electric Magik templates
  * Magik keywords
* Highlighting
* History
* Navigation using the keyboard:
  * Left/right/up/down arrow keys to move within prompt
  * Up/down to recall history
  * Regular Unix keys:
    * Ctrl-a to move cursor to start of line
    * Ctrl-b to move cursor back one character
    * Ctrl-f to move cursor forward one character
    * Ctrl-r to search history
    * Ctrl-u to cut text from beginning of line to cursor
    * Ctrl-y to paste/"yank"
    * Ctrl-_ to undo action
    * Ctrl-backspace to delete word
    * Alt-enter to insert line
    * ...
  * ANSI colors, for example through [sw5_color_terminal](https://github.com/StevenLooman/sw5_color_terminal)
