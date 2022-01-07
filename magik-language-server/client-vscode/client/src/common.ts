import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';

export function getJavaExec(): fs.PathLike {
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
