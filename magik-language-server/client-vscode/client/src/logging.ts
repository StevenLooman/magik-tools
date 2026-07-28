import * as vscode from 'vscode';

import { ServerLogLevel, readServerLogLevel } from './server-log';

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

/**
 * Log output channel that logs the language server's own `SEVERE` and `WARNING`
 * lines as such, instead of logging everything the server writes as an error.
 */
class ServerLogOutputChannel implements vscode.LogOutputChannel {
	private readonly _channel: vscode.LogOutputChannel;

	constructor(channel: vscode.LogOutputChannel) {
		this._channel = channel;
	}

	public get name(): string {
		return this._channel.name;
	}

	public get logLevel(): vscode.LogLevel {
		return this._channel.logLevel;
	}

	public get onDidChangeLogLevel(): vscode.Event<vscode.LogLevel> {
		return this._channel.onDidChangeLogLevel;
	}

	public append(value: string): void {
		this._channel.append(value);
	}

	public appendLine(value: string): void {
		this._channel.appendLine(value);
	}

	public replace(value: string): void {
		this._channel.replace(value);
	}

	public clear(): void {
		this._channel.clear();
	}

	public show(column?: vscode.ViewColumn | boolean, preserveFocus?: boolean): void {
		// The column is ignored by VSCode for output channels, hence the
		// deprecated overload taking one is not forwarded.
		this._channel.show(typeof column === 'boolean' ? column : preserveFocus);
	}

	public hide(): void {
		this._channel.hide();
	}

	public dispose(): void {
		this._channel.dispose();
	}

	public trace(message: string, ...args: unknown[]): void {
		this._channel.trace(message, ...args);
	}

	public debug(message: string, ...args: unknown[]): void {
		this._channel.debug(message, ...args);
	}

	public info(message: string, ...args: unknown[]): void {
		this._channel.info(message, ...args);
	}

	public warn(message: string, ...args: unknown[]): void {
		this._channel.warn(message, ...args);
	}

	public error(error: string | Error, ...args: unknown[]): void {
		if (typeof error === 'string') {
			const level = readServerLogLevel(error);
			if (level !== undefined) {
				this.logWithLevel(level, error, ...args);
				return;
			}
		}

		this._channel.error(error, ...args);
	}

	private logWithLevel(level: ServerLogLevel, message: string, ...args: unknown[]): void {
		switch (level) {
			case 'SEVERE':
				this._channel.error(message, ...args);
				break;
			case 'WARNING':
				this._channel.warn(message, ...args);
				break;
			// The finer levels are logged as information, and not as debug or trace, to
			// keep them visible at the default log level of a channel.
			case 'INFO':
			case 'CONFIG':
			case 'FINE':
			case 'FINER':
			case 'FINEST':
			default:
				this._channel.info(message, ...args);
				break;
		}
	}
}

/**
 * Create a log output channel for a language server session.
 * @param name Name of the channel.
 * @returns The log output channel.
 */
export function createServerLogOutputChannel(name: string): vscode.LogOutputChannel {
	const channel = vscode.window.createOutputChannel(name, { log: true });
	return new ServerLogOutputChannel(channel);
}
