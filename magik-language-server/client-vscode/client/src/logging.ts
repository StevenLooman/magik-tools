import * as vscode from 'vscode';

let logger: vscode.LogOutputChannel | undefined;

/**
 * The extension's channel ("Magik").
 * @returns The shared log output channel.
 */
export function getLogger(): vscode.LogOutputChannel {
	if (logger === undefined) {
		logger = vscode.window.createOutputChannel('Magik', { log: true });
	}

	return logger;
}

/**
 * Register the shared logger.
 * @param context Extension context.
 * @returns The shared log output channel.
 */
export function registerLogger(context: vscode.ExtensionContext): vscode.LogOutputChannel {
	const log = getLogger();
	context.subscriptions.push(log);
	return log;
}
