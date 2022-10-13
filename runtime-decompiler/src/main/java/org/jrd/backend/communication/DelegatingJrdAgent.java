package org.jrd.backend.communication;

public interface DelegatingJrdAgent extends JrdAgent {

    enum CommandDelegationOptions {
        FIRST_OK /*eg  getBytecode*/,
        ALL /*eg init, and most likely also get classes, but that needs set form all without duplications*/,
        MAIN_ONLY /*eg overwrite */
    }

    String submitRequest(String request);

    JrdAgent addDelegatingAgent(JrdAgent agent);

    JrdAgent removeDelegatingAgent(JrdAgent agent);

    int cleanDelegatingAgents();

}
