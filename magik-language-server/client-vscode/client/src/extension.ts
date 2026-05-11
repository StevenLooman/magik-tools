import * as vscode from 'vscode';

import { MagikLanguageClient } from './language-client';
import { MagikAliasTaskProvider } from './alias-task-provider';
import { MagikDebugProvider } from './debug-provider';
import { MagikSessionProvider } from './magik-session';
import { MagikTestProvider } from './test-provider';
import { MagikEditorCommandsProvider } from './editor-commands-provider';


let languageClient: MagikLanguageClient | undefined;
let aliasTaskProvider: MagikAliasTaskProvider | undefined;
let magikSessionProvider: MagikSessionProvider | undefined;
let debugProvider: MagikDebugProvider | undefined;
let testProvider: MagikTestProvider | undefined;
let editorCommandsProvider: MagikEditorCommandsProvider | undefined;


//#region Start/stop
export function activate(context: vscode.ExtensionContext) {
	languageClient = new MagikLanguageClient(context);
	aliasTaskProvider = new MagikAliasTaskProvider(context);
	magikSessionProvider = new MagikSessionProvider(context);
	languageClient.magikSessionProvider = magikSessionProvider;
	debugProvider = new MagikDebugProvider(context);
	testProvider = new MagikTestProvider(context, languageClient);
	editorCommandsProvider = new MagikEditorCommandsProvider(context);

	languageClient.start();
}

export function deactivate(): Thenable<void> | undefined {
	if (editorCommandsProvider != null) {
		editorCommandsProvider.dispose();
		editorCommandsProvider = undefined;
	}

	if (testProvider != null) {
		testProvider.dispose();
		testProvider = undefined;
	}

	if (debugProvider != null) {
		debugProvider.dispose();
		debugProvider = undefined;
	}

	if (magikSessionProvider != null) {
		magikSessionProvider.dispose();
		magikSessionProvider = undefined;
	}

	if (aliasTaskProvider) {
		aliasTaskProvider.dispose();
		aliasTaskProvider = undefined;
	}

	let thenable: Thenable<void> | undefined;
	if (languageClient) {
		thenable = languageClient.stop();

		languageClient.dispose();
		languageClient = undefined;
	}

	return thenable
}
//#endregion
