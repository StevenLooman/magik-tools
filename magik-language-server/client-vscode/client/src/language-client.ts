import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import * as vscodeLanguageClient from 'vscode-languageclient/node';

import { getJavaExec } from './common';
import { MAGIK_TOOLS_VERSION } from './const';
import { MagikSessionProvider } from './magik-session';


export class MagikLanguageClient implements vscode.Disposable {

	private _context: vscode.ExtensionContext;
	private _client: vscodeLanguageClient.LanguageClient;
	private _magikSessionProvider: MagikSessionProvider | undefined;

	constructor(context: vscode.ExtensionContext) {
		this._context = context;

		this.registerCommands();
	}

	public get magikSessionProvider() {
		return this._magikSessionProvider;
	}

	public set magikSessionProvider(magikSessionProvider: MagikSessionProvider) {
		if (this._magikSessionProvider) {
			throw new Error("Illegal state");
		}

		this._magikSessionProvider = magikSessionProvider;
	}

	public dispose() {
		// Nop.
	}

	public start() {
		const javaExec = getJavaExec();
		if (javaExec == null) {
			vscode.window.showWarningMessage('Could locate java executable, either set Java Home setting ("magik.javaHome") or JAVA_HOME environment variable.');
			return;
		}
		const jar = path.join(__dirname, '..', '..', 'server', 'magik-language-server-' + MAGIK_TOOLS_VERSION + '.jar');
		const javaDebuggerOptions = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,quiet=y,address=5005';

		const serverOptions: vscodeLanguageClient.ServerOptions = {
			run: {
				command: javaExec.toString(),
				args: ['-jar', jar, '--debug'],
				transport: vscodeLanguageClient.TransportKind.stdio
			},
			debug: {
				command: javaExec.toString(),
				args: [javaDebuggerOptions, '-jar', jar, '--debug'],
				transport: vscodeLanguageClient.TransportKind.stdio
			}
		};

		const clientOptions: vscodeLanguageClient.LanguageClientOptions = {
			documentSelector: [
				{ scheme: 'file', language: 'product.def' },
				{ scheme: 'file', language: 'module.def' },
				{ scheme: 'file', language: 'magik' },
			],
			synchronize: {
				fileEvents: [
					// Include all files (and directories) and filter in the language server itself.
					vscode.workspace.createFileSystemWatcher('**/*')
				],
				configurationSection: 'magik',
			}
		};

		this._client = new vscodeLanguageClient.LanguageClient(
			'magik',
			'Magik Language Server',
			serverOptions,
			clientOptions
		);

		this._client.start();
	}

	private registerCommands() {
		const reIndex = vscode.commands.registerCommand('magik.custom.reIndex', () => this.command_custom_re_index());
		this._context.subscriptions.push(reIndex);
	}

	public stop(): Thenable<void> {
		return this._client.stop();
	}

	public sendRequest<R>(request: string): Promise<R> {
		return this._client.sendRequest(request);
	}

	public sendToSession(text: string, sourcePath: fs.PathLike | undefined) {
		this._magikSessionProvider.sendToSession(text, sourcePath);
	}

	//#region: Commands
	private command_custom_re_index() {
		this._client.sendRequest('custom/reIndex');
	}
	//#endregion

}
