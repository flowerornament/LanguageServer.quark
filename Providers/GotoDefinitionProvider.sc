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
        var wordAtCursor, startTime, result, elapsedMs;

        startTime = Main.elapsedTime;

        doc.rehydrateIfNeeded;

        wordAtCursor = LSPDatabase.getDocumentWordAt(
            doc,
            params["position"]["line"].asInteger,
            params["position"]["character"].asInteger
        );

        Log('LanguageServer.quark').info("Found word at cursor: %", wordAtCursor);

        result = wordAtCursor !? { this.getDefinitionsForWord(wordAtCursor) };

        elapsedMs = ((Main.elapsedTime - startTime) * 1000).round(0.01);
        Log('LanguageServer.quark').info("[timing] definition lookup: %ms", elapsedMs);

        ^result
    }

    getDefinitionsForWord {
        |word|
        ^LSPDatabase.findDefinitions(word.asSymbol)
    }
}
