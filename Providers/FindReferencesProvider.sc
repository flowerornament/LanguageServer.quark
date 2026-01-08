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
        var wordAtCursor, startTime, result, elapsedMs;
        var line = params["position"]["line"].asInteger;
        var character = params["position"]["character"].asInteger;
        var debug = "SCLANG_LSP_DEBUG".getenv().notNil;

        startTime = Main.elapsedTime;

        if (debug) {
            ("REFS DEBUG uri=% line=% char=% open=% hasString=%"
                .format(params["textDocument"]["uri"], line, character, doc.isOpen, doc.string.notNil)
            ).postln;
        };

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

        if (debug) {
            ("REFS DEBUG word=% open=% size=%"
                .format(wordAtCursor, doc.isOpen, doc.string !? _.size ?? { "nil" })
            ).postln;
        };

        result = wordAtCursor !? {
            var refs = LSPDatabase.getReferences(wordAtCursor) ?? { Array.new };
            var defs = Array.new;
            var cls, declLoc;
            var includeDecl = (params["context"] !? _["includeDeclaration"]) == true;

            if (debug) {
                ("REFS DEBUG returning % refs for % (includeDecl=% params=%)"
                    .format(refs.size, wordAtCursor, includeDecl, params)
                ).postln;
            };

            // No refs: optionally fall back to definitions or class doc range when includeDecl is true.
            if (refs.size == 0) {
                defs = LSPDatabase.getDefinitionsForWord(wordAtCursor) ?? { Array.new };
                if (debug) { ("REFS DEBUG fallback getDefinitions=% for %".format(defs.size, wordAtCursor)).postln; };

                if (defs.size == 0 and: { includeDecl }) {
                    cls = wordAtCursor.asSymbol.asClass;
                    cls !? {
                        declLoc = this.prClassDocRange(cls);
                        declLoc !? { defs = [declLoc] };
                    };
                };

                defs
            } {
                refs
            }
        } ?? {[]};

        elapsedMs = (Main.elapsedTime - startTime) * 1000;
        Log('LanguageServer.quark').info("[timing] references lookup: %.2fms", elapsedMs);

        ^result
    }

    // Returns a range covering the first block comment in the class file (or line 0 if none).
    *prClassDocRange { |cls|
        ^LSPDatabase.getClassDocRange(cls)
    }
}
