// Handle completions where the prefix looks like a class name, e.g.:
//   Class.metho
+LSPCompletionHandler {
    *classMethodHandler {
        ^LSPCompletionHandler.prNew(
            name: "method_class",
            trigger: ".",
            prefixHandler: {
                |prefix|
                var prefixClass;

                prefixClass = prefix.findRegexp("^[^\\w]*([A-Z][A-Za-z0-9_]*)");

                if (prefixClass.notEmpty) {
                    prefixClass = prefixClass[1][1];
                    
                    if ((prefixClass = prefixClass.asSymbol.asClass).notNil) {
                        prefixClass.class
                    } {
                        nil
                    }
                } {
                    nil
                }
            },
            action: {
                |prefixClass, trigger, completion, provideCompletionsFunc|
                var results = LSPDatabase.findClassMethods(prefixClass);

                Log('LanguageServer.quark').debug("Found % class method completions", results.size);

                provideCompletionsFunc.value(results, false);
            }
        )
    }
}
