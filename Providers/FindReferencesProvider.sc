// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_implementation
FindReferencesProvider : LSPProvider {
    *methodNames {
        ^[
            "textDocument/references",
        ]
    }
    *clientCapabilityName { ^"textDocument.references" }
    *serverCapabilityName { ^"referencesProvider" }

    init {
        |clientCapabilities|
    }

    options {
        ^true
    }

    onReceived {
        |method, params|
        var doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);
        var wordAtCursor;
        var line = params["position"]["line"].asInteger;
        var character = params["position"]["character"].asInteger;

        ("REFS DEBUG uri=% line=% char=% open=% hasString=%"
            .format(params["textDocument"]["uri"], line, character, doc.isOpen, doc.string.notNil)
        ).postln;

        // If the doc isn't open yet, try to rehydrate from last didOpen cache.
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

        ("REFS DEBUG word=% open=% size=%"
            .format(wordAtCursor, doc.isOpen, doc.string !? _.size ?? { "nil" })
        ).postln;

        ^(wordAtCursor !? {
            var refs = LSPDatabase.getReferences(wordAtCursor) ?? { Array.new };
            var defs = Array.new;
            var cls, declLoc;
            var includeDecl = params["context"] !? _["includeDeclaration"] ?? { false };

            ("REFS DEBUG returning % refs for % (includeDecl=% params=%)"
                .format(refs.size, wordAtCursor, includeDecl, params)
            ).postln;

            // No refs: optionally fall back to definitions or class doc range when includeDecl is true.
            if (refs.size == 0) {
                defs = LSPDatabase.getDefinitionsForWord(wordAtCursor) ?? { Array.new };
                ("REFS DEBUG fallback getDefinitions=% for %".format(defs.size, wordAtCursor)).postln;

                if (defs.size == 0 and: { includeDecl }) {
                    cls = wordAtCursor.asSymbol.asClass;
                    cls !? {
                        declLoc = this.prClassDocRange(cls);
                        declLoc !? { defs = [declLoc] };
                    };
                };

                ^defs
            };

            refs
        } ?? {[]})
    }

    // Returns a range covering the first block comment in the class file (or line 0 if none).
    *prClassDocRange { |cls|
        var path = cls.filenameSymbol !? _.asString;
        var startLine, endLine, fileLines;
        if (path.isNil) { ^nil };

        ("REFS DEBUG classDoc path=%".format(path)).postln;

        if (File.exists(path)) {
            fileLines = File.readAllString(path).split($\n);
            fileLines.do {
                |ln, idx|
                if (startLine.isNil and: { ln.find("/*").notNil }) {
                    startLine = idx;
                };
                if (startLine.notNil and: { endLine.isNil and: { ln.find("*/").notNil } }) {
                    endLine = idx;
                };
            };
        };

        startLine = startLine ?? { 0 };
        endLine = endLine ?? { startLine };

        ^(
            uri: path.standardizePath.pathToFileURI,
            range: (
                start: (line: startLine, character: 0),
                end: (line: endLine, character: 0)
            )
        )
    }
}
