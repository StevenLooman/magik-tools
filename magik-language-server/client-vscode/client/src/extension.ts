import * as vscode from 'vscode';

import { MagikLanguageClient } from './language-client';
import { MagikAliasTaskProvider } from './alias-task-provider';
import { MagikDebugProvider } from './debug-provider';
import { MagikSessionProvider } from './magik-session';
import { MagikTestProvider } from './test-provider';


let languageClient: MagikLanguageClient | undefined;
let aliasTaskProvider: MagikAliasTaskProvider | undefined;
let magikSessionProvider: MagikSessionProvider | undefined;
let debugProvider: MagikDebugProvider | undefined;
let testProvider: MagikTestProvider | undefined;


//#region Start/stop
export function activate(context: vscode.ExtensionContext) {
	languageClient = new MagikLanguageClient(context);
	aliasTaskProvider = new MagikAliasTaskProvider(context);
	magikSessionProvider = new MagikSessionProvider(context);
	languageClient.magikSessionProvider = magikSessionProvider;
	debugProvider = new MagikDebugProvider(context);
	testProvider = new MagikTestProvider(context, languageClient);

	languageClient.start();
}

export function deactivate(): Thenable<void> | undefined {
	if (testProvider != null) {
		testProvider.dispose();
		testProvider = null;
	}

	if (debugProvider != null) {
		debugProvider.dispose();
		debugProvider = null;
	}

	if (magikSessionProvider != null) {
		magikSessionProvider.dispose();
		magikSessionProvider = null;
	}

	if (aliasTaskProvider) {
		aliasTaskProvider.dispose();
		aliasTaskProvider = null;
	}

	let thenable: Thenable<void> | undefined;
	if (languageClient) {
		thenable = languageClient.stop();

		languageClient.dispose();
		languageClient = null;
	}

	return thenable
}
//#endregion
