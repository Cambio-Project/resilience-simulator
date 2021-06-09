package de.rss.fachstudie.MiSim.export;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import desmoj.core.report.Reporter;
import desmoj.core.simulator.TimeInstant;

/**
 * Dynamically-typed data point collector.
 *
 * @author Lion Wagner
 */
public class MultiDataPointReporter extends Reporter {

    protected final HashMap<String, HashMap<Double, ?>> dataSets = new HashMap<>();
    protected final String datasetsPrefix;

    public MultiDataPointReporter() {
        this("");
    }

    public MultiDataPointReporter(String datasetsPrefix) {
        this.datasetsPrefix = datasetsPrefix;
        register();
    }

    private void register() {
        ReportCollector.getInstance().register(this);
    }

    public final HashMap<String, HashMap<Double, ?>> getDataSets() {
        return dataSets;
    }

    public <T> void addDatapoint(final String dataSetName, final TimeInstant when, final T data) {
        Objects.requireNonNull(dataSetName);
        Objects.requireNonNull(when);
        Objects.requireNonNull(data);

        Map<Double, T> dataSet =
            (HashMap<Double, T>) dataSets.computeIfAbsent(datasetsPrefix + dataSetName, s -> new HashMap<Double, T>());
        dataSet.put(when.getTimeAsDouble(), data);
    }

    //implemented to keep compatibility to desmoj default reporter framework
    @Override
    public String[] getEntries() {
        StringBuilder builder = new StringBuilder("Multidatapointcollector\n");

        //very inefficient (combining, splitting, combining, splitting), but works...
        for (Map.Entry<String, HashMap<Double, ?>> dataSet : dataSets.entrySet()) {
            builder.append(dataSet.getKey()).append("\n");
            builder.append(String.join("\n", getEntries(dataSet.getKey())));
        }
        return builder.toString().split("\n");
    }


    public String[] getEntries(String datasetkey) {
        StringBuilder builder = new StringBuilder("Time;Value\n");
        for (Map.Entry<Double, ?> dataPoint : dataSets.get(datasetkey).entrySet()) {
            builder.append(dataPoint.getKey())
                .append(";")
                .append(dataPoint.getValue())
                .append("\n");
        }
        return builder.toString().split("\n");
    }

    public void reset() {
        dataSets.clear();
    }

}
