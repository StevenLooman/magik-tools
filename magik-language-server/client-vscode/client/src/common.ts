import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';

export function getJavaExecSafe(): fs.PathLike | null {
	const javaHomeEnv = process.env['JAVA_HOME'];
	const javaHome = vscode.workspace.getConfiguration().get('magik.javaHome', javaHomeEnv);
	if (javaHome == null) {
		return null;
	}

	const javaName = process.platform === "win32" ? 'java.exe' : 'java';
	const javaExec = path.join(javaHome, 'bin', javaName);
	if (!fs.existsSync(javaExec)) {
		return null;
	}

	return javaExec;
}

export function getJavaExec(): fs.PathLike {
	const javaExec = getJavaExecSafe();
	if (javaExec == null) {
		const message = 'Could not locate java executable, either set Java Home setting ("magik.javaHome") or JAVA_HOME environment variable.';
		vscode.window.showWarningMessage(message);
		throw new Error(message);
	}

	return javaExec;
}
