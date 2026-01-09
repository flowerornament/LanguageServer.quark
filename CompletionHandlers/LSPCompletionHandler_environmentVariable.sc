// Handle completions where the prefix looks like an environment variable, e.g.:
//   ~objec
+LSPCompletionHandler {
    *environmentVariableHandler {
        ^LSPCompletionHandler.prNew(
            name: "environment_variable",
            trigger: "~",
            prefixHandler: {
                |prefix|
                if (prefix.isEmpty || "\\W+?$".matchRegexp(prefix)) {
                    true
                } {
                    nil
                }
            },
            action: {
                |prefix, trigger, completion, provideCompletionsFunc|
                var results;

                results = currentEnvironment.keys;

                results = results.asArray.collect({
                    |name|
                    var nameString = name.asString;
                    (
                        label: 			"~" ++ nameString,
                        insertText:		"%$0".format(nameString),
                        filterText:     nameString,
                        insertTextFormat: 2, // Snippet,
                        labelDetails:	(
                            detail: "   %".format(currentEnvironment[name]),
                        ),
                        kind: 			6 // CompletionItemKind.Variable
                    )
                });
                
                if (results.notEmpty) {
                    provideCompletionsFunc.(results, false);
                }
            }
        )
    }
}
