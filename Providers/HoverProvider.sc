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

        ("HOVER DEBUG uri=% line=% char=% open=% hasString=%"
            .format(params["textDocument"]["uri"], line, character, doc.isOpen, doc.string.notNil)
        ).postln;

        // Rehydrate if not open.
        if (doc.isOpen.not or: { doc.string.isNil }) {
            TextDocumentProvider.lastOpenByUri[params["textDocument"]["uri"]] !? {
                |cached|
                doc.initFromLSP(
                    cached["languageId"],
                    cached["version"].asInteger,
                    cached["text"]
                ).isOpen_(true);
            };
        };

        wordAtCursor = LSPDatabase.getDocumentWordAt(
            doc,
            line,
            character
        );

        ("HOVER DEBUG word=% open=% size=%"
            .format(wordAtCursor, doc.isOpen, doc.string !? _.size ?? { "nil" })
        ).postln;

        // Try to show a short doc comment from the class file if available.
        classDoc = wordAtCursor !? {
            var cls = wordAtCursor.asSymbol.asClass;
            var path = cls !? _.filenameSymbol !? _.asString;
            var fileString, start, stop;

            if (path.notNil and: { File.exists(path) }) {
                fileString = File.readAllString(path);
                start = fileString.find("/*");
                stop = fileString.find("*/");
                if (start.notNil and: { stop.notNil and: { stop > start } }) {
                    fileString = fileString.copyRange(start + 2, stop - 1).stripWhiteSpace;
                } {
                    fileString = nil;
                };
                fileString;
            } {
                nil
            }
        };

        ^(wordAtCursor !? {
            var contents = [(
                language: "supercollider",
                value: wordAtCursor.asString
            )];

            classDoc !? {
                contents = contents.add((
                    language: "markdown",
                    value: classDoc
                ));
            };

            (contents: contents)
        })
    }
}
