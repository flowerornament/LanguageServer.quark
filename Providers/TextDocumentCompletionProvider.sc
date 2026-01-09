// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_completion
TextDocumentCompletionProvider : LSPProvider {
    *methodNames { 
        ^["textDocument/completion"] 
    }
    *clientCapabilityName { ^"textDocument.completion" }
    *serverCapabilityName { ^"completionProvider" }
    
    init {
        |clientCapabilities|
    }

    // https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#completionOptions
    options {
        ^(
            triggerCharacters: [".", "(", "~"],
            allCommitCharacters: [],
            resolveProvider: false,
            completionItem: (
                labelDetailsSupport: true
            )
        )
    }
    
    onReceived {
        |method, params|
        var doc, line, character, triggerCharacters;
        
        doc = LSPDocument.findByQUuid(params["textDocument"]["uri"]);
        triggerCharacters = params["context"]["triggerCharacter"];
        
        line = params["position"]["line"].asInteger;
        character = params["position"]["character"].asInteger;
        
        LSPDatabase.getDocumentLine(doc, line) !? {
            |lineString|
            lineString = lineString[0..character];
            
            ^LSPCompletionHandler.handleCompletion(lineString, triggerCharacters);
        } ?? {
            ^nil
        }
    }
}
