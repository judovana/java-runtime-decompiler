package com.redhat.thermostat.vm.decompiler.core;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.shared.locale.Translate;
import java.util.concurrent.CountDownLatch;

/**
 * Listener for response of requests. 
 * Expects only OK, ERROR or AUTH_FAILED in case Thermostat refuses the request.
 */
public class DecompilerAgentRequestResponseListener implements com.redhat.thermostat.common.command.RequestResponseListener {
    
    private final CountDownLatch latch;
    private String errorMsg = "";
    private boolean isError = false;
    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();
    
    
    public DecompilerAgentRequestResponseListener(CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     *  Is invoked once we received a response.
     * @param request request we put in queue
     * @param response we got from the agent
     */
    @Override
    public void fireComplete(Request request, Response response) {
        switch(response.getType()) {
        case AUTH_FAILED:
            isError = true;
            errorMsg = translate.localize(LocaleResources.REQUEST_FAILED_AUTH_ISSUE).getContents();
            break;
        case ERROR:
            isError = true;
            errorMsg = translate.localize(LocaleResources.REQUEST_FAILED_UNKNOWN_ISSUE).getContents();
            break;
        case OK:
            break;
        default:
            isError = true;
           errorMsg = translate.localize(LocaleResources.ERROR_UNKNOWN_RESPONSE).getContents();
        }
        latch.countDown();
    }
    
    public String getErrorMessage() {
        return errorMsg;
    }
    
    public boolean isError() {
        return isError;
    }

}