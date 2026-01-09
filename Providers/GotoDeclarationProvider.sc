// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_declaration
GotoDeclarationProvider : GotoProvider {
    *methodNames { ^["textDocument/declaration"] }
    *clientCapabilityName { ^"textDocument.declaration" }
    *serverCapabilityName { ^"declarationProvider" }
}
