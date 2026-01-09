// Handle completions where the prefix looks likeregular method call syntax, e.g.:
//   object.metho
+LSPCompletionHandler {
    *methodHandler {
        ^LSPCompletionHandler.prNew(
            name: "method",
            trigger: ".",
            prefixHandler: {
                |prefix|
                var prefixString = prefix.findRegexp("^[^~0-9A-Za-z-]*([~0-9a-z][\\w\\.]*)");

                if (prefixString.notEmpty) {
                    prefixString = prefixString[1][1];
                    prefixString.stripWhiteSpace;
                } {
                    nil
                }
            },
            action: {
                |prefixString, trigger, completion, provideCompletionsFunc|
                var result = LSPDatabase.findMethods(completion, LSPCompletionHandler.completionLimit);

                Log('LanguageServer.quark').debug("Found % method completions", result[\items].size);

                provideCompletionsFunc.(result[\items], result[\isIncomplete])
            }
        )
    }
}
