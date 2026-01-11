// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_hover
HoverProvider : LSPProvider {
    *methodNames { ^["textDocument/hover"] }
    *clientCapabilityName { ^"textDocument.hover" }
    *serverCapabilityName { ^"hoverProvider" }

    init { |clientCapabilities| }

    options { ^true }

    onReceived {
        |method, params|
        var doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);
        var wordAtCursor;
        var line = params["position"]["line"].asInteger;
        var character = params["position"]["character"].asInteger;
        var classDoc;
        var debug = "SCLANG_LSP_DEBUG".getenv().notNil;

        if (debug) {
            ("HOVER DEBUG uri=% line=% char=% open=% hasString=%"
                .format(params["textDocument"]["uri"], line, character, doc.isOpen, doc.string.notNil)
            ).postln;
        };

        doc.rehydrateIfNeeded;

        wordAtCursor = LSPDatabase.getDocumentWordAt(
            doc,
            line,
            character
        );

        if (debug) {
            ("HOVER DEBUG word=% open=% size=%"
                .format(wordAtCursor, doc.isOpen, doc.string !? _.size ?? { "nil" })
            ).postln;
        };

        ^(wordAtCursor !? {
            var cls = wordAtCursor.asSymbol.asClass;
            var classDoc, schelpPath, schelpMarkdown;
            var contents = [(
                language: "supercollider",
                value: wordAtCursor.asString
            )];

            // Try to fetch schelp documentation via launcher
            cls !? {
                schelpPath = LSPDatabase.findSchelpPath(cls);
                schelpPath !? {
                    schelpMarkdown = LSPDatabase.fetchSchelpMarkdown(schelpPath);
                };
            };

            // Prefer schelp markdown, fallback to /* */ comment
            if (schelpMarkdown.notNil) {
                contents = contents.add((
                    language: "markdown",
                    value: schelpMarkdown
                ));
            } {
                // Fallback to /* */ comment from class file
                classDoc = cls !? { LSPDatabase.getClassDocumentation(cls) };
                classDoc !? {
                    contents = contents.add((
                        language: "markdown",
                        value: classDoc
                    ));
                };
            };

            (contents: contents)
        })
    }
}
