// https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize
TextDocumentProvider : LSPProvider {
    classvar <>pendingOpens, <>pendingChanges, <>initialized=false;
    classvar <>lastOpenByUri;
    *methodNames {
        ^[
            "textDocument/didOpen",
            "textDocument/didChange",
            "textDocument/didClose",
            "textDocument/didSave",
        ]
    }
    *clientCapabilityName { ^"textDocument.synchronization" }
    *serverCapabilityName { ^"textDocumentSync" }

    init {
        |clientCapabilities|
        // No superclass init (LSPFeature:init is subclassResponsibility)
        ^this
    }

    *initClass {
        super.initClass;
        pendingOpens = Array.new;
        pendingChanges = Array.new;
        initialized = false;
        lastOpenByUri = ();
    }

    *queuePending {
        |method, params|
        // Stash didOpen/didChange that arrive before the provider is registered.
        pendingOpens = pendingOpens ?? { Array.new };
        pendingChanges = pendingChanges ?? { Array.new };

        method = method.asString;

        if (method == "textDocument/didOpen") {
            pendingOpens = pendingOpens.add(params);
            // Cache latest text per URI for fallback rehydrate.
            lastOpenByUri[params["textDocument"]["uri"]] = params["textDocument"];
        } {
            if (method == "textDocument/didChange") {
                pendingChanges = pendingChanges.add(params);
            }
        };
        Log('LanguageServer.quark').warning(
            "Queued pending % (opens=% changes=%)",
            method,
            pendingOpens.size,
            pendingChanges.size
        );
    }

    options {
        // https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocumentSyncOptions
        ^(
            openClose: true,
            change: 2, // Incremental
            save: true
        )
    }

    onReceived {
        |method, params|
        Log('LanguageServer.quark').info("Handling: %", method);

        // If initialization hasn't completed yet, queue didOpen/didChange.
        if (initialized.not and: { ["textDocument/didOpen", "textDocument/didChange"].includes(method) }) {
            Log('LanguageServer.quark').warning("Queuing % until server is initialized", method);
            if (method == 'textDocument/didOpen') {
                pendingOpens = pendingOpens.add(params);
            } {
                pendingChanges = pendingChanges.add(params);
            };
            ^nil
        };

        switch(
            method,

            'textDocument/didChange', {
                this.didChange(
                    params["textDocument"]["uri"],
                    params["textDocument"]["version"],
                    params["contentChanges"]
                )
            },
            'textDocument/didOpen', {
                this.didOpen(
                    uri: 		params["textDocument"]["uri"],
                    languageId: params["textDocument"]["languageId"],
                    version:	params["textDocument"]["version"].asInteger,
                    text:		params["textDocument"]["text"],
                );
            },
            'textDocument/didClose', {
                this.didClose(
                    uri: params["textDocument"]["uri"]
                )
            },
            'textDocument/didSave', {
                this.didSave(
                    uri: params["textDocument"]["uri"]
                )
            },
            {
                Error("Couldn't handle method: %".format(method)).throw
            }
        );

        ^nil
    }

    didOpen {
        |uri, languageId, version, text|
        Log('LanguageServer.quark').warning("didOpen % version=% size=%", uri, version, text.size);
        lastOpenByUri[uri] = (
            uri: uri,
            languageId: languageId,
            version: version,
            text: text
        );
        LSPConnection.connection.prHandleNotification(
            method: 'window/logMessage',
            params: (
                type: 3,
                message: "TEXTDOCUMENT didOpen % lang=% version=% size=%".format(uri, languageId, version, text.size)
            )
        );
        LSPDocument.findByQUuid(uri).initFromLSP(
            languageId,
            version,
            text
        ).isOpen_(true);
    }

    didClose {
        |uri|
        LSPDocument.findByQUuid(uri).isOpen_(false)
    }

    didSave {
        |uri|
        LSPDocument.findByQUuid(uri).documentWasSaved();
    }

    didChange {
        |uri, version, changes|
        var doc = LSPDocument.findByQUuid(uri);
        var range;
        Log('LanguageServer.quark').warning("didChange % version=% changes=%", uri, version, changes.size);

        // Handle race condition where didChange arrives before didOpen is processed
        if (doc.isOpen.not) {
            Log('LanguageServer.quark').warning("Document % received change before open, forcing open", uri);
            doc.isOpen_(true);
        };

        changes = changes.collect {
            |change|
            if (change["range"].notNil) {
                range = change["range"];
                LSPDocumentChange(
                    range["start"]["line"].asInteger,
                    range["start"]["character"].asInteger,
                    range["end"]["line"].asInteger,
                    range["end"]["character"].asInteger,
                    change["text"]
                )
            } {
                LSPDocumentChange.wholeDocument(change["text"])
            }
        };

        changes.do(doc.applyChange(version, _));
    }

    *processPending {
        Log('LanguageServer.quark').warning("Processing % pending didOpen and % pending didChange messages", pendingOpens.size, pendingChanges.size);
        initialized = true;

        pendingOpens.do {
            |params|
            this.new().didOpen(
                uri:        params["textDocument"]["uri"],
                languageId: params["textDocument"]["languageId"],
                version:    params["textDocument"]["version"].asInteger,
                text:       params["textDocument"]["text"],
            );
        };

        pendingChanges.do {
            |params|
            this.new().didChange(
                params["textDocument"]["uri"],
                params["textDocument"]["version"],
                params["contentChanges"]
            );
        };

        pendingOpens = Array.new;
        pendingChanges = Array.new;
    }
}
