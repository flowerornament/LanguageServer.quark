// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_codeAction
CodeLensProvider : LSPProvider {
    *methodNames {
        ^[
            "textDocument/codeLens",
        ]
    }
    *clientCapabilityName { ^"textDocument.codeLens" }
    *serverCapabilityName { ^"codeLensProvider" }

    init {
        |clientCapabilities|
    }

    options {
        ^(
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
        var doc, regions;

        doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);
        if (doc.isNil) { ^[] };

        if (doc.string.isNil) {
            try { doc.initFromDisk } { |error| };
        };
        if (doc.string.isNil) { ^[] };

        regions = try {
            LSPDatabase.getDocumentRegions(doc)
        } { |error|
            error.reportError;
            []
        };

        if (regions.isNil) { ^[] };
        if (regions.respondsTo(\collect).not) { ^[] };

        ^regions.collect { |region|
            var range = region[\range];
            if (range.isNil) { nil } {
                (
                    range: range,
                    command: (
                        title: "SC: Evaluate (CodeLens)",
                        command: "supercollider.evaluateSelection",
                        arguments: [
                            params["textDocument"]["uri"],
                            range,
                        ]
                    )
                )
            }
        }.reject(_.isNil).asArray
    }
}
