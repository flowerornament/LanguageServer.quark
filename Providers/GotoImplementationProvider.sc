// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_implementation
GotoImplementationProvider : GotoProvider {
    *methodNames { ^["textDocument/implementation"] }
    *clientCapabilityName { ^"textDocument.implementation" }
    *serverCapabilityName { ^"implementationProvider" }
}
