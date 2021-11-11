package org.jrd.backend.data.cli;

class ModifiableDummyTestingHelper extends AbstractSourceTestClass {

    @Override
    protected String getClassName() {
        return "TestingModifiableDummy";
    }

    @Override
    protected String getPackageName() {
        return "testing.modifiabledummy";
    }

    @Override
    String getContentWithoutPackage(String nwHello) {
        return "public class " + getClassName() + " {\n" + "    public static void main(String[] args) throws InterruptedException {\n" +
                "        while(true) {\n" + "            new " + getClassName() + "().print();\n" + "            Thread.sleep(100);\n" +
                "        }\n" + "    }\n" + "   private void print(){\n/*API_PLACEHOLDER*/\nSystem.out.println(\"" + nwHello + "\");}\n" +
                "}\n";
    }

    @Override
    String getGreetings() {
        return "Hello";
    }

}
