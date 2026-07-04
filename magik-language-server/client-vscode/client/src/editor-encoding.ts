import * as vscode from 'vscode';

import { magikToVscodeEncoding, readDeclaredEncoding } from './encoding';
import { getLogger } from './logging';

const MAGIK_LANGUAGE_ID = 'magik';
const MATCH_SETTING = 'editor.matchDeclaredEncoding';

/**
 * Decode the first line of a byte buffer as Latin-1, hopefully containing the `#% text_encoding`
 * declaration. The `#% text_encoding` declaration is always ASCII, so this reads it regardless
 * of the file's actual encoding.
 * @param bytes Raw file bytes.
 * @returns The first line, without its line terminator.
 */
function firstLineAsLatin1(bytes: Uint8Array): string {
	let end = bytes.length;
	for (let i = 0; i < bytes.length; i++) {
		if (bytes[i] === 0x0a || bytes[i] === 0x0d) {
			end = i;
			break;
		}
	}

	let line = '';
	for (let i = 0; i < end; i++) {
		line += String.fromCharCode(bytes[i]);
	}

	return line;
}

/**
 * If a Magik document declares a `#% text_encoding` that differs from how VSCode
 * decoded it, reopen it with the declared encoding so it displays and saves
 * correctly.
 * @param document The document to align.
 * @param log Channel to trace the decision on.
 */
async function matchDocumentEncoding(
	document: vscode.TextDocument,
	log: vscode.LogOutputChannel,
): Promise<void> {
	if (document.languageId !== MAGIK_LANGUAGE_ID || document.uri.scheme !== 'file') {
		return;
	}

	const name = document.uri.fsPath;

	// A dirty document cannot be re-decoded without losing edits.
	if (document.isDirty) {
		log.debug(`[encoding] ${name}: skipped, document has unsaved changes`);
		return;
	}

	let bytes: Uint8Array;
	try {
		bytes = await vscode.workspace.fs.readFile(document.uri);
	} catch (error) {
		log.warn(`[encoding] ${name}: could not read file: ${String(error)}`);
		return;
	}

	const declared = readDeclaredEncoding(firstLineAsLatin1(bytes));
	if (declared === undefined) {
		log.debug(`[encoding] ${name}: no '#% text_encoding' declaration; leaving encoding as ${document.encoding}`);
		return;
	}

	const encoding = magikToVscodeEncoding(declared);
	if (encoding === undefined) {
		log.warn(`[encoding] ${name}: declared encoding '${declared}' has no VSCode equivalent; leaving as ${document.encoding}`);
		return;
	}

	if (encoding === document.encoding) {
		log.debug(`[encoding] ${name}: already ${encoding} (declared '${declared}')`);
		return;
	}

	log.info(`[encoding] ${name}: declared '${declared}' -> ${encoding}, reopening (was ${document.encoding})`);
	try {
		// Reopening a document that is already open re-decodes it with the given
		// encoding, updating the visible editor in place.
		await vscode.workspace.openTextDocument(document.uri, { encoding });
		log.info(`[encoding] ${name}: reopened with ${encoding}`);
	} catch (error) {
		log.warn(`[encoding] ${name}: reopening with ${encoding} failed: ${String(error)}`);
	}
}

function isEnabled(): boolean {
	return vscode.workspace.getConfiguration('magik').get<boolean>(MATCH_SETTING) ?? true;
}

/**
 * Register the editor-encoding matcher: on open (and for already-open Magik
 * files), align the editor encoding with the file's `#% text_encoding`
 * declaration. Controlled by the `magik.editor.matchDeclaredEncoding` setting.
 * Decisions are traced to the shared "Magik" output channel.
 * @param context Extension context.
 */
export function registerEditorEncodingMatcher(context: vscode.ExtensionContext): void {
	const log = getLogger();
	log.info(`[encoding] matcher registered (magik.${MATCH_SETTING}=${isEnabled()}).`);

	const onOpen = vscode.workspace.onDidOpenTextDocument((document) => {
		if (isEnabled()) {
			void matchDocumentEncoding(document, log);
		}
	});
	context.subscriptions.push(onOpen);

	if (isEnabled()) {
		for (const document of vscode.workspace.textDocuments) {
			void matchDocumentEncoding(document, log);
		}
	}
}
