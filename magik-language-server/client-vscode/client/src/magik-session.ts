import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as vscode from 'vscode';
import { MagikAliasTaskProvider } from './alias-task-provider';


export class MagikSessionProvider implements vscode.Disposable {

	private context: vscode.ExtensionContext;
	private currentSession: MagikSession | undefined;

	constructor(context: vscode.ExtensionContext) {
		this.context = context;

		this.registerCommands();
		this.registerWindowHandlers();
	}

	dispose() {
		// Nop.
	}

	private registerCommands() {
		const transmitFileCommand = vscode.commands.registerTextEditorCommand('magik.transmitFile', () => this.command_transmit_file());
		this.context.subscriptions.push(transmitFileCommand);
		const transmitMethodCommand = vscode.commands.registerTextEditorCommand('magik.transmitCurrentRegion', () => this.command_transmit_current_region());
		this.context.subscriptions.push(transmitMethodCommand);

		const transmitLoadListCommand = vscode.commands.registerTextEditorCommand('magik.transmitLoadList', () => this.command_transmit_load_list());
		this.context.subscriptions.push(transmitLoadListCommand);

		const transmitProductDefCommand = vscode.commands.registerTextEditorCommand('magik.transmitProductDef', () => this.command_transmit_product_def());
		this.context.subscriptions.push(transmitProductDefCommand);

		const transmitModuleDefCommand = vscode.commands.registerTextEditorCommand('magik.transmitModuleDef', () => this.command_transmit_module_def());
		this.context.subscriptions.push(transmitModuleDefCommand);
	}

	private registerWindowHandlers() {
		vscode.window.onDidOpenTerminal((terminal: vscode.Terminal) => {
			const creationOptions = terminal.creationOptions;

			// Check if creationOptions has shellArgs and handle it appropriately
			const shellArgs = (creationOptions as vscode.TerminalOptions).shellArgs;

			if ((creationOptions.name ?? '').startsWith(MagikAliasTaskProvider.AliasType) ||
				(Array.isArray(shellArgs) && shellArgs.some(arg => arg.includes("runalias.exe "))) ||
				(Array.isArray(shellArgs) && shellArgs.some(arg => arg.includes("runalias ")))
			) {
				this.currentSession = new MagikSession(terminal);
			}
		});

		vscode.window.onDidCloseTerminal((terminal: vscode.Terminal) => {
			if (this.currentSession != null && this.currentSession.terminal == terminal) {
				this.currentSession.dispose();
				this.currentSession = null;
			}
		});
	}

	private command_transmit_file() {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.transmitMagik(vscode.window.activeTextEditor);
	}

	private command_transmit_current_region() {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.transmitEditorRegion(vscode.window.activeTextEditor);
	}

	private command_transmit_load_list() {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.transmitLoadList(vscode.window.activeTextEditor);
	}

	private command_transmit_product_def() {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.transmitProductDef(vscode.window.activeTextEditor);
	}

	private command_transmit_module_def() {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.transmitModuleDef(vscode.window.activeTextEditor);
	}

	public sendToSession(text: string, sourcePath: fs.PathLike | undefined) {
		if (this.currentSession == null) {
			vscode.window.showErrorMessage("No active Smallworld session.");
			return;
		}

		this.currentSession.sendToSession(text, sourcePath);
	}

}


class MagikSession implements vscode.Disposable {

	private _workdir: fs.PathLike;
	private _terminal: vscode.Terminal;

	constructor(terminal: vscode.Terminal) {
		const dir = path.join(os.tmpdir(), "vscode-magik-");
		this._workdir = fs.mkdtempSync(dir);
		this._terminal = terminal;
	}

	public get terminal() : vscode.Terminal {
		return this._terminal;
	}

	public dispose() {
		if (this._workdir !== null) {
			fs.rmSync(this._workdir, {recursive: true});
			this._workdir = null;
		}
	}

	public sendToSession(text: string, sourcePath: fs.PathLike | undefined) {
		// Save contents to temp file.
		const tempPath = this.getTempFile();
		fs.writeFileSync(tempPath, text);

		// Clear current input in session.
		if (process.platform === "win32") {
			// this.terminal.sendText(`\u001B`, false);  // ESC
			// this.terminal.sendText(`\u000D`, false);  // \r
		} else {
			this._terminal.sendText(`\u0015`, false);  // NAK / ^U
		}

		// Send load_file(..) to active session.
		const sourcePathParam = sourcePath ? `"${sourcePath}"` : "_unset";
		const magik = `_protect sw:load_file("${tempPath}", _unset, ${sourcePathParam}) _protection sw:system.unlink("${tempPath}", _true, _true) _endprotect\n$`;
		this._terminal.sendText(magik);
	}

