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
            params["position"]["line"].asInteger,
            params["position"]["character"].asInteger
        );

        Log('LanguageServer.quark').info("Hover word: %", wordAtCursor);

        ^(wordAtCursor !? {
            (
                contents: [(
                    language: "supercollider",
                    value: wordAtCursor.asString
                )]
            )
        })
    }
}
