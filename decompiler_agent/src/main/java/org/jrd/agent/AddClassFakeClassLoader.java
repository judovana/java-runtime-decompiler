package org.jrd.agent;

public final class AddClassFakeClassLoader extends ClassLoader {

    public AddClassFakeClassLoader() {
        super("jrd-agent-add-class-" + System.getProperty(Main.JRD_AGENT_LOADED), InstrumentationProvider.class.getClassLoader());
    }

    public void defineCLassIn(ClassLoader cl, String className, byte[] b) throws ClassNotFoundException {
        //This do not work, defineClass is protected final
        //Class futureClazz = getParent().defineClass(className, b, 0, b.length);
        //Class futureClazz = cl.defineClass(className, b, 0, b.length);
        //System.err.println("JRD Agent added " + futureClazz);
        Class.forName(className);
    }
}
