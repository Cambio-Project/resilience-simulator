package de.rss.fachstudie.MiSim.entities.microservice;

import de.rss.fachstudie.MiSim.entities.networking.*;
import de.rss.fachstudie.MiSim.export.MultiDataPointReporter;
import de.rss.fachstudie.MiSim.resources.CPUImpl;
import de.rss.fachstudie.MiSim.resources.CPUProcess;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import java.util.LinkedHashSet;

/**
 * @author Lion Wagner
 */
public class MicroserviceInstance extends Entity implements IRequestUpdateListener {

    private final CPUImpl cpu;
    private final Microservice owner;
    private final int instanceID;

    private InstanceState state;
    private LinkedHashSet<Request> currentRequestsToHandle = new LinkedHashSet<>(); //Queue with only unique entries
    private LinkedHashSet<Request> currentInternalRequests = new LinkedHashSet<>(); //Queue with only unique entries

    private LinkedHashSet<NetworkRequestSendEvent> currentAnswers = new LinkedHashSet<>(); //Contains all current outgoing answers
    private LinkedHashSet<NetworkRequestSendEvent> currentInternalSends = new LinkedHashSet<>(); //contains all current outgoing dependency requests

    private final MultiDataPointReporter reporter;

    public MicroserviceInstance(Model model, String name, boolean showInTrace, Microservice microservice, int instanceID) {
        super(model, name, showInTrace);
        this.owner = microservice;
        this.instanceID = instanceID;
        this.cpu = new CPUImpl(model, String.format("%s_CPU", name), showInTrace, microservice.getCapacity());

        String[] names = name.split("_");
        reporter = new MultiDataPointReporter(String.format("I%s_[%s]_", names[0], names[1]));

        changeState(InstanceState.CREATED);
    }

    public double getUsage() {
        return this.cpu.getCurrentRelativeWorkDemand();
    }

    public InstanceState getState() {
        return state;
    }

    public void handle(Request request) {
        if (!checkIfCanHandle(request)) { //throw error if instance cannot handle the request
            throw new IllegalStateException(String.format("Cannot handle this Request. State: [%s]", this.state));
        }

        if (currentRequestsToHandle.add(request)) { //register request and stamp as received
            request.stampReceivedAtHandler(presentTime());
        }


        //three possiblities:
        //1. request is completed -> send it back to its sender (target is retrieved by the SendEvent)
        //2. requests' dependecies were all recevied -> send it to the cpu for handling. The CPU will "send" it back to this method once its done.
        //3. request does have dependencies -> create internal
        if (request.isCompleted()) {
            RequestAnswer answer = new RequestAnswer(request);
            answer.addUpdateListener(this);
            NetworkRequestSendEvent sendEvent = new NetworkRequestSendEvent(getModel(), "Request_Answer_" + request.getQuotedName(), traceIsOn(), answer);
            currentAnswers.add(sendEvent);
            sendEvent.schedule();//send away the answer

        } else if (request.getDependencyRequests().isEmpty() || request.areDependencies_completed()) {
            CPUProcess newProcess = new CPUProcess(request);
            cpu.submitProcess(newProcess);
        } else {
            for (NetworkDependency dependency : request.getDependencyRequests()) {

                Request internalRequest = new InternalRequest(getModel(), this.traceIsOn(), dependency, this);
                internalRequest.addUpdateListener(this);
                currentInternalRequests.add(internalRequest);

                NetworkRequestSendEvent sendEvent = new NetworkRequestSendEvent(getModel(), String.format("Send Cascading_Request for %s", request.getQuotedName()), traceIsOn(), internalRequest);
                currentInternalSends.add(sendEvent);
                sendEvent.schedule(presentTime());
            }
        }

        collectQueueStatistics(); //collecting Statistics
    }

