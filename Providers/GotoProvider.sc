// Base class for goto-style providers (definition, declaration, implementation)
// These all share the same logic: find word at cursor, look up definitions
GotoProvider : LSPProvider {
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

        Log('LanguageServer.quark').debug("Found word at cursor: %", wordAtCursor);

        result = wordAtCursor !? { this.getDefinitionsForWord(wordAtCursor) };

        elapsedMs = ((Main.elapsedTime - startTime) * 1000).round(0.01);
        Log('LanguageServer.quark').debug("[timing] % lookup: %ms", method, elapsedMs);

        ^result
    }

    getDefinitionsForWord {
        |word|
        ^LSPDatabase.findDefinitions(word.asSymbol)
    }
}
