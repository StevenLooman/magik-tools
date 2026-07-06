import * as assert from 'node:assert';
import {
	determineEncoding,
	encodeMagikSource,
	magikToVscodeEncoding,
	readDeclaredEncoding,
} from './encoding';

const hex = (bytes: Uint8Array): string => Buffer.from(bytes).toString('hex');

describe('determineEncoding', () => {
	it('defaults to UTF-8 when no encoding is declared', () => {
		assert.strictEqual(determineEncoding('write("hi")'), 'utf8');
	});

	it('reads the declared encoding from the first line', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = utf8\nwrite("hi")'), 'utf8');
	});

	it('is case-insensitive and trims surrounding whitespace', () => {
		assert.strictEqual(determineEncoding('#% text_encoding =   UTF8  \n_block'), 'utf8');
	});

	it('accepts code pages beyond Node\'s native set (Latin-2)', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = iso8859_2\n_block'), 'iso8859_2');
	});

	it('accepts ISO 8859-15 (Latin-9)', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = iso8859_15\n_block'), 'iso8859_15');
	});

	it('keeps utf16 as-is (encoded with a BOM downstream)', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = utf16\n_block'), 'utf16');
	});

	it('falls back to the default (UTF-8) for an unrecognised encoding', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = klingon-9\n_block'), 'utf8');
	});

	it('only inspects the first line', () => {
		assert.strictEqual(determineEncoding('_block\n#% text_encoding = iso8859_1'), 'utf8');
	});

	it('uses the supplied default when no encoding is declared', () => {
		assert.strictEqual(determineEncoding('write("hi")', 'iso8859_1'), 'iso8859_1');
	});

	it('lets a declaration override the supplied default', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = utf8\nx', 'iso8859_1'), 'utf8');
	});

	it('falls back to UTF-8 when the supplied default is not a real encoding', () => {
		assert.strictEqual(determineEncoding('write("hi")', 'klingon-9'), 'utf8');
	});

	it('accepts the no-space declaration form', () => {
		assert.strictEqual(determineEncoding('#%text_encoding=iso8859_1\nx'), 'iso8859_1');
	});
});

describe('encodeMagikSource', () => {
	it('encodes undeclared source as UTF-8 (e.g. umlauts as multi-byte sequences)', () => {
		// ö ä ü -> c3 b6 c3 a4 c3 bc in UTF-8.
		const bytes = encodeMagikSource('"öäü"');
		assert.strictEqual(hex(bytes), '22c3b6c3a4c3bc22');
	});

	it('encodes declared UTF-8 as multi-byte sequences', () => {
		const bytes = encodeMagikSource('#% text_encoding = utf8\n"ö"');
		// trailing "ö" -> c3 b6
		assert.ok(hex(bytes).endsWith('22c3b622'), `unexpected bytes: ${hex(bytes)}`);
	});

	it('writes UTF-16 little-endian with a BOM', () => {
		const bytes = encodeMagikSource('#% text_encoding = utf16\nabc');
		assert.strictEqual(bytes[0], 0xff);
		assert.strictEqual(bytes[1], 0xfe);
	});

	it('encodes Latin-2 with its own bytes, not a Latin-1 fallback', () => {
		// ł ą ř ů are unrepresentable in Latin-1 (would become 0x3f '?'),
		// but are b3 b1 f8 f9 in ISO 8859-2.
		const bytes = encodeMagikSource('#% text_encoding = iso8859_2\n"łąřů"');
		assert.ok(hex(bytes).includes('b3b1f8f9'), `unexpected bytes: ${hex(bytes)}`);
		assert.ok(!hex(bytes).includes('3f3f3f3f'), 'Latin-2 text was mangled to "????"');
	});
});

describe('readDeclaredEncoding', () => {
	it('reads the declared encoding', () => {
		assert.strictEqual(readDeclaredEncoding('#% text_encoding = iso8859_1'), 'iso8859_1');
	});

	it('lower-cases and trims the value', () => {
		assert.strictEqual(readDeclaredEncoding('#% text_encoding =   UTF8  '), 'utf8');
	});

	it('returns undefined when there is no declaration', () => {
		assert.strictEqual(readDeclaredEncoding('_package user'), undefined);
	});

	it('accepts the no-space and mixed-spacing forms', () => {
		assert.strictEqual(readDeclaredEncoding('#%text_encoding=utf8'), 'utf8');
		assert.strictEqual(readDeclaredEncoding('#% text_encoding=iso8859_1'), 'iso8859_1');
	});
});

describe('magikToVscodeEncoding', () => {
	it('maps Magik ISO names onto VSCode ids by stripping separators', () => {
		assert.strictEqual(magikToVscodeEncoding('iso8859_1'), 'iso88591');
		assert.strictEqual(magikToVscodeEncoding('iso8859_2'), 'iso88592');
		assert.strictEqual(magikToVscodeEncoding('iso8859_15'), 'iso885915');
	});

	it('passes through matching ids such as utf8', () => {
		assert.strictEqual(magikToVscodeEncoding('utf8'), 'utf8');
	});

	it('applies aliases (latin1 -> iso88591, utf16 -> utf16le)', () => {
		assert.strictEqual(magikToVscodeEncoding('latin1'), 'iso88591');
		assert.strictEqual(magikToVscodeEncoding('utf16'), 'utf16le');
	});

	it('is case- and separator-insensitive', () => {
		assert.strictEqual(magikToVscodeEncoding('ISO-8859-2'), 'iso88592');
	});

	it('returns undefined for unsupported encodings', () => {
		assert.strictEqual(magikToVscodeEncoding('klingon-9'), undefined);
	});
});
