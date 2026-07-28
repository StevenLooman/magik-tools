import * as assert from 'node:assert';

import { readServerLogLevel } from './server-log';

describe('readServerLogLevel', () => {
	it('reads the level from a log line', () => {
		const line =
			'2026-07-28 11:20:47 FINE    nl.ramsolutions.sw.magik.languageserver.MagikTextDocumentService : didOpen, uri: file:///a.magik ';
		assert.strictEqual(readServerLogLevel(line), 'FINE');
	});

	it('reads a level that is not padded', () => {
		const line =
			'2026-07-28 11:20:47 WARNING nl.ramsolutions.sw.magik.languageserver.MagikLanguageServer : Something ';
		assert.strictEqual(readServerLogLevel(line), 'WARNING');
	});

	it('reads every level the server logs with', () => {
		const levels = ['SEVERE', 'WARNING', 'INFO', 'CONFIG', 'FINE', 'FINER', 'FINEST'];
		for (const level of levels) {
			const line = `2026-07-28 11:20:47 ${level.padEnd(7)} some.Logger : message `;
			assert.strictEqual(readServerLogLevel(line), level);
		}
	});

	it('ignores a line without a timestamp', () => {
		assert.strictEqual(readServerLogLevel('FINE some.Logger : message'), undefined);
	});

	it('ignores the non-debug log format, which carries no level', () => {
		assert.strictEqual(readServerLogLevel('PID: 284683'), undefined);
		assert.strictEqual(readServerLogLevel('Version: 0.13.0-SNAPSHOT'), undefined);
	});

	it('ignores an unknown level', () => {
		assert.strictEqual(readServerLogLevel('2026-07-28 11:20:47 TRACE   some.Logger : message'), undefined);
	});

	it('ignores a stack trace continuation line', () => {
		assert.strictEqual(readServerLogLevel('\tat java.base/java.lang.Thread.run(Thread.java:1583)'), undefined);
	});

	it('ignores a level that is not at the start of the line', () => {
		assert.strictEqual(
			readServerLogLevel('prefix 2026-07-28 11:20:47 FINE    some.Logger : message'),
			undefined
		);
	});
});
