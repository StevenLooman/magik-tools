import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import * as vscodeLanguageClient from 'vscode-languageclient/node';

import { getJavaExec } from './common';
import { MAGIK_TOOLS_VERSION } from './const';
import { MagikSessionProvider } from './magik-session';


export class MagikLanguageClient implements vscode.Disposable {
	private static readonly STOP_TIMEOUT_MS = 5 * 60 * 1000;

	private readonly _context: vscode.ExtensionContext;
	private _client: vscodeLanguageClient.LanguageClient | undefined;
	private _isStarted = false;
	private _outputSessionCounter = 0;
	private _magikSessionProvider: MagikSessionProvider | undefined;

	constructor(context: vscode.ExtensionContext) {
		this._context = context;

		this.registerCommands();
	}

	private createSessionOutputChannel(): vscode.LogOutputChannel {
		this._outputSessionCounter += 1;
		const channelName =
			this._outputSessionCounter === 1
				? 'Magik Language Server'
				: `Magik Language Server #${this._outputSessionCounter}`;
		const outputChannel = vscode.window.createOutputChannel(channelName, { log: true });
		this._context.subscriptions.push(outputChannel);
		return outputChannel;
	}

	public get magikSessionProvider(): MagikSessionProvider | undefined {
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

	public start(notify = true) {
		if (this._isStarted) {
			if (notify) {
				vscode.window.showInformationMessage('Magik Language Server is already running.');
			}
			return;
		}

		const outputChannel = this.createSessionOutputChannel();

		const javaExec = getJavaExec();

		const jar = path.join(__dirname, '..', '..', 'server', 'magik-language-server-' + MAGIK_TOOLS_VERSION + '.jar');
		const javaDebuggerOptions = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,quiet=y,address=127.0.0.1:5005';

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
			outputChannel,
			documentSelector: [
				{ scheme: 'file', language: 'sw-product-def' },
				{ scheme: 'file', language: 'sw-module-def' },
				{ scheme: 'file', language: 'sw-load-list' },
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
		this._isStarted = true;
	}

	private registerCommands() {
		// Used by the server's "Extract to method/proc" code action to position the cursor
		// on the placeholder name and trigger an inline rename.
		const triggerRename = vscode.commands.registerCommand(
			'magik.triggerRename',
			async (uriString: string, lineNumber: number, column: number) => {
				const uri = vscode.Uri.parse(uriString);
				// lineNumber and column are 1-based (as sent by the server).
				const position = new vscode.Position(lineNumber - 1, column - 1);
				const editor = await vscode.window.showTextDocument(uri);
				// Save so the language server re-parses the file and indexes the
				// newly extracted method/proc before the rename request is sent.
				await editor.document.save();
				editor.selection = new vscode.Selection(position, position);
				await vscode.commands.executeCommand('editor.action.rename');
			}
		);
		this._context.subscriptions.push(triggerRename);

		const addProduct = vscode.commands.registerCommand(
			'magik.session.addProduct',
			(productDir: string) => {
				this.sendToSession(`smallworld_product.add_product("${productDir}")\n$`, undefined);
			}
		);
		this._context.subscriptions.push(addProduct);

		const loadModule = vscode.commands.registerCommand(
			'magik.session.loadModule',
			(moduleName: string) => {
				this.sendToSession(`sw_module_manager.load_module(:${moduleName})\n$`, undefined);
			}
		);
		this._context.subscriptions.push(loadModule);

		const startLanguageServer = vscode.commands.registerCommand(
			'magik.startLanguageServer',
			() => this.start(true)
		);
		this._context.subscriptions.push(startLanguageServer);

		const stopLanguageServer = vscode.commands.registerCommand(
			'magik.stopLanguageServer',
			() => this.stop(true)
		);
		this._context.subscriptions.push(stopLanguageServer);

	}

	public stop(notify = true): Thenable<void> {
		if (!this._isStarted || !this._client) {
			if (notify) {
				vscode.window.showInformationMessage('Magik Language Server is not running.');
			}
			this._isStarted = false;
			return Promise.resolve();
		}

		const result = this._client.stop(MagikLanguageClient.STOP_TIMEOUT_MS);
		return result
			.then(() => {
				this._isStarted = false;
			})
			.catch((error) => {
				this._isStarted = false;
				console.warn('Language server stop failed:', error);
			});
	}

	public sendRequest<R>(request: string, params?: unknown): Promise<R> {
		if (!this._client) {
			return Promise.reject(new Error('Language client is not started'));
		}
		return this._client.sendRequest(request, params);
	}

	public sendToSession(text: string, sourcePath: fs.PathLike | undefined) {
		if (!this._magikSessionProvider) {
			throw new Error('MagikSessionProvider is not initialized');
		}
		this._magikSessionProvider.sendToSession(text, sourcePath);
	}

}
