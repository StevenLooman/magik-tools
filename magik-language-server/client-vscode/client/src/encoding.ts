import * as iconv from 'iconv-lite';

/** Prefix of Magik's source-encoding declaration, e.g. `#% text_encoding = utf8`. */
export const ENCODING_LINE = "#% text_encoding =";

/** Smallworld's default source encoding, assumed when none is declared. */
const DEFAULT_ENCODING = "iso8859_1";

/**
 * Determine the encoding to write the temp file with, mirroring Magik's
 * convention: the first line may declare `#% text_encoding = <encoding>`,
 * otherwise ISO 8859-1 is assumed (Smallworld's default source encoding).
 *
 * The label is consumed by `iconv-lite`, which covers the full code-page range
 * (e.g. `iso8859_2`/Latin-2 and other neighbouring code pages). A bare `utf16`
 * is written little-endian with a BOM. Names it does not recognise fall back to
 * ISO 8859-1, matching Smallworld's default.
 * @param text Source text to be transmitted.
 * @returns iconv-lite encoding label.
 */
export function determineEncoding(text: string): string {
	const firstLine = text.split(/\r?\n/, 1)[0] ?? "";
	let encoding = DEFAULT_ENCODING;
	if (firstLine.startsWith(ENCODING_LINE)) {
		encoding = firstLine.substring(ENCODING_LINE.length).trim().toLowerCase();
	}

	return iconv.encodingExists(encoding) ? encoding : DEFAULT_ENCODING;
}

/**
 * Encode Magik source into the bytes to write to the session temp file, using
 * the encoding the source declares (see {@link determineEncoding}). The session's
 * `load_file()` then decodes the temp file exactly as it would the original.
 * @param text Source text to be transmitted.
 * @returns Encoded bytes.
 */
export function encodeMagikSource(text: string): Uint8Array {
	return new Uint8Array(iconv.encode(text, determineEncoding(text)));
}