    /**
     * Checks whether this Instance can handle the Request.
     *
     * @return true if this request will be handled, false otherwise
     */
    public boolean checkIfCanHandle(Request request) {

        //if the instance is running it can handle the request
        if ((state == InstanceState.RUNNING)) return true;

        //if the instance is shutting down but already received the request it can continue to finish it.
        // else the instance cant handle the instance
        return state == InstanceState.SHUTTING_DOWN && currentRequestsToHandle.contains(request);
    }

    private void changeState(InstanceState targetState) {
        if (this.state == targetState)
            return;

        sendTraceNote(this.getQuotedName() + " changed to state " + targetState.name());
        reporter.addDatapoint("State", presentTime(), targetState.name());
        this.state = targetState;

    }

    public void start() {
        if (!(this.state == InstanceState.CREATED || this.state == InstanceState.SHUTDOWN)) {
            throw new IllegalStateException(String.format("Cannot start Instance %s: Was not recently created or Shutdown. (Current State [%s])", this.getQuotedName(), state.name()));
        }

        changeState(InstanceState.STARTING);

        changeState(InstanceState.RUNNING);

    }

    public final void startShutdown() {
        if (!(this.state == InstanceState.CREATED || this.state == InstanceState.SHUTDOWN)) {
            throw new IllegalStateException(String.format("Cannot start Instance %s: Was not recently created or Shutdown. (Current State [%s])", this.getQuotedName(), state.name()));
        }
        changeState(InstanceState.SHUTTING_DOWN);
    }

    public final void shutdown() {
        if (this.state != InstanceState.SHUTTING_DOWN) {
            throw new IllegalStateException(String.format("Cannot shutdown Instance %s: This instance has not started its shutdown. (Current State [%s])", this.getQuotedName(), state.name()));
        }
        changeState(InstanceState.SHUTDOWN);
    }

    public final void die() {
        if (this.state == InstanceState.KILLED) {
            throw new IllegalStateException(String.format("Cannot kill Instance %s: This instance was already killed. (Current State [%s])", this.getQuotedName(), state.name()));
        }
        changeState(InstanceState.KILLED);

        //clears all currently running calculations
        cpu.clear();
        //cancel all send answers
        currentAnswers.forEach(NetworkRequestSendEvent::cancel);
        //cancel all send answers
        currentInternalSends.forEach(NetworkRequestSendEvent::cancel);

        //TODO: notify sender of currently handled requests, that the requests failed (TCP/behavior)
        currentRequestsToHandle.forEach(Request::cancelExecutionAtHandler);


    }

    public final Microservice getOwner() {
        return owner;
    }

    public final int getInstanceID() {
        return instanceID;
    }


    private void collectQueueStatistics() {
        reporter.addDatapoint("SendOff_Internal_Requests", presentTime(), currentInternalRequests.size());
        reporter.addDatapoint("Requests_InSystem", presentTime(), currentRequestsToHandle.size());
        reporter.addDatapoint("Requests_NotComputed", presentTime(), currentRequestsToHandle.stream().filter(request -> !request.isComputation_completed()).count());
        reporter.addDatapoint("Requests_WaitingForDependencies", presentTime(), currentRequestsToHandle.stream().filter(request -> !request.isDependencies_completed()).count());
    }

    @Override
    public void onRequestFailed(Request failed_request) {
        //TODO: Retry and Circuitbreaker
    }

    @Override
    public void onRequestArrivalAtTarget(Request request) {
        if (request instanceof RequestAnswer) {
            request = ((RequestAnswer) request).unpack();
        }

        currentRequestsToHandle.remove(request);
        if (currentRequestsToHandle.isEmpty() && getState() == InstanceState.SHUTTING_DOWN) {
            InstanceShutdownEndEvent event = new InstanceShutdownEndEvent(getModel(), String.format("Instance %s Shutdown End", this.getQuotedName()), traceIsOn());
            event.schedule(this); //shutdown after the last answer was send. It doesn't care if the original sender does not live anymore
        }
    }
}
