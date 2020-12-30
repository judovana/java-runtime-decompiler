
public class Jasm {

    public String decompile(byte[] bytecode, String[] options) {
		org.openjdk.asmtools.Main.main(new String[]{});
        return "jasm invoked";
    }
}
