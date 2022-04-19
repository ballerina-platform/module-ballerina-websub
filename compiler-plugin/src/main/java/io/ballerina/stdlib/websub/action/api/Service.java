package io.ballerina.stdlib.websub.action.api;

import java.util.List;

public class Service implements CodeSnippet {
    private final List<Annotation> annotations;
    private final List<Function> functions;

    public Service(List<Annotation> annotations, List<Function> functions) {
        this.annotations = annotations;
        this.functions = functions;
    }

    @Override
    // todo: implement this method properly
    public String snippetAsAString() {
        return null;
    }
}
