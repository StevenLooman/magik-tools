import * as assert from 'node:assert';
import { stripDebugStatements } from './magik-debug';

describe('stripDebugStatements', () => {
	it('activates a #DEBUG-prefixed line by removing the token, keeping indentation', () => {
		assert.strictEqual(stripDebugStatements('\t#DEBUG show(x)'), '\t show(x)');
	});

	it('leaves a line without #DEBUG unchanged', () => {
		const source = '_block\n\tdo_thing()\n_endblock';
		assert.strictEqual(stripDebugStatements(source), source);
	});

	it('strips #DEBUG on every matching line', () => {
		assert.strictEqual(stripDebugStatements('#DEBUG a()\n#DEBUG b()'), ' a()\n b()');
	});

	it('leaves a mid-line (trailing-comment) #DEBUG untouched', () => {
		const source = 'do_thing() #DEBUG note';
		assert.strictEqual(stripDebugStatements(source), source);
	});

	it('does not strip a longer word beginning with #DEBUG', () => {
		const source = '#DEBUGGING is a comment';
		assert.strictEqual(stripDebugStatements(source), source);
	});

	it('is case-sensitive, so #debug stays a comment', () => {
		const source = '#debug show(x)';
		assert.strictEqual(stripDebugStatements(source), source);
	});

	it('strips a bare #DEBUG line on CRLF source without eating the newline', () => {
		assert.strictEqual(stripDebugStatements('a()\r\n#DEBUG\r\nb()'), 'a()\r\n\r\nb()');
	});

	it('preserves the rest of the line after the token', () => {
		assert.strictEqual(stripDebugStatements('    #DEBUG print(!current!)'), '     print(!current!)');
	});

	it('strips a bare #DEBUG at end of input (no trailing newline)', () => {
		assert.strictEqual(stripDebugStatements('a()\n#DEBUG'), 'a()\n');
	});
});
