// https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize
InitializeProvider : LSPProvider {
    classvar <>suggestedServerPort=57110;
    classvar <>initializeActions, <>startupFiles;
    classvar <>cachedInitializeParams;  // Survives recompile

    var <initializationOptions, initializeParams;

    *methodNames {
        ^["initialize"]
    }
    *clientCapabilityName { ^nil }
    *serverCapabilityName { ^nil }

    *onInitialize {
        |func|
        initializeActions = initializeActions.add(func);
    }

    *prDoOnInitialize {
        |options|

        thisProcess.platform.startup;
        StartUp.run;

        initializeActions.do {
            |func|
            try {
                func.value(options)
            } {
                |e|
                e.dumpBacktrace
            }
        }
    }

    init {
    }

    options {
        // https://microsoft.github.io/language-server-protocol/specifications/specification-3-17/#clientCapabilities
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
        var serverCapabilities, startupPaths;
        var debug = "SCLANG_LSP_DEBUG".getenv().notNil;

        if (debug) { "*** INITIALIZE REQUEST RECEIVED ***".postln; };

        initializeParams = params;
        this.class.cachedInitializeParams = params;  // Cache for recompile survival
        initializationOptions = initializeParams["initializationOptions"] ?? {()};

        initializeParams["workspaceFolders"] !? {
            |folders|
            folders.do {
                |folder|
                server.workspaceFolders.add(folder["uri"].copy.fileURIToPath)
            };
        } ?? {
            initializeParams["rootUri"] ?? initializeParams["rootPath"] !? {
                |root|
                server.workspaceFolders.add(root.copy.fileURIToPath)
            };
        };

        Log('LanguageServer.quark').debug("suggestedServerPortRange: %", initializationOptions["suggestedServerPortRange"]);
        initializationOptions["suggestedServerPortRange"] !? {
            |range|
            range = [range[0].asInteger, range[1].asInteger];
            this.class.suggestedServerPort = range[0];
            if (debug) { "Using default server port: % (allocated range: %-%)".format(range[0], range[0], range[1]-1).postln; };
            Server.all.do {
                |s|
                s.addr.port = this.class.suggestedServerPort.asInteger;
            }
        };

        initializationOptions["useGlobalStartupFile"] ?? {"true"} !? {
            |bool|
            if (bool == "true") {
                startupPaths = startupPaths.add(thisProcess.platform.userConfigDir);
            }
        };

        initializationOptions["useWorkspaceStartupFile"] ?? {"false"} !? {
            |bool|
            if (bool == "true") {
                startupPaths = startupPaths.addAll(server.workspaceFolders);
            }
        };

        this.class.startupFiles = startupPaths.collect { |p| p +/+ "startup.scd" };

        serverCapabilities = ();
        this.addProviders(initializeParams["capabilities"], serverCapabilities);
        Log('LanguageServer.quark').info("Server capabilities are: %", serverCapabilities);
        Log('LanguageServer.quark').info("Registered providers: %", LSPConnection.providers.keys);

        // After providers are registered, let TextDocumentProvider process any queued opens/changes
        TextDocumentProvider.processPending();

        { this.class.prDoOnInitialize(initializationOptions) }.defer(0.0000001);

        ^(
            "serverInfo": server.serverInfo,
            "capabilities": serverCapabilities;
        );
    }

    *reregisterProvidersIfCached {
        |server|
        cachedInitializeParams !? {
            |params|
            var provider = this.new(server, {});
            var serverCapabilities = ();
            var debug = "SCLANG_LSP_DEBUG".getenv().notNil;

            if (debug) { "*** RE-REGISTERING PROVIDERS FROM CACHED INITIALIZE ***".postln; };
            Log('LanguageServer.quark').info("Re-registering providers from cached initializeParams");

            provider.addProviders(params["capabilities"], serverCapabilities);
            Log('LanguageServer.quark').info("Re-registered server capabilities: %", serverCapabilities);

            TextDocumentProvider.processPending();
        };
    }

    addProviders {
        |clientCapabilities, serverCapabilities, pathRoot=([])|
        var allProviders = LSPFeature.all;

        Log('LanguageServer.quark').debug("Found providers: %", allProviders.collect(_.methodNames).join(", "));

        allProviders.do {
            |providerClass|
            var provider, clientCapability;

            // If clientCapabilityName.isNil, assume we ALWAYS use this provider
            clientCapability = providerClass.clientCapabilityName !? {
                this.getClientCapability(clientCapabilities, providerClass.clientCapabilityName)
            } ?? { () };

            clientCapability !? {
                |capability|
                Log('LanguageServer.quark').debug("Registering provider: %", providerClass.methodNames);

                provider = providerClass.new(server, capability);

                providerClass.serverCapabilityName !? {
                    |capabilityName|
                    this.addServerCapability(
                        serverCapabilities,
                        capabilityName,
                        provider.options
                    )
                };

                server.addProvider(provider);
            }
        }
    }

    getClientCapability {
        |clientCapabilities, path|
        Log('LanguageServer.quark').debug("Checking for client capability at % (clientCapabilities: %)", path, clientCapabilities);

        if (path.isNil) { ^() };

        path.split($.).do {
            |key|
            if (clientCapabilities.isNil or: { clientCapabilities.isKindOf(Dictionary).not }) {
                ^nil
            } {
                clientCapabilities = clientCapabilities[key]
            }
        };

        ^clientCapabilities
    }

    addServerCapability {
        |serverCapabilities, path, options|
        Log('LanguageServer.quark').debug("Adding server capability at %: %", path, options);

        if (path.isNil) { ^this };

        if (options.notNil) {
            path = path.split($.).collect(_.asSymbol);

            if (path.size > 1) {
                path[0..(path.size-2)].do {
                    |key|
                    Log('LanguageServer.quark').debug("looking up key %", key);
                    serverCapabilities[key] = serverCapabilities[key] ?? { () };
                    serverCapabilities = serverCapabilities[key];
                };
            };

            Log('LanguageServer.quark').debug("writing options into key %", path.last);
            serverCapabilities[path.last] = options;
        }
    }
}
