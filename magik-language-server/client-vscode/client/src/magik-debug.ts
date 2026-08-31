// Line-leading `#DEBUG` token (a line's first non-whitespace); `(?=\s|$)` keeps `#DEBUGGING` a comment and `\r` handles CRLF.
const DEBUG_STATEMENT = /^([ \t]*)#DEBUG(?=\s|$)/gm;

// Strip the line-leading `#DEBUG` marker so marked lines compile, keeping indentation and the rest of each line intact.
export function stripDebugStatements(text: string): string {
	return text.replace(DEBUG_STATEMENT, '$1');
}
