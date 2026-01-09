// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_definition
GotoDefinitionProvider : GotoProvider {
    *methodNames { ^["textDocument/definition"] }
    *clientCapabilityName { ^"textDocument.definition" }
    *serverCapabilityName { ^"definitionProvider" }
}
