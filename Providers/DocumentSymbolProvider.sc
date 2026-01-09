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
        ^nil
    }
    
    onReceived {
        |method, params|
        var doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);
        var uri = params["textDocument"]["uri"];
        var debug = "SCLANG_LSP_DEBUG".getenv().notNil;

        if (debug) {
            ("DOCSYMS DEBUG uri=% open=% hasString=%"
                .format(uri, doc.isOpen, doc.string.notNil)
            ).postln;
        };

        doc.rehydrateIfNeeded;
        
        if (uri.endsWith(".sc")) {
            Log('LanguageServer.quark').warning("DocumentSymbol: skipping .sc file");
            ^nil
        } {
            if (uri.endsWith(".scd")) {
                var regions = LSPDatabase.getDocumentRegions(doc);
                if (debug) { ("DOCSYMS DEBUG regions=%".format(regions.size)).postln; };
                ^regions.collect {
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
