// https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_symbol
WorkspaceSymbolProvider : LSPProvider {
    *methodNames {
        ^[
            "workspace/symbol"
        ]
    }
    *clientCapabilityName { ^"workspace.symbol" }
    *serverCapabilityName { ^"workspaceSymbolProvider" }
    
    init {
        |clientCapabilities|
    }
    
    options {
        ^()
    }
    
    onReceived {
        |method, params|
        var query = params["query"];
        var result;

        Log('LanguageServer.quark').info("WorkspaceSymbol query: '%'", query);
        result = LSPDatabase.findSymbols(query, LSPCompletionHandler.completionLimit);
        Log('LanguageServer.quark').info("WorkspaceSymbol result count: %", result.size);
        ^result;
    }
}