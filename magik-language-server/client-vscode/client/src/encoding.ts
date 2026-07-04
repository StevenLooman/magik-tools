import * as iconv from 'iconv-lite';

/** Prefix of Magik's source-encoding declaration, e.g. `#% text_encoding = utf8`. */
export const ENCODING_LINE = "#% text_encoding =";

/**
 * Default encoding used when a file declares none. Follows VSCode's default
 * `files.encoding` (UTF-8) — i.e. the encoding the editor writes files with —
 * so the temp file matches how the source was created.
 */
const DEFAULT_ENCODING = "utf8";

/**
 * Determine the encoding to write the temp file with, mirroring Magik's
 * convention: the first line may declare `#% text_encoding = <encoding>`,
 * otherwise `defaultEncoding` is used (the session's default encoding, which
 * should match its locale).
 *
 * The label is consumed by `iconv-lite`, which covers the full code-page range
 * (e.g. `iso8859_2`/Latin-2 and other neighbouring code pages). A bare `utf16`
 * is written little-endian with a BOM. Names it does not recognise fall back to
 * the default encoding.
 * @param text Source text to be transmitted.
 * @param defaultEncoding Encoding to assume when none is declared; falls back to
 *   {@link DEFAULT_ENCODING} when it is not a recognised encoding.
 * @returns iconv-lite encoding label.
 */
export function determineEncoding(text: string, defaultEncoding: string = DEFAULT_ENCODING): string {
	const fallback = iconv.encodingExists(defaultEncoding) ? defaultEncoding : DEFAULT_ENCODING;
	const firstLine = text.split(/\r?\n/, 1)[0] ?? "";
	let encoding = fallback;
	if (firstLine.startsWith(ENCODING_LINE)) {
		encoding = firstLine.substring(ENCODING_LINE.length).trim().toLowerCase();
	}

	return iconv.encodingExists(encoding) ? encoding : fallback;
}

/**
 * Encode Magik source into the bytes to write to the session temp file, using
 * the encoding the source declares (see {@link determineEncoding}). The session's
 * `load_file()` then decodes the temp file exactly as it would the original.
 * @param text Source text to be transmitted.
 * @param defaultEncoding Encoding to assume when none is declared.
 * @returns Encoded bytes.
 */
export function encodeMagikSource(text: string, defaultEncoding: string = DEFAULT_ENCODING): Uint8Array {
	return new Uint8Array(iconv.encode(text, determineEncoding(text, defaultEncoding)));
}

/** Valid VSCode encoding ids (see `TextDocument.encoding`). */
const VSCODE_ENCODINGS = new Set<string>([
	"utf8", "utf8bom", "utf16le", "utf16be", "windows1252", "iso88591", "iso88593",
	"iso885915", "macroman", "cp437", "windows1256", "iso88596", "windows1257",
	"iso88594", "iso885914", "windows1250", "iso88592", "cp852", "windows1251",
	"cp866", "cp1125", "iso88595", "koi8r", "koi8u", "iso885913", "windows1253",
	"iso88597", "windows1255", "iso88598", "iso885910", "iso885916", "windows1254",
	"iso88599", "windows1258", "gbk", "gb18030", "cp950", "big5hkscs", "shiftjis",
	"eucjp", "euckr", "windows874", "iso885911", "koi8ru", "koi8t", "gb2312",
	"cp865", "cp850", "cp857",
]);

/** Aliases for Magik/iconv labels that do not normalise onto a VSCode encoding id. */
const VSCODE_ENCODING_ALIASES: Record<string, string> = {
	latin1: "iso88591",
	utf16: "utf16le",
};

/**
 * Read the encoding declared on the first line of a Magik file (the
 * `#% text_encoding = ...` comment), or `undefined` when it is absent.
 * @param firstLine First line of the source, decoded as ASCII/Latin-1.
 * @returns The declared encoding label, lower-cased, or `undefined`.
 */
export function readDeclaredEncoding(firstLine: string): string | undefined {
	if (!firstLine.startsWith(ENCODING_LINE)) {
		return undefined;
	}

	return firstLine.substring(ENCODING_LINE.length).trim().toLowerCase();
}

/**
 * Map a Magik `#% text_encoding` label to a VSCode encoding id, or `undefined`
 * when there is no supported equivalent. Magik uses names such as `iso8859_1`
 * that map onto VSCode's `iso88591` once separators are removed.
 * @param label The declared Magik encoding label.
 * @returns A VSCode encoding id, or `undefined`.
 */
export function magikToVscodeEncoding(label: string): string | undefined {
	const normalized = label.trim().toLowerCase().replace(/[-_\s]/g, "");
	if (normalized in VSCODE_ENCODING_ALIASES) {
		return VSCODE_ENCODING_ALIASES[normalized];
	}

	return VSCODE_ENCODINGS.has(normalized) ? normalized : undefined;
}
