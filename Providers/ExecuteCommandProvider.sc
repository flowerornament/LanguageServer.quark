// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_implementation
ExecuteCommandProvider : LSPProvider {
    *methodNames {
        ^[
            "workspace/executeCommand",
        ]
    }
    // Always register - needed for HTTP eval endpoint which doesn't go through LSP init
    *clientCapabilityName { ^nil }
    *serverCapabilityName { ^"executeCommandProvider" }

    init {
        |clientCapabilities|
    }

    options {
        ^(
            commands: this.class.commands.keys.asArray
        )
    }

    *commands {
        ^(
            'supercollider.internal.bootServer': {
                Server.default.boot;
            },
            'supercollider.internal.rebootServer': {
                Server.default.reboot;
            },
            'supercollider.internal.recompile': {
                thisProcess.recompile;
            },
            'supercollider.internal.quitServer': {
                Server.default.quit;
            },
            'supercollider.internal.killAllServers': {
                Server.killAll();
            },
            'supercollider.internal.showServerWindow': {
                Server.default.makeWindow()
            },
            'supercollider.internal.showServerMeter': {
                Server.default.meter()
            },
            'supercollider.internal.showScope': {
                Server.default.scope()
            },
            'supercollider.internal.showFreqscope': {
                Server.default.freqscope()
            },
            'supercollider.internal.dumpNodeTree': {
                Server.default.queryAllNodes()
            },
            'supercollider.internal.dumpNodeTreeWithControls': {
                Server.default.queryAllNodes(true)
            },
            'supercollider.internal.showNodeTree': {
                Server.default.plotTree()
            },
            'supercollider.internal.startRecording': {
                Server.default.record()
            },
            'supercollider.internal.pauseRecording': {
                Server.default.pauseRecording()
            },
            'supercollider.internal.stopRecording': {
                Server.default.stopRecording()
            },
            'supercollider.internal.cmdPeriod': {
                CmdPeriod.run();
            },
            // Direct eval command - takes raw source code, no document required
            // Used by HTTP eval endpoint in sc_launcher
            // NOTE: Do NOT use ^ (non-local return) in these command functions.
            // They are called via valueArray, and ^ would bypass the return value capture.
            'supercollider.eval': {
                |sourceCode|
                var result, output;
                if (sourceCode.isNil || { sourceCode.isEmpty }) {
                    (result: "")
                } {
                    try {
                        result = sourceCode.interpret;
                        ("> " ++ result.asString).postln;
                        output = (result: result.asString);
                    } {
                        |error|
                        error.reportError;
                        output = (error: error.errorString);
                    };
                    output
                }
            },
            'supercollider.evaluateSelection': {
                |uri, range|
                var doc, normalizedRange, source;

                doc = LSPDocument.findByQUuid(uri) ?? { LSPDocument.findByQUuid(uri.urlDecode) };

                if (doc.isNil) {
                    "supercollider.evaluateSelection: document % not found".format(uri).warn;
                    nil
                } {
                    normalizedRange = LSPDatabase.normalizeRange(range);
                    source = LSPDatabase.stringForRange(doc, normalizedRange);

                    if (source.isNil) {
                        "supercollider.evaluateSelection: unable to extract source for range %".format(range).warn;
                        nil
                    } {
                        EvaluateProvider.evaluateSource(doc, source)
                    }
                }
            }
        )
    }

    onReceived {
        |method, params|
        var command, arguments, result;

        command = params["command"].asSymbol;
        arguments = params["arguments"];

        this.class.commands[command] !? {
            |func|
            arguments = arguments ?? { [] };
            arguments.isArray.not.if {
                arguments = [arguments];
            };
            result = func.valueArray(arguments);
            ^result
        } ?? {
            Exception("Command doesn't exist: %".format(command)).throw;
            ^nil
        }
    }
}