	/**
	 * Transmit current - at cursor - editor region to session.
	 * @param editor Editor to transmit.
	 */
	public transmitEditorRegion(editor: vscode.TextEditor) {
		// Save file in editor.
		const doc = editor.document;
		if (doc.uri.scheme != 'untitled') {
			doc.save();
		}

		// Get cursor position and determine range to transmit.
		const position = editor.selection.active;
		const selection = this.expandTransmittableSelection(doc, position);
		editor.selection = selection;

		// Build prefix, with package specifications.
		const prefix = this.buildPrefix(doc, selection.anchor);

		// Transmit to session.
		const text = prefix + doc.getText(selection);
		const docPath = doc.uri.fsPath;
		this.sendToSession(text, docPath);

		this.showSession();
	}

	/**
	 * Transmit editor to session.
	 * @param editor Editor to transmit.
	 */
	 public transmitMagik(editor: vscode.TextEditor) {
		// Save file in editor.
		const doc = editor.document;
		if (doc.uri.scheme != 'untitled') {
			doc.save();
		}

		// Get active file.
		const text = doc.getText();
		const docPath = doc.uri.fsPath;

		this.sendToSession(text, docPath);

		this.showSession();
	}

	public transmitLoadList(editor: vscode.TextEditor) {
		// Save file in editor.
		const doc = editor.document;
		if (doc.uri.scheme != 'untitled') {
			doc.save();
		}

		// Get active file.
		const docPath = doc.uri.fsPath;
		const docPathParent = path.dirname(docPath);

		const text = `sw:load_file_list("${docPathParent}")\n$`;
		this.sendToSession(text, undefined);

		this.showSession();
	}

	public transmitProductDef(editor: vscode.TextEditor) {
		// Save file in editor.
		const doc = editor.document;
		if (doc.uri.scheme != 'untitled') {
			doc.save();
		}

		// Get active file.
		const docPath = doc.uri.fsPath;
		const docPathParent = path.dirname(docPath);

		// Load/reinitialise product in active session.
		const text = `_block
	_local product << sw:sw_product.new_from_def("${docPathParent}")
	_local registered_product << sw:smallworld_product.product(product.name)
	_if registered_product _isnt _unset
	_then
		registered_product.reinitialize()
	_else
		sw:smallworld_product.add_product("${docPathParent}")
	_endif
_endblock
$`;
		this.sendToSession(text, undefined);

		this.showSession();
	}

	public transmitModuleDef(editor: vscode.TextEditor) {
		// Save file in editor.
		const doc = editor.document;
		if (doc.uri.scheme != 'untitled') {
			doc.save();
		}

		// Get active file.
		const docPath = doc.uri.fsPath;
		const docPathParent = path.dirname(docPath);

		// (Re)load module in active session.
		const text = `_try
	_local module << sw:sw_module.new_from_def("${docPath}", "${docPathParent}")
	sw:sw_module_manager.load_module(module.name, module.version, :force_reload?, _true)
_when sw_module_no_such_module
	sw:sw_module_manager.load_standalone_module_definition("${docPath}")
_endtry
$`;
		this.sendToSession(text, undefined);

		this.showSession();
	}

	public showSession() {
		this._terminal.show();
	}

	private getTempFile(): fs.PathLike {
		let tempPath: fs.PathLike;
		let index = 0;
		do {
			tempPath = path.join(this._workdir.toString(), `tmp${index}.magik`);
			index += 1;
		} while (fs.existsSync(tempPath));
		return tempPath;
	}

	private buildPrefix(doc: vscode.TextDocument, position: vscode.Position) {
		const prefixEndLine = position.line > 0 ? position.line - 1 : 0;
		const anchor = new vscode.Position(0, 0);
		const active = new vscode.Position(prefixEndLine, 0);
		const prefixRange = new vscode.Selection(anchor, active);
		let prefix = "";
		if (!prefixRange.isEmpty) {
			const prefixText = doc.getText(prefixRange);
			for (const line of prefixText.split(/\r?\n/)) {
				if (line.match(/^\s*_package\s+\w+\s*/)) {
					prefix += line + "\n";
				} else {
					prefix += "\n";
				}
			}
		}

		return prefix;
	}

	private expandTransmittableSelection(doc: vscode.TextDocument, position: vscode.Position) {
		let startPosition = position;
		while (startPosition.line > 0) {
			const line = doc.lineAt(startPosition.line);
			if (line.text === "$") {
				// No need to include the "$".
				startPosition = new vscode.Position(startPosition.line + 1, 0);
				break;
			}

			startPosition = new vscode.Position(startPosition.line - 1, 0);
		}

		let endPosition = position;
		while (endPosition.line < doc.lineCount) {
			const line = doc.lineAt(endPosition.line);
			if (line.text === "$") {
				break;
			}

			endPosition = new vscode.Position(endPosition.line + 1, 0);
		}

		if (endPosition.line < doc.lineCount) {
			// Do include the "$".
			endPosition = new vscode.Position(endPosition.line + 1, 0);
		}

		return new vscode.Selection(startPosition, endPosition);
	}

}
