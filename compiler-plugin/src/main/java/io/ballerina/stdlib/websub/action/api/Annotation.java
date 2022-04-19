package io.ballerina.stdlib.websub.action.api;

import java.util.List;

public class Annotation implements CodeSnippet {
    private final Type type;
    private final List<AnnotationField> fields;

    public Annotation(Type type, List<AnnotationField> fields) {
        this.type = type;
        this.fields = fields;
    }

    @Override
    // todo: implement this properly
    public String snippetAsAString() {
        return null;
    }

    public static class AnnotationField {
        private final String name;
        private final String value;

        public AnnotationField(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
