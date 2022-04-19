package io.ballerina.stdlib.websub.action.api;

import java.util.List;

public class Function implements CodeSnippet {
    private String type = "remote";
    private final String  name;
    private final List<FunctionArg> args;
    private final List<Type> returnTypes;
    private boolean optionalReturnTypes = false;

    private Function(String name, List<FunctionArg> args, List<Type> returnTypes) {
        this.name = name;
        this.args = args;
        this.returnTypes = returnTypes;
    }

    public static Function functionWithOptionalReturnTypes(String name, List<FunctionArg> args,
                                                           List<Type> returnTypes) {
        Function function = new Function(name, args, returnTypes);
        function.optionalReturnTypes = true;
        return function;
    }

    @Override
    // todo: implement this method properly
    public String snippetAsAString() {
        return null;
    }

    static class FunctionArg {
        private final Type type;
        private final String name;

        public FunctionArg(Type type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}
