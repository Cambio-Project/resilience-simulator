package de.rss.fachstudie.MiSim.export;

import desmoj.core.simulator.TimeInstant;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Lion Wagner
 */
public class ReportWriter {

    public static void writeReporterCollectorOutput(TreeMap<String, TreeMap<Double, Object>> data, Path reportLocation) {
        for (Map.Entry<String, TreeMap<Double, Object>> dataset : data.entrySet()) {
            CSVExporter.writeDataset(dataset.getKey(), dataset.getValue(),reportLocation);
            //TODO: custom names for value column at CSVExporter#writeDataset(String,String,Map)
        }
    }

}
