import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as xml2js from 'xml2js';
import * as vscode from 'vscode';
import { integer } from 'vscode-languageserver-types';
import { MagikLanguageClient } from './language-client';

interface MUnitTestItem {
	id: string;
	label: string;
	children: MUnitTestItem[];
	location?: {
		range: {
			start: {
				line: integer,
				character: integer;
			},
			end: {
				line: integer,
				character: integer;
			}
		}
		uri: string;
	};
}

class TestItemCollection implements vscode.TestItemCollection {

	private _items = new Map<string, vscode.TestItem>();

	constructor(...items: vscode.TestItem[]) {
		items.forEach(this.add, this);
	}

	[Symbol.iterator](): Iterator<[id: string, testItem: vscode.TestItem], any, undefined> {
		return this._items.entries();
	}

	public get size() : number {
		return this._items.size;
	}

	replace(items: readonly vscode.TestItem[]): void {
		throw new Error('Method not implemented.');
	}

	forEach(callback: (item: vscode.TestItem, collection: vscode.TestItemCollection) => unknown, thisArg?: unknown): void {
		for (const item of this._items.values()) {
			callback.call(thisArg, item, this);
		}
	}

	add(item: vscode.TestItem): void {
		this._items.set(item.id, item);
	}

	delete(itemId: string): void {
		this._items.delete(itemId);
	}

	get(itemId: string): vscode.TestItem {
		return this._items.get(itemId);
	}

}


let _channel: vscode.OutputChannel;
function getOutputChannel(): vscode.OutputChannel {
	if (!_channel) {
		_channel = vscode.window.createOutputChannel('Magik MUnit');
	}

	return _channel;
}


export class MagikTestProvider implements vscode.Disposable {

	private context: vscode.ExtensionContext;
	private controller: vscode.TestController;
	private client: MagikLanguageClient;
	private workdir: fs.PathLike;
	private currentRun: vscode.TestRun | undefined;

	constructor(context: vscode.ExtensionContext, client: MagikLanguageClient) {
		this.context = context;
		this.client = client;

		const dir = path.join(os.tmpdir(), "vscode-magik-");
		this.workdir = fs.mkdtempSync(dir);

		this.controller = vscode.tests.createTestController('magikMUnitTestController', 'Magik MUnit');
		this.context.subscriptions.push(this.controller);

		this.controller.createRunProfile(
			'Run Tests',
			vscode.TestRunProfileKind.Run,
			(request: vscode.TestRunRequest, token: vscode.CancellationToken) => this.runHandler(request, token),
			true);

		this.registerFileWatchers();
	}

	dispose() {
		if (this.workdir !== null) {
			fs.rmdirSync(this.workdir, {recursive: true});
			this.workdir = null;
		}
	}

	private registerFileWatchers() {
		const watchers = vscode.workspace.workspaceFolders.map(workspaceFolder => {
			const pattern = new vscode.RelativePattern(workspaceFolder, '**/*.magik');
			const watcher = vscode.workspace.createFileSystemWatcher(pattern);

			watcher.onDidDelete(uri => this.onDidDelete(uri));
			watcher.onDidChange(uri => this.onDidChange(uri));

			vscode.workspace.findFiles(pattern).then(files => {
				if (files) {
					this.onDidChange(null);
				}
			});

			return watcher;
		});

		this.context.subscriptions.push(...watchers);
	}

	private onDidDelete(_uri: vscode.Uri) {
		this.getTestItems();
	}

	private onDidChange(_uri: vscode.Uri | null) {
		this.getTestItems();
	}

	private getTestItems() {
		this.client.sendRequest("custom/munit/getTestItems")
			.then((munitTestItems: MUnitTestItem[]) => this.parseTestItems(munitTestItems))
			.catch((err) => vscode.window.showErrorMessage(`Error getting test items: ${err}`));
	}

	private parseTestItems(munitTestItems: MUnitTestItem[]) {
		// MUnit Test Items follow this structure:
		//   sw product --> sw module --> test case exemplar --> test method
		munitTestItems.forEach(munitTestItem => {
			const ti = this.createTestItem(munitTestItem);
			this.controller.items.add(ti);
		});
	}

	private createTestItem(munitTestItem: MUnitTestItem, parent?: vscode.TestItem): vscode.TestItem {
		const uri = munitTestItem.location ? vscode.Uri.parse(munitTestItem.location.uri) : null;
		const testItem = this.controller.createTestItem(munitTestItem.id, munitTestItem.label, uri);
		testItem.range = munitTestItem.location
			? new vscode.Range(
				munitTestItem.location.range.start.line, munitTestItem.location.range.start.character,
				munitTestItem.location.range.end.line, munitTestItem.location.range.end.character)
			: null;

		if (parent) {
			parent.children.add(testItem);
		}

		munitTestItem.children.forEach(munitTestItemChild => this.createTestItem(munitTestItemChild, testItem));

		return testItem;
	}

