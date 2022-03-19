package cambio.simulator.entities.patterns;

import java.util.LinkedList;

import cambio.simulator.entities.microservice.Microservice;
import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.TimeSpan;
import org.javatuples.Quartet;
import org.javatuples.Tuple;

/**
 * This class represents an actual CircuitBreaker with the behavior defined by Hystrix.
 *
 * @author Lion Wagner
 * @see CircuitBreaker
 */
public class CircuitBreakerState {

    private final double errorThresholdPercentage;
    private final int rollingWindow; //window over which error rates are collected
    private final double sleepWindow;
    private final Microservice monitoredService;
    //contains the results of the last {rollingWindow size} requests (values, 1 for success and 0 for failure)
    private final LinkedList<Integer> currentWindow = new LinkedList<>();
    private int totalSuccessCounter = 0;
    private int totalFailureCounter = 0;
    private BreakerState state = BreakerState.CLOSED;
    public CircuitBreaker circuitBreaker;

    CircuitBreakerState(Microservice monitoredService, double errorThresholdPercentage, int rollingWindow,
                        double sleepWindow, CircuitBreaker circuitBreaker) {
        this.errorThresholdPercentage = errorThresholdPercentage;
        this.rollingWindow = rollingWindow;
        this.monitoredService = monitoredService;
        this.sleepWindow = sleepWindow;
        this.circuitBreaker = circuitBreaker;
    }

    public Tuple getCurrentStatistics() {
        double errorRate = getErrorRate();
        return new Quartet<>(state, totalSuccessCounter, totalFailureCounter, errorRate);
    }

    public BreakerState getState() {
        return state;
    }

    public boolean isOpen() {
        return state == BreakerState.OPEN;
    }

    synchronized void notifySuccessfulCompletion() {
        totalSuccessCounter++;

        if (state == BreakerState.HALF_OPEN) {
            currentWindow.clear();
            state = BreakerState.CLOSED;
        }

        currentWindow.addLast(1);
        checkErrorRate();
    }

    synchronized void notifyArrivalFailure() {
        totalFailureCounter++;

        if (state == BreakerState.HALF_OPEN) {
            openBreaker();
            return;
        }

        currentWindow.addLast(0);
        checkErrorRate();
    }

    /**
     * Method called by the {@link HalfOpenBreakerEvent} to half open this circuit after a certain amount of time.
     */
    synchronized void toHalfOpen() {
        state = BreakerState.HALF_OPEN;
    }

    private synchronized void checkErrorRate() {
        //cut down the current window to rolling window size
        while (currentWindow.size() > rollingWindow) {
            currentWindow.removeFirst();
        }

        //check error rate if enough entries are present
        if (currentWindow.size() >= rollingWindow) {
            double errorRate = getErrorRate();
            if (errorRate >= errorThresholdPercentage) {
                openBreaker();
            }
        }
    }

    private synchronized void openBreaker() {
        state = BreakerState.OPEN;
        currentWindow.clear();
        if(!circuitBreaker.waitsForHalfOpen){
            circuitBreaker.waitsForHalfOpen = true;
            ExternalEvent openEvent = new HalfOpenBreakerEvent(monitoredService.getModel(), null, false, this);
            openEvent.schedule(new TimeSpan(sleepWindow, monitoredService.getModel().getExperiment().getReferenceUnit()));
        }

    }

    private synchronized double getErrorRate() {
        //if there are not enough datapoints we cant determine errorrate -> 0
        return currentWindow.size() < rollingWindow ? 0 :
            1.0 - ((double) currentWindow.stream().mapToInt(value -> value).sum() / currentWindow.size());
    }


    /**
     * Contains the three possible states of a CircuitBreaker.
     */
    public enum BreakerState {
        CLOSED, HALF_OPEN, OPEN
    }

}
