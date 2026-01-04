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

        // Debug logging - silently fail if directory doesn't exist
        try {
            File.use(
                Platform.userAppSupportDir ++ "/Zed/sclang-debug.log",
                "a",
                { |file|
                    file.write("codelens handleRequest %\n".format(params["textDocument"]["uri"]))
                }
            );
        };

        doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);

        if (doc.isNil) { ^[] };

        if (doc.string.isNil) {
            try {
                doc.initFromDisk;
            } { |error|
                error.reportError;
            };
        };

        doc.string.isNil.if { ^[] };

        // Debug logging - silently fail if directory doesn't exist
        try {
            File.use(
                Platform.userAppSupportDir ++ "/Zed/sclang-debug.log",
                "a",
                { |file|
                    file.write("codelens doc class=% size=%\n".format(doc.string.class, doc.string.size))
                }
            );
        };

        regions = try {
            LSPDatabase.getDocumentRegions(doc)
        } { |error|
            error.reportError;
            []
        };

        if (regions.isNil) { ^[] };

        // Debug logging - silently fail if directory doesn't exist
        try {
            File.use(
                Platform.userAppSupportDir ++ "/Zed/sclang-debug.log",
                "a",
                { |file|
                    file.write("codelens regions class=%\n".format(regions.class))
                }
            );
        };

        if (regions.respondsTo(\collect).not) {
            Log('LanguageServer.quark').error("CODELENS regions lacks collect; class=%", regions.class);
            ^[]
        };

        ^regions.collect {
            |region|
            (
                range: region[\range],
                command: (
                    title: "▶ EVALUATE ———————————————————————————————",
                    command: "supercollider.evaluateSelection",
                    name: region[\text],
                    arguments: [
                        params["textDocument"]["uri"],
                        region[\range],
                    ]
                )
            )
        }.asArray
    }
}
