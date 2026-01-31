// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#codeAction
CodeActionProvider : LSPProvider {
    *methodNames {
        ^[
            "textDocument/codeAction",
        ]
    }
    *clientCapabilityName { ^"textDocument.codeAction" }
    *serverCapabilityName { ^"codeActionProvider" }

    init {
        |clientCapabilities|
    }

    options {
        ^(
            codeActionKinds: ["source"]
        )
    }

    onReceived {
        |method, params|
        ^try {
            this.handleRequest(method, params)
        } { |error|
            error.reportError;
            []
        }
    }

    handleRequest {
        |method, params|
        var doc, uri, range, normalizedRange, selectionEmpty, actions, line, lineText, lineRange, wordAtCursor;
        var regions, blockRegion, blockRange;

        uri = params["textDocument"]["uri"];
        doc = LSPDocument.findByQUuid(uri);
        if (doc.isNil) { ^[] };

        if (doc.string.isNil) {
            try {
                doc.initFromDisk;
            } { |error| };
        };

        doc.string.isNil.if { ^[] };

        range = params["range"];
        normalizedRange = LSPDatabase.normalizeRange(range) ?? (
            start: (line: 0, character: 0),
            end: (line: 0, character: 0)
        );

        selectionEmpty = LSPDatabase.rangeIsEmpty(normalizedRange);
        actions = [];

        if (selectionEmpty.not) {
            actions = actions.add(
                this.makeEvaluateAction("SC: Evaluate Selection", uri, normalizedRange)
            );
        };

        line = normalizedRange[\start][\line];
        lineText = LSPDatabase.lineTextAt(doc, line);
        lineRange = (
            start: (line: line, character: 0),
            end: (line: line, character: lineText.size)
        );
        actions = actions.add(
            this.makeEvaluateAction("SC: Evaluate Line", uri, lineRange)
        );

        regions = try {
            LSPDatabase.getDocumentRegions(doc)
        } { |error|
            error.reportError;
            []
        };

        if (regions.isNil) { ^actions };
        if (regions.respondsTo(\collect).not) {
            Log('LanguageServer.quark').error("CODEACTION regions lacks collect; class=%", regions.class);
            ^actions
        };
        blockRegion = try {
            regions.reverse.detect {
                |region|
                var regionRange = LSPDatabase.normalizeRange(region[\range]);
                regionRange.isNil.not and: {
                    (normalizedRange[\start][\line] >= regionRange[\start][\line])
                    and: {
                        normalizedRange[\start][\line] <= regionRange[\end][\line]
                    }
                }
            }
        } { |error|
            error.reportError;
            nil
        };
        blockRegion.notNil.if {
            blockRange = try {
                LSPDatabase.normalizeRange(blockRegion[\range])
            } { |error|
                error.reportError;
                nil
            };
            blockRange.isNil.not.if {
                actions = actions.add(
                    this.makeEvaluateAction("SC: Evaluate Block", uri, blockRange)
                );
            }
        };

        actions = actions.add(
            this.makeCommandAction("SC: Hard Stop", "supercollider.internal.cmdPeriod")
        );
        actions = actions.add(
            this.makeCommandAction("SC: Boot Server", "supercollider.internal.bootServer")
        );
        actions = actions.add(
            this.makeCommandAction("SC: Quit Server", "supercollider.internal.quitServer")
        );
        actions = actions.add(
            this.makeCommandAction("SC: Recompile", "supercollider.internal.recompile")
        );

        // Help actions for class under cursor
        wordAtCursor = LSPDatabase.getDocumentWordAt(doc, line, normalizedRange[\start][\character]);
        wordAtCursor !? {
            var cls = wordAtCursor.asSymbol.asClass;
            cls !? {
                actions = actions.add(
                    this.makeEvalAction(
                        "SC: Help: " ++ cls.name,
                        "LSPDatabase.openHelpFor(\"%\")".format(cls.name)
                    )
                );
                actions = actions.add(
                    this.makeEvalAction(
                        "SC: Help in Browser: " ++ cls.name,
                        "LSPDatabase.openHelpInBrowser(\"%\")".format(cls.name)
                    )
                );
                actions = actions.add(
                    this.makeEvalAction(
                        "SC: Help in Terminal: " ++ cls.name,
                        "LSPDatabase.openHelpInTerminal(\"%\")".format(cls.name)
                    )
                );
            };
        };

        Log('LanguageServer.quark').debug("Code actions for %:% — %", uri, normalizedRange, actions);
        ^actions
    }

    makeEvaluateAction {
        |title, uri, range|
        ^(
            title: title,
            kind: "source",
            command: (
                title: title,
                command: "supercollider.evaluateSelection",
                arguments: [uri, range]
            )
        )
    }

    makeCommandAction {
        |title, commandId|
        ^(
            title: title,
            kind: "source",
            command: (
                title: title,
                command: commandId,
                arguments: []
            )
        )
    }

    makeEvalAction { |title, code|
        ^(
            title: title,
            kind: "source",
            command: (
                title: title,
                command: "supercollider.eval",
                arguments: [code]
            )
        )
    }
}
