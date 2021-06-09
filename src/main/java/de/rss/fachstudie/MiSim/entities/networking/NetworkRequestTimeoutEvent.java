package de.rss.fachstudie.MiSim.entities.networking;

import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.rss.fachstudie.MiSim.misc.Priority;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

/**
 * Event that represents the timeout of a {@link Request}. Is automatically scheduled with each send and canceled
 * automatically on failure or receive.
 */
public class NetworkRequestTimeoutEvent extends NetworkRequestEvent implements IRequestUpdateListener {
    private boolean canceled = false;

    public NetworkRequestTimeoutEvent(Model model, String name, boolean showInTrace, Request request) {
        super(model, name, showInTrace, request);
        this.setSchedulingPriority(Priority.LOW);
        this.schedule(new TimeSpan(8, TimeUnit.SECONDS));
    }

    @Override
    public void eventRoutine() throws SuspendExecution {
        if (canceled) {
            return;
        }
        NetworkRequestEvent cancelEvent =
            new NetworkRequestCanceledEvent(getModel(), "RequestCancel", getModel().traceIsOn(), travelingRequest,
                RequestFailedReason.TIMEOUT,
                "Request " + travelingRequest.getName() + " was canceled due to a timeout.");
        cancelEvent.schedule(new TimeSpan(0));
    }

    @Override
    public boolean onRequestFailed(Request request, TimeInstant when, RequestFailedReason reason) {
        canceled = true;
        return false;
    }

    @Override
    public boolean onRequestResultArrivedAtRequester(Request request, TimeInstant when) {
        canceled = true;
        return false;
    }

    @Override
    public int getListeningPriority() {
        return Priority.NORMAL + 1;
    }
}
