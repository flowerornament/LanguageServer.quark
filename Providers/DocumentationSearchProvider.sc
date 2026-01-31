DocumentationSearchProvider : LSPProvider {
    *methodNames {
        ^[
            "documentation/search",
        ]
    }
    *clientCapabilityName { ^nil }
    *serverCapabilityName { ^nil }
    
    init {
        |clientCapabilities|
        // Defer SCDoc indexing so it doesn't block provider registration.
        // Early codeAction requests were arriving before providers finished
        // registering because indexAllDocuments blocks for ~0.5 seconds.
        {
            try {
                SCDoc.indexAllDocuments
            } {}
        }.defer(0.001)
    }
    
    options {
        ^(
        )
    }
    
    onReceived {
        |method, params|
        var brokenAction, urlString, url;
        
        brokenAction = brokenAction ? { |fragment|
			var brokenUrl = URI.fromLocalPath( SCDoc.helpTargetDir++"/BrokenLink.html" );
			brokenUrl.fragment = fragment;
			brokenUrl;
		};

        urlString = params["searchString"].findHelpFile;
        if (urlString.notNil) {
		    url = URI(urlString);
            url = SCDoc.prepareHelpForURL(url) ?? { brokenAction.(urlString) };

            ^(
                uri: url.asString,
                rootUri: SCDoc.helpTargetUrl
            )
        } {
            ^nil
        }
    }
}
