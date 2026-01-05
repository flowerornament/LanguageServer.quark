// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_definition
GotoDefinitionProvider : LSPProvider {
    *methodNames {
        ^[
            "textDocument/definition",
        ]
    }
    *clientCapabilityName { ^"textDocument.definition" }
    *serverCapabilityName { ^"definitionProvider" }

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
            params["position"]["line"].asInteger,
            params["position"]["character"].asInteger
        );

        Log('LanguageServer.quark').info("Found word at cursor: %", wordAtCursor);

        ^(wordAtCursor !? { this.getDefinitionsForWord(wordAtCursor) })
    }

    getDefinitionsForWord {
        |word|
        ^LSPDatabase.findDefinitions(word.asSymbol)
    }
}
