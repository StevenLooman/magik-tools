import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';

export class MagikAliasTaskProvider implements vscode.TaskProvider, vscode.Disposable {

	public static readonly AliasType: string = 'run_alias';

	private context: vscode.ExtensionContext;
	private provider: vscode.Disposable;
	private promise: Thenable<vscode.Task[]> | undefined = undefined;
	private fileWatcher: vscode.FileSystemWatcher = undefined;

	constructor(context: vscode.ExtensionContext) {
		this.context = context;
		this.provider = vscode.tasks.registerTaskProvider(MagikAliasTaskProvider.AliasType, this);
	}

	dispose() {
		this.provider.dispose();
	}

	public provideTasks(): vscode.ProviderResult<vscode.Task[]> {
		let aliasesPath = getAliasesPath();
		if (!aliasesPath) {
			return [];
		}

		// Ensure something is watching for changes.
		if (!this.fileWatcher) {
			this.fileWatcher = vscode.workspace.createFileSystemWatcher(aliasesPath.toString());
			this.fileWatcher.onDidChange(() => this.promise = undefined);
			this.fileWatcher.onDidCreate(() => this.promise = undefined);
			this.fileWatcher.onDidDelete(() => this.promise = undefined);
		}

		// Ensure we have a promise.
		if (this.promise == undefined) {
			this.promise = getAliasesTasks(aliasesPath);
		}

		return this.promise;
	}

	public resolveTask(task: vscode.Task): vscode.Task | undefined {
		const entry = task.definition.entry;
		if (entry) {
			const definition: AliasTaskDefinition = <any>task.definition;
			const runAliasPath = getRunAliasPath();
			const aliasesPath = getAliasesPath();
			const entryName = definition.entry;
			const environmentFile = getEnvironmentPath();
			const additionalArgs = definition.args;
			const commandLine = getStartAliasCommand(runAliasPath, aliasesPath, entryName, environmentFile, additionalArgs);
			return new vscode.Task(
				definition,
				task.scope ?? vscode.TaskScope.Workspace,
				entryName,
				MagikAliasTaskProvider.AliasType,
				new vscode.ShellExecution(commandLine));
		}
		return undefined;
	}

}

interface AliasTaskDefinition extends vscode.TaskDefinition {
	/**
	 * The entry name.
	 */
	entry: string;

	/**
	 * Additional arguments.
	 */
	args?: string[];
}

let _channel: vscode.OutputChannel;
function getOutputChannel(): vscode.OutputChannel {
	if (!_channel) {
		_channel = vscode.window.createOutputChannel('Aliases Auto Detection');
	}

	return _channel;
}

function getRunAliasPath(): fs.PathLike {
	let smallworldGisPath: fs.PathLike = vscode.workspace.getConfiguration().get('magik.smallworldGis');
	if (process.platform === "win32") {
		return path.join(smallworldGisPath.toString(), 'bin', 'x86', 'runalias.exe');
	}

	return path.join(smallworldGisPath.toString(), 'bin', 'share', 'runalias');
}

function getEnvironmentPath(): fs.PathLike {
	return vscode.workspace.getConfiguration().get('magik.environment');
}

function getAliasesPath(): fs.PathLike {
	return vscode.workspace.getConfiguration().get('magik.aliases');
}

function getStartAliasCommand(runAliasPath: fs.PathLike, aliasesPath: fs.PathLike, entryName: string, environmentFile?: fs.PathLike, additionalArgs?: string[]): string {
	let commandLine = `${runAliasPath} -a ${aliasesPath}`;
	if (environmentFile != null) {
		commandLine = `${commandLine} -e ${environmentFile}`;
	}
	if (additionalArgs != null) {
		commandLine = `${commandLine} ${additionalArgs.join(' ')}`;
	}
	commandLine = `${commandLine} ${entryName}`;
	return commandLine;
}

async function getAliasesTasks(aliasesPath: fs.PathLike): Promise<vscode.Task[]> {
	const emptyTasks: vscode.Task[] = [];

	if (!fs.existsSync(aliasesPath)) {
		return emptyTasks;
	}

	const runAliasPath: fs.PathLike = getRunAliasPath();
	if (!fs.existsSync(runAliasPath)) {
		return emptyTasks;
	}

	const environmentFile: fs.PathLike = getEnvironmentPath();

	const tasks: vscode.Task[] = [];
	try {
		const contents = fs.readFileSync(aliasesPath, 'latin1');
		const lines = contents.split(/\r?\n/);
		for (let line of lines) {
			if (line.length === 0) {
				continue;
			}

			const regExp = /^(\w+):$/;
			const matches = regExp.exec(line);
			if (matches && matches.length === 2) {
				const entryName = matches[1].trim();
				const definition: AliasTaskDefinition = {
					type: MagikAliasTaskProvider.AliasType,
					entry: entryName,
					additionalArguments: [],
				};
				const commandLine = getStartAliasCommand(runAliasPath, aliasesPath, entryName, environmentFile);

				const task = new vscode.Task(
					definition,
					vscode.TaskScope.Workspace,
					entryName,
					MagikAliasTaskProvider.AliasType,
					new vscode.ShellExecution(commandLine));
				tasks.push(task);
			}
		}
	} catch (err) {
		const channel = getOutputChannel();
		channel.appendLine('Auto detecting run_alias tasts failed:');
		channel.appendLine(err);
		channel.show(true);
		return emptyTasks;
	}

	return tasks;
}
