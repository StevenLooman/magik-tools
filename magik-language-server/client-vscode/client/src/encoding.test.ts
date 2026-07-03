import * as assert from 'node:assert';
import { determineEncoding, encodeMagikSource } from './encoding';

const hex = (bytes: Uint8Array): string => Buffer.from(bytes).toString('hex');

describe('determineEncoding', () => {
	it('defaults to ISO 8859-1 when no encoding is declared', () => {
		assert.strictEqual(determineEncoding('write("hi")'), 'iso8859_1');
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

	it('falls back to ISO 8859-1 for an unrecognised encoding', () => {
		assert.strictEqual(determineEncoding('#% text_encoding = klingon-9\n_block'), 'iso8859_1');
	});

	it('only inspects the first line', () => {
		assert.strictEqual(determineEncoding('_block\n#% text_encoding = utf8'), 'iso8859_1');
	});
});

describe('encodeMagikSource', () => {
	it('encodes undeclared source as ISO 8859-1 (e.g. umlauts as single bytes)', () => {
		// ö ä ü -> f6 e4 fc in Latin-1.
		const bytes = encodeMagikSource('"öäü"');
		assert.strictEqual(hex(bytes), '22f6e4fc22');
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