	private runHandler(request: vscode.TestRunRequest, token: vscode.CancellationToken) {
		if (this.currentRun != null) {
			this.currentRun.end();
		}

		this.currentRun = this.controller.createTestRun(request);

		// Mark all as enqueued.
		(request.include || []).forEach(testItem => {
			this.currentRun.enqueued(testItem);
		});

		// Build test runner script and execute.
		const outputPath = this.getTempFile("test_run.xml");
		const tests = request.include
			? new TestItemCollection(...request.include)
			: this.controller.items;
		this.runTests(tests, outputPath);

		// Wait for file to appear.
		const options = {interval: 250};
		fs.watchFile(outputPath, options, (curr, prev) => {
			if (!fs.existsSync(outputPath)) {
				return;
			}

			// Parse test runner results while marking TestItems.
			this.parseTestRunnerResults(request, this.currentRun, outputPath);

			// Delete XML result file.
			fs.unlinkSync(outputPath);

			this.currentRun.end();

			fs.unwatchFile(outputPath);
		});
	}

	private runTests(testItems: TestItemCollection|vscode.TestItemCollection, outputPath: fs.PathLike) {
		// Gather required products.
		const products = this.getSelfAndAncestors(testItems, 'product');
		const productsStr = products
			.map(testItem => ":" + testItem.id.substr('product:'.length))
			.join(",");

		// Get modules to be loaded.
		const modules = [
			...this.getSelfAndAncestors(testItems, 'module'),
			...this.getSelfAndDescendants(testItems, 'module')
		];
		const modulesStr = modules
			.map(testItem => ":" + testItem.id.substr('module:'.length))
			.join(",");

		// Build test runner script.
		let script = `
_protect
	# Require products to be added.
	_block
		_for product _over {${productsStr}}.fast_elements()
		_loop
			_if sw:smallworld_product.product(product) _is _unset
			_then
				sw:condition.raise(:error, :string, sw:write_string('Product could not be found: ', product))
			_endif
		_endloop
	_endblock

	# Load modules if needed.
	_block
		_for module _over {${modulesStr}}.fast_elements()
		_loop
			_if _not sw:sw_module_manager.module(module).loaded?
			_then
				sw:sw_module_manager.load_module(module)
			_endif
		_endloop
	_endblock

	# Load munit_xml_base for xml_test_runner.
	sw:sw_module_manager.load_module(:munit_xml)

	_dynamic sw:!global_auto_declare?! << _false

	# Create test_suite and run it.
	_block
		_local top_suite << sw:get_global_value(:|sw:test_suite|).new(_unset, :vscode_munit_test_runner)

		# Add all test_cases.`;

		testItems.forEach(testItem => {
			script += this.generateTestCase(testItem, "top_suite");
		});

		script += `
		# Run tests.
		_local tmp_file << sw:system.temp_file_name('vscode_test_run.xml')
		_local output << sw:external_text_output_stream.new(tmp_file, :utf8)
		_protect
			_local runner << sw:get_global_value(:|sw:xml_test_runner|).new(output)
			runner.run_in_foreground(top_suite)
		_protection
			output.close()
		_endprotect

		_local output_path << "${outputPath}"
		sw:system.rename(tmp_file, output_path)
	_endblock
_protection
	_local output_path << "${outputPath}"
	_if _not sw:system.file_exists?(output_path)
	_then
		# Place a dummy file.
		_local tmp_file << sw:system.temp_file_name('vscode_test_run.xml')
		_local os << sw:external_text_output_stream.new(tmp_file)
		os.write('<testsuite name="vscode_munit_test_runner" />')
		os.close()
		sw:system.rename(tmp_file, output_path)
	_endif
_endprotect
$
`;

		// Run script.
		this.client.sendToSession(script, undefined);
	}

	private parseTestRunnerResults(request: vscode.TestRunRequest, run: vscode.TestRun, outputPath: fs.PathLike) {
		const testRunnerResults = fs.readFileSync(outputPath);

		// Log XML.
		let channel = getOutputChannel();
		channel.appendLine("");
		channel.appendLine("XML Test runner output:");
		channel.append(testRunnerResults.toString("utf8"));
		channel.appendLine("");

		xml2js.parseString(testRunnerResults, (err: Error, result: any) => {
			// Parse test suites.
			this.parseTestRunnerSuites(request, run, result.testsuite);
		});
	}

