import * as path from 'path';
import * as vscode from 'vscode';

import { getJavaExec } from './common';


export class MagikDebugProvider implements vscode.Disposable {

	private context: vscode.ExtensionContext;

	constructor(context: vscode.ExtensionContext) {
		this.context = context;

		const factory = new DebugAdapterExecutableFactory();
		const subscription = vscode.debug.registerDebugAdapterDescriptorFactory('magik', factory);
		this.context.subscriptions.push(subscription);
	}

	dispose() {
		// Nop.
	}

}


class DebugAdapterExecutableFactory implements vscode.DebugAdapterDescriptorFactory {

	createDebugAdapterDescriptor(_session: vscode.DebugSession, _executable: vscode.DebugAdapterExecutable | undefined): vscode.ProviderResult<vscode.DebugAdapterDescriptor> {
		const javaExec = getJavaExec();
		if (javaExec == null) {
			vscode.window.showWarningMessage('Could locate java executable, either set Java Home setting ("magik.javaHome") or JAVA_HOME environment variable.');
			return;
		}
		const jar = path.join(__dirname, '..', '..', 'server', 'magik-debug-adapter-0.5.5-SNAPSHOT.jar');
		const javaDebuggerOptions = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,quiet=y,address=5006';

		const command = javaExec.toString();
		const args = [javaDebuggerOptions, '-jar', jar, '--debug'];
		const options = {};
		return new vscode.DebugAdapterExecutable(command, args, options);
	}

}
