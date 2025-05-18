import * as vscode from "vscode";

export class MagikEditorCommandsProvider implements vscode.Disposable {
	private readonly context: vscode.ExtensionContext;

	constructor(context: vscode.ExtensionContext) {
		this.context = context;

		this.registerCommands();
	}

	dispose() {
		// Nop.
	}

	private registerCommands() {
		const transmitFileCommand = vscode.commands.registerTextEditorCommand(
			"magik.traceStatement",
			() => this.traceStatement()
		);
		this.context.subscriptions.push(transmitFileCommand);
	}

	private traceStatement() {
		const editor = vscode.window.activeTextEditor;
		if (!editor) {
			return;
		}

		const document = editor.document;
		if (document.languageId !== "magik") {
			return;
		}

		const selection = editor.selection;
		const currentLine = selection.start.line;
		const position = new vscode.Position(currentLine, 0);

		const lineText = document.lineAt(currentLine).text;
		const indent = lineText.match(/^\s+/) || "";
		const traceCommand = `write('+++ ${document.lineAt(currentLine).text.trim().split("'").join("', %', '")} +++')`;
		const traceText = `${indent}${traceCommand}\n`;

		editor.edit((editBuilder) => {
			editBuilder.insert(position, traceText);
		});
	}
}
