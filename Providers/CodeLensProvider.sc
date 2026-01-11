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
            var range, start, end, startLine, endLine, name, title;

            range = region[\range];
            if (range.isNil) { nil } {
                start = range[\start];
                end = range[\end];
                startLine = if (start.notNil) { start[\line] ?? 0 } { 0 };
                endLine = if (end.notNil) { end[\line] ?? startLine } { startLine };
                name = region[\text];

                title = if (startLine == endLine) {
                    "SC: Evaluate Line"
                } {
                    // Check if name is meaningful (not auto-generated "[block N]")
                    if (name.notNil and: { name.beginsWith("[block").not }) {
                        "SC: Evaluate: " ++ name
                    } {
                        "SC: Evaluate Block"
                    }
                };

                (
                    range: range,
                    command: (
                        title: title,
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
