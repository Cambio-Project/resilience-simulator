package de.rss.fachstudie.MiSim.events;

import java.util.Arrays;
import java.util.Objects;

import co.paralleluniverse.fibers.SuspendExecution;
import de.rss.fachstudie.MiSim.entities.microservice.Microservice;
import de.rss.fachstudie.MiSim.entities.microservice.Operation;
import de.rss.fachstudie.MiSim.misc.Util;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.NumericalDist;
import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

/**
 * Event that triggers a latency injection. The injection can be applied on different levels:<br> Either all outgoing
 * requests of a {@code Microservice} are delayed.<br> Or all outgoing dependency requests of a single {@code Operation}
 * can be delayed.<br> Or the connection between two specific {@code Operation}s can also be delayed.
 *
 * @author Lion Wagner
 */
public class LatencyMonkeyEvent extends SelfScheduledEvent {
    private final double delay;
    private final double stdDeviation;
    private final Microservice microservice;
    private final Operation operationSrc;
    private final Operation operationTrg;

    private double duration;

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, Microservice microservice) {
        this(model, name, showInTrace, delay, 0, microservice, null, null);
    }

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, Operation operationSrc) {
        this(model, name, showInTrace, delay, 0, operationSrc.getOwnerMS(), operationSrc, null);
    }

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, Operation operationSrc,
                              Operation operationTrg) {
        this(model, name, showInTrace, delay, 0, operationSrc.getOwnerMS(), operationSrc, operationTrg);
    }

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, double stdDeviation,
                              Microservice microservice) {
        this(model, name, showInTrace, delay, stdDeviation, microservice, null, null);
    }

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, double stdDeviation,
                              Operation operationSrc) {
        this(model, name, showInTrace, delay, stdDeviation, operationSrc.getOwnerMS(), operationSrc, null);
    }

    public LatencyMonkeyEvent(Model model, String name, boolean showInTrace, double delay, double stdDeviation,
                              Microservice microservice, Operation operationSrc, Operation operationTrg) {
        super(model, name, showInTrace);
        Objects.requireNonNull(microservice);

        this.delay = delay;
        this.stdDeviation = stdDeviation;
        this.microservice = microservice;
        this.operationSrc = operationSrc;
        this.operationTrg = operationTrg;

        validateArguments();
    }

    private void validateArguments() {
        Objects.requireNonNull(microservice);

        Util.requireNonNegative(delay, "The injected delay cannot be negative");
        Util.requireNonNegative(stdDeviation, "The standard deviation of the injected delay cannot be negative");

        if (operationSrc != null) {
            if (Arrays.stream(microservice.getOperations()).noneMatch(operation -> operation == operationSrc)) {
                throw new IllegalArgumentException(
                    String.format("Operation %s is not an operation of microservice %s", operationSrc, microservice));
            }
            if (operationTrg != null
                && Arrays.stream(operationSrc.getDependencies())
                .noneMatch(dependency -> dependency.getTargetOperation() == operationTrg)) {
                throw new IllegalArgumentException(
                    String.format("Operation %s is not a dependency of operation %s", operationTrg, operationSrc));
            }
        }
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public void eventRoutine() {
        NumericalDist<Double> dist =
            new ContDistNormal(getModel(), String.format("DelayDistribution_of_%s", this.getName()), delay,
                               stdDeviation, false, false);
        microservice.applyDelay(dist, operationSrc, operationTrg);
        new ExternalEvent(getModel(), "LatencyMonkeyDeactivator", this.traceIsOn()) {
            @Override
            public void eventRoutine() throws SuspendExecution {
                microservice.applyDelay(null, operationSrc, operationTrg);
            }
        }.schedule(new TimeSpan(duration));

    }
}
