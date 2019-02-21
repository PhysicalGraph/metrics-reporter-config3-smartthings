package com.addthis.metrics3.reporter.config;

import com.addthis.metrics.reporter.config.AbstractMetricReporterConfig;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartthings.cassandra.datadog.metrics3.DatadogReporter3;
import smartthings.cassandra.datadog.metrics3.Expansions;
import smartthings.cassandra.datadog.metrics3.PrefixReplacingFormatter;

import java.util.*;

public class DatadogReporter3Config extends AbstractMetricReporterConfig implements MetricsReporterConfigThree {
    private static final Logger log = LoggerFactory.getLogger(DatadogReporter3Config.class);

    String apiKey;
    List<String> expansions = Arrays.asList("RATE_1_MINUTE", "P95");
    String hostName;
    String fileName;
    String prefixReplacement = "cassandra";

    public boolean enable(MetricRegistry registry) {
        log.info("metrics: begin datadog enablement");
        String className = "smartthings.cassandra.datadog.metrics3.DatadogReporter3";
        if (!isClassAvailable(className)) {
            log.error("Tried to enable DatadogReporter, but class "+className+" was not found");
            return false;
        }

        try {
            Set<Expansions> exs = new HashSet<Expansions>();
            for (String ex : expansions) {
                Expansions e = Expansions.valueOf(ex);
                if (ex != null) {
                    log.info("  expansion {}",ex);
                    exs.add(e);
                }
            }
            if (exs.size() <= 0) {
                throw new IllegalArgumentException("Must have one or more expansion");
            }
            log.info("prefix replacement: {}",prefixReplacement);
            log.info("metrics: instantiating DatadogReporter");
            log.debug("  registry names {}",registry.getNames());


            DatadogReporter3 datadogReporter = new DatadogReporter3(
                    "datadog-reporter",
                    registry,
                    hostName,
                    apiKey,
                    fileName,
                    EnumSet.copyOf(exs),
                    null,
                    null,
                    getRealRateunit(),
                    getRealTimeunit(),
                    MetricFilterTransformer.generateFilter(getPredicate()),
                    new PrefixReplacingFormatter("org.apache.cassandra.metrics", prefixReplacement)
            );
            datadogReporter.start(period, getRealTimeunit());
            log.info("metrics: DatadogReporter initialized");

        } catch (Exception e) {
            log.error("metrics: Failed to enable DatadogReporter", e);
            return false;
        }
        return true;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<String> expansions) {
        this.expansions = expansions;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPrefixReplacement() { return prefixReplacement; }

    public void setPrefixReplacement(String prefixReplacement) { this.prefixReplacement = prefixReplacement; }
}