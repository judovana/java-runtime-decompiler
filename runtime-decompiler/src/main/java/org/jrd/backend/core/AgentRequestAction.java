/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package org.jrd.backend.core;

import org.jrd.backend.communication.DelegatingJrdAgent;
import org.jrd.backend.data.VmInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author pmikova
 */
public class AgentRequestAction {

    private final Map<String, String> parameters;

    public enum RequestAction {
        HELLO(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        VERSION(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        CLASSES(DelegatingJrdAgent.CommandDelegationOptions.ALL),
        SEARCH_CLASSES(DelegatingJrdAgent.CommandDelegationOptions.ALL),
        BYTES(DelegatingJrdAgent.CommandDelegationOptions.FIRST_OK),
        HALT(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        OVERWRITE(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        ADD_CLASS(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        ADD_JAR(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        INIT_CLASS(DelegatingJrdAgent.CommandDelegationOptions.ALL),
        OVERRIDES(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        REMOVE_OVERRIDES(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY),
        CLASSES_WITH_INFO(DelegatingJrdAgent.CommandDelegationOptions.MAIN_ONLY);

        private final DelegatingJrdAgent.CommandDelegationOptions delegation;

        RequestAction(DelegatingJrdAgent.CommandDelegationOptions delegation) {
            this.delegation = delegation;
        }

        public static RequestAction fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(RequestAction.values()).filter(v -> v.toString().equals(s)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

    }

    public AgentRequestAction() {
        parameters = new TreeMap<>();
    }

    public static final String VM_ID_PARAM_NAME = "vm-id";
    public static final String VM_PID_PARAM_NAME = "vm-pid";
    public static final String ACTION_PARAM_NAME = "action";
    public static final String HOSTNAME_PARAM_NAME = "hostname:";
    public static final String LISTEN_PORT_PARAM_NAME = "listen-port";
    public static final int NOT_ATTACHED_PORT = -1;
    public static final String CLASS_NAME_PARAM = "class--name-param";

    public static final String CLASS_TO_OVERWRITE_BODY = "body-to-overwrite";
    public static final String CLASS_LOADER = "class-loader";

    public static
            AgentRequestAction
            create(VmInfo vmInfo, String hostname, int listenPort, RequestAction action, String name, String base64body) {
        AgentRequestAction req = create(vmInfo, hostname, listenPort, action, name);
        req.setParameter(CLASS_TO_OVERWRITE_BODY, base64body);
        return req;
    }

    public static AgentRequestAction create(
            VmInfo vmInfo, String hostname, int listenPort, RequestAction action, String name, String base64body, String base64classloader
    ) {
        AgentRequestAction req = create(vmInfo, hostname, listenPort, action, name);
        req.setParameter(CLASS_TO_OVERWRITE_BODY, base64body);
        req.setParameter(CLASS_LOADER, base64classloader);
        return req;
    }

    public static AgentRequestAction create(VmInfo vmInfo, String hostname, int listenPort, RequestAction action, String name) {
        AgentRequestAction req = create(vmInfo, hostname, listenPort, action);
        req.setParameter(CLASS_NAME_PARAM, name);
        return req;
    }

    public static AgentRequestAction create(VmInfo vmInfo, String hostname, int listenPort, RequestAction action) {
        AgentRequestAction req = new AgentRequestAction();
        req.setParameter(VM_ID_PARAM_NAME, vmInfo.getVmId());
        req.setParameter(VM_PID_PARAM_NAME, Integer.toString(vmInfo.getVmPid()));
        req.setParameter(ACTION_PARAM_NAME, action.toString());
        req.setParameter(HOSTNAME_PARAM_NAME, hostname);
        req.setParameter(LISTEN_PORT_PARAM_NAME, Integer.toString(listenPort));
        return req;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }
}
