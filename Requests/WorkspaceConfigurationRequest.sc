// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_codeAction
WorkspaceConfiguration : LSPRequest {
    var sections, namespace="supercollider", clientOptions;
    
    *methodNames {
        ^[
            "workspace/configuration",
        ]
    }
    *clientCapabilityName { ^"workspace.configuration" }
    *serverCapabilityName { ^nil }
    
    init {
        |clientCapabilities|
        Log('LanguageServer.quark').debug("initializing WorkspaceConfiguration");
        
        sections = [
            "sclang.evaluateResultPrefix",
            "sclang.postEvaluateResults",
            "sclang.improvedErrorReports",
            "sclang.maxCompletions",
            "languageServerLogLevel"
        ];
        clientOptions = ();
        
        fork {
            0.01.wait; // Delay needed - immediate request on launch fails
            this.doRequest();
        }
    }
    
    doRequest {
        this.sendRequest((
            items: sections.collect {
                |section|
                (section: [namespace, section].join("."))
            }
        )).then({
            |options|
            Log('LanguageServer.quark').debug("client options: %", options);
            
            clientOptions.clear();
            options.do {
                |option, index|
                clientOptions[sections[index].asSymbol] = option;
            };

            // Update completion limit if configured
            clientOptions['sclang.maxCompletions'] !? { |v|
                LSPCompletionHandler.completionLimit = v.asInteger.clip(10, 1000);
            };

            server.changed(\clientOptions, clientOptions);
        }).onError({
            |e|
            e.dumpBackTrace
        });
    }
    
    options {
        ^(
        )
    }
}


