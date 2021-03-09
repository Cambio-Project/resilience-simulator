package de.rss.fachstudie.MiSim.export;

import desmoj.core.report.Reporter;
import desmoj.core.simulator.TimeInstant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Lion Wagner
 */
public class MultiDataPointReporter extends Reporter {

    protected final HashMap<String, TreeMap<TimeInstant, ?>> dataSets = new HashMap<>();
    protected final String datasets_prefix;

    public MultiDataPointReporter() {
        this("");
    }

    public MultiDataPointReporter(String datasets_prefix) {
        this.datasets_prefix = datasets_prefix;
        register();
    }

    private void register() {
        ReportCollector.getInstance().register(this);
    }

    public final HashMap<String, TreeMap<TimeInstant, ?>> getDataSets() {
        return dataSets;
    }

    public <T> void addDatapoint(final String dataSetName, final TimeInstant when, final T data) {
        Objects.requireNonNull(dataSetName);
        Objects.requireNonNull(when);
        Objects.requireNonNull(data);

        Map<TimeInstant, T> dataSet = (TreeMap<TimeInstant, T>) dataSets.computeIfAbsent(datasets_prefix + dataSetName, s -> new TreeMap<TimeInstant, T>());
        dataSet.put(when, data);
    }

    //implemented to keep compatibility to desmoj default reporter framework
    @Override
    public String[] getEntries() {
        StringBuilder builder = new StringBuilder("Multidatapointcollector\n");

        //very inefficient (combining, splitting, combining, splitting), but works...
        for (Map.Entry<String, TreeMap<TimeInstant, ?>> dataSet : dataSets.entrySet()) {
            builder.append(dataSet.getKey()).append("\n");
            builder.append(String.join("\n", getEntries(dataSet.getKey())));
        }
        return builder.toString().split("\n");
    }


    public String[] getEntries(String datasetkey) {
        StringBuilder builder = new StringBuilder("Time;Value\n");
        for (Map.Entry<TimeInstant, ?> dataPoint : dataSets.get(datasetkey).entrySet()) {
            builder.append(dataPoint.getKey())
                    .append(";")
                    .append(dataPoint.getValue())
                    .append("\n");
        }
        return builder.toString().split("\n");
    }

}
