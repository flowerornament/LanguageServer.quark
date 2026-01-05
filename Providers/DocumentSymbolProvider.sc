// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_documentSymbol
DocumentSymbolProvider : LSPProvider {
    *methodNames {
        ^[
            "textDocument/documentSymbol",
        ]
    }
    *clientCapabilityName { ^"textDocument.documentSymbol" }
    *serverCapabilityName { ^"documentSymbolProvider" }
    
    init {
        |clientCapabilities|
    }
    
    options {
        ^(
        )
    }
    
    onReceived {
        |method, params|
        var doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);

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
        
        if (params["textDocument"]["uri"].endsWith(".sc")) {
            ^nil
        } {
            if (params["textDocument"]["uri"].endsWith(".scd")) {
                ^LSPDatabase.getDocumentRegions(doc).collect {
                    |region|
                    (
                        name: region[\text],
                        kind: 2,
                        range: region[\range],
                        selectionRange: region[\range]
                    )
                }
            }
        }
        
        ^nil
    }
}
