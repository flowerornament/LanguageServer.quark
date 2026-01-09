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
                var methodIndexRange, methods, results, isIncomplete;

                methodIndexRange = LSPDatabase.matchMethods(completion);

                Log('LanguageServer.quark').info("Found % completions, returning %", methodIndexRange.size, min(100, methodIndexRange.size));

                isIncomplete = (methodIndexRange.size > 100);
                methodIndexRange.size = min(100, methodIndexRange.size);
                
                results = Array(methodIndexRange.size);
                
                methodIndexRange.collect {
                    |index|
                    var method = LSPDatabase.allMethods[index];
                    results = results.add(LSPDatabase.makeMethodCompletion(method))
                };
                
                provideCompletionsFunc.(results, isIncomplete)
            }
        )
    }
}