	private parseTestRunnerSuites(request: vscode.TestRunRequest, run: vscode.TestRun, resultTestSuite: any, testItem?: vscode.TestItem) {
		(resultTestSuite.testsuite || []).forEach(testsuite => {
			const id = testsuite.$.name;
			const matchedTestItem = testItem
				? testItem.children.get(id)
				: request.include.filter((value) => value.id == id)[0];
			this.parseTestRunnerSuites(request, run, testsuite, matchedTestItem);
		});

		// Parse test cases.
		(resultTestSuite.testcase || []).forEach(testcase => {
			const id = `method:${testcase.$.name}`;
			const matchedTestItem = testItem
				? testItem.children.get(id)
				: request.include.filter((value) => value.id == id)[0];
			const duration = testcase.$.time ? parseFloat(testcase.$.time) * 1000 : undefined;
			if (testcase.error) {
				const error = testcase.error[0];
				const message = new vscode.TestMessage(`${error.$.type}\n${error._}`);
				run.errored(matchedTestItem, message, duration);
			} else if (testcase.failure) {
				const failure = testcase.failure[0];
				const message = new vscode.TestMessage(`${failure.$.message}\n${failure._}`);
				run.failed(matchedTestItem, message, duration);
			} else {
				run.passed(matchedTestItem, duration);
			}
		});
	}

	private getTempFile(filename: string): fs.PathLike {
		// Get base filename + extension
		const dotIndex = filename.lastIndexOf('.');
		const extension = dotIndex != -1 ? filename.substring(dotIndex + 1, filename.length) : undefined;
		const basename = dotIndex != -1 ? filename.substring(0, dotIndex) : filename

		var tempPath: fs.PathLike;
		var index = 0;
		do {
			tempPath = path.join(this.workdir.toString(), `${basename}${index}.${extension}`);
			index += 1;
		} while (fs.existsSync(tempPath));
		return tempPath;
	}

	private getSelfAndAncestors(testItems: TestItemCollection|vscode.TestItemCollection, filterType?: string): vscode.TestItem[] {
		const filter = filterType ? filterType + ":" : undefined;

		// Try testItems.
		const items = [];
		testItems.forEach(testItem => {
			if (filter) {
				if (testItem.id.startsWith(filter)) {
					items.push(testItem);
				}
			} else {
				items.push(testItem);
			}

			// Recurse all parents of selfs.
			if (testItem.parent) {
				this.getSelfAndAncestors(new TestItemCollection(testItem.parent), filterType).forEach(parentTestItem => {
					items.push(parentTestItem);
				})
			}
		});

		return items;
	}

	private getSelfAndDescendants(testItems: TestItemCollection|vscode.TestItemCollection, filterType?: string): vscode.TestItem[] {
		const filter = filterType ? filterType + ":" : undefined;

		// Try testItems.
		const items = [];
		testItems.forEach(testItem => {
			if (filter) {
				if (testItem.id.startsWith(filter)) {
					items.push(testItem);
				}
			} else {
				items.push(testItem);
			}

			// Recurse all children of testItems.
			testItem.children.forEach(testItemChild => {
				this.getSelfAndDescendants(new TestItemCollection(testItemChild), filterType).forEach(matchedChild => {
					items.push(matchedChild);
				})
			});
		});

		return items;
	}

	private generateTestCase(testItem: vscode.TestItem, parentId: string, indent?: integer): string {
		const indentStr = "	".repeat(indent | 1);
		const newIndent = (indent | 1) + 1;
		if (testItem.children.size) {
			// This is a container test case, generate case and recurse.
			const type = testItem.id.split(":")[0];
			const suiteName = `${type}_suite`;
			let childItems = "";
			testItem.children.forEach(childTestItem => {
				childItems += this.generateTestCase(childTestItem, suiteName, newIndent);
			});
			return `
${indentStr}_block
${indentStr}	_local ${suiteName} << sw:get_global_value(:|sw:test_suite|).new(_unset, "${testItem.id}")
${indentStr}	${parentId}.add_test(${suiteName})
${indentStr}	${childItems}
${indentStr}_endblock`;
		}

		// Actual test case.
		const exemplarName = testItem.parent.id.substring('test_case:'.length);
		const testMethodName = testItem.id.substring('method:'.length);
		return `
${indentStr}	${parentId}.add_test(sw:get_global_value(:|${exemplarName}|).new(:|${testMethodName}|))`;
	}

}
