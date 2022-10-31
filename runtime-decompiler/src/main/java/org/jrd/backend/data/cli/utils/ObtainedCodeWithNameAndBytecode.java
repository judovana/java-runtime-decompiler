package org.jrd.backend.data.cli.utils;

public class ObtainedCodeWithNameAndBytecode {
    private final String name;
    private final String base64Body;
    private final Integer bytecodeLevel;

    public ObtainedCodeWithNameAndBytecode(String name, String base64Body, Integer bytecodeLevel) {
        this.name = name;
        this.base64Body = base64Body;
        this.bytecodeLevel = bytecodeLevel;
    }

    public String getName() {
        return name;
    }

    public String getBase64Body() {
        return base64Body;
    }

    public Integer getBytecodeLevel() {
        return bytecodeLevel;
    }
}
