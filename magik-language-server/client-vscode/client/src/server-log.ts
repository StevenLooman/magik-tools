/**
 * Levels the Magik language server logs with, as written by `java.util.logging`.
 */
export type ServerLogLevel =
	| 'SEVERE'
	| 'WARNING'
	| 'INFO'
	| 'CONFIG'
	| 'FINE'
	| 'FINER'
	| 'FINEST';

/**
 * Pattern for a log line as written by the language server when started with
 * `--debug`. Kept in sync with `java.util.logging.SimpleFormatter.format` in
 * `magik-language-server/src/main/resources/debug-logging.properties`:
 * `%1$tF %1$tT %4$-7s %3$s : %5$s %6$s%n`.
 */
const SERVER_LOG_LINE =
	/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} +(SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST) /;

/**
 * Read the level a language server log line was logged with.
 * @param line Line as written by the language server to stderr.
 * @returns The level, or `undefined` when the line is not a formatted log line.
 */
export function readServerLogLevel(line: string): ServerLogLevel | undefined {
	const match = SERVER_LOG_LINE.exec(line);
	if (match === null) {
		return undefined;
	}

	return match[1] as ServerLogLevel;
}
