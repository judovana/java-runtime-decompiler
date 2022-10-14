package org.jrd.backend.communication;

import java.util.ArrayList;
import java.util.List;

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

    class DelegatingHelper {
        private final List<JrdAgent> delegationCandidates = new ArrayList<>(1);

        public JrdAgent addDelegatingAgent(JrdAgent agent) {
            if (!delegationCandidates.contains(agent)) {
                delegationCandidates.add(agent);
                return agent;
            } else {
                return null;
            }
        }

        public JrdAgent removeDelegatingAgent(JrdAgent agent) {
            if (delegationCandidates.contains(agent)) {
                delegationCandidates.remove(agent);
                return agent;
            } else {
                return null;
            }
        }

        public int cleanDelegatingAgents() {
            int r = delegationCandidates.size();
            delegationCandidates.clear();
            return r;
        }
    }

}
