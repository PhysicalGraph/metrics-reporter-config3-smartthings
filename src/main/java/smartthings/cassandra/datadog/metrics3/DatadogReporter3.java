package smartthings.cassandra.datadog.metrics3;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class DatadogReporter3 extends ScheduledReporter {
    private static final Logger log = LoggerFactory.getLogger(DatadogReporter3.class);

    private final Clock clock;

    private final String host;
    private final List<String> tags;
    private final EnumSet<Expansions> expansions;

    private final MetricNameFormatter metricNameFormatter;
    private final MetricTransport transport; // http or file/udp/agent

    public DatadogReporter3(String name,
                            MetricRegistry registry,
                            String host,
                            String apiKey,
                            String fileName,
                            EnumSet<Expansions> expansions,
                            List<String> tags,
                            Clock clock,
                            TimeUnit rateUnit,
                            TimeUnit durationUnit,
                            MetricFilter filter,
                            MetricNameFormatter metricNameFormatter
    ) {
        super(registry, name == null ? "datadog-reporter" : name, filter == null ? MetricFilter.ALL : filter, rateUnit, durationUnit);
        log.debug("metrics: construct datadogreporter3");
        log.debug("  registry names {}", registry.getNames());
        log.debug("      gauges: {}", registry.getGauges(filter));
        log.debug("      histos: {}", registry.getHistograms(filter));
        log.debug("      meters: {}", registry.getMeters(filter));
        log.debug("      timers: {}", registry.getTimers(filter));
        log.debug("      counts: {}", registry.getCounters(filter));
        this.host = host == null ? AwsHelper.getEc2InstanceId() : host;
        log.debug("  host {}", this.host);
        this.clock = clock == null ? Clock.defaultClock() : clock;
        this.tags = tags;
        log.debug("  tags {}", this.tags);
        this.expansions = expansions;
        log.debug("  expansions {}", this.expansions);
        this.metricNameFormatter = metricNameFormatter == null ? new DefaultMetricNameFormatter() : metricNameFormatter;
        if (apiKey != null && !apiKey.isEmpty()) {
            log.debug("  http transport");
            transport = new HttpTransport(apiKey);
        } else if (fileName != null && !fileName.isEmpty()) {
            log.debug("  file transport");
            transport = new FileTransport(fileName);
        } else {
            throw new RuntimeException("metrics: Transport not configured for datadog-reporter");
        }
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers
    ) {
        MetricRequest request;
        try {
            log.debug("metrics: start new req");
            request = transport.prepare();
        } catch (Exception e) {
            log.error("Unable to prepare request for transport", e);
            return;
        }

        log.debug("      gauges: {}",gauges);
        log.debug("      histos: {}",histograms);
        log.debug("      meters: {}",meters);
        log.debug("      timers: {}",timers);
        log.debug("      counts: {}",counters);

        try {
            final long epochMillis = clock.getTime();

            final long epochSeconds = epochMillis / 1000L;
            log.debug("millis: {} seconds: {}", epochMillis, epochSeconds);

            if (!gauges.isEmpty()) {
                for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                    String metricName = entry.getKey();
                    log.debug("  process gauge {}", metricName);
                    Gauge gauge = entry.getValue();
                    if (gauge.getValue() instanceof Number) {
                        pushGauge(request, metricName, (Number) gauge.getValue(), epochSeconds);
                    } else {
                        log.debug("Gauge $metricName had non-Number value, skipped");
                    }
                }
            }

            log.debug("counters");
            if (!counters.isEmpty()) {
                for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                    String metricName = entry.getKey();
                    Counter counter = entry.getValue();
                    log.debug("  process counter {}", metricName);
                    pushCounter(request, metricName, counter.getCount(), epochSeconds);
                }
            }

            log.debug("histos");
            if (!histograms.isEmpty()) {
                for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                    String metricName = entry.getKey();
                    Histogram histogram = entry.getValue();
                    log.debug("  process histogram {}", metricName);
                    Snapshot snapshot = histogram.getSnapshot();
                    checkExpand(request, Expansions.MIN, metricName, snapshot.getMin(), epochSeconds);
                    checkExpand(request, Expansions.MAX, metricName, snapshot.getMax(), epochSeconds);
                    checkExpand(request, Expansions.MEAN, metricName, snapshot.getMean(), epochSeconds);
                    checkExpand(request, Expansions.STD_DEV, metricName, snapshot.getStdDev(), epochSeconds);
                    checkExpand(request, Expansions.MEDIAN, metricName, snapshot.getMedian(), epochSeconds);
                    checkExpand(request, Expansions.P75, metricName, snapshot.get75thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P95, metricName, snapshot.get95thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P98, metricName, snapshot.get98thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P99, metricName, snapshot.get99thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P999, metricName, snapshot.get999thPercentile(), epochSeconds);
                }
            }

            log.debug("meters");
            if (!meters.isEmpty()) {
                for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                    String metricName = entry.getKey();
                    Meter meter = entry.getValue();
                    log.debug("  process meter {}", metricName);
                    if (expansions.contains(Expansions.COUNT)) {
                        pushCounter(request, metricName, meter.getCount(), epochSeconds, Expansions.COUNT.toString());
                    }
                    checkExpand(request, Expansions.RATE_MEAN, metricName, meter.getMeanRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_1_MINUTE, metricName, meter.getOneMinuteRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_5_MINUTE, metricName, meter.getFiveMinuteRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_15_MINUTE, metricName, meter.getFifteenMinuteRate(), epochSeconds);
                }
            }

            log.debug("timers");
            if (!timers.isEmpty()) {
                for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                    String metricName = entry.getKey();
                    Timer timer = entry.getValue();
                    log.debug("  process timer {}", metricName);
                    if (expansions.contains(Expansions.COUNT)) {
                        pushCounter(request, metricName, timer.getCount(), epochSeconds, Expansions.COUNT.toString());
                    }
                    checkExpand(request, Expansions.RATE_MEAN, metricName, timer.getMeanRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_1_MINUTE, metricName, timer.getOneMinuteRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_5_MINUTE, metricName, timer.getFiveMinuteRate(), epochSeconds);
                    checkExpand(request, Expansions.RATE_15_MINUTE, metricName, timer.getFifteenMinuteRate(), epochSeconds);
                    Snapshot snapshot = timer.getSnapshot();
                    checkExpand(request, Expansions.MIN, metricName, snapshot.getMin(), epochSeconds);
                    checkExpand(request, Expansions.MAX, metricName, snapshot.getMax(), epochSeconds);
                    checkExpand(request, Expansions.MEAN, metricName, snapshot.getMean(), epochSeconds);
                    checkExpand(request, Expansions.STD_DEV, metricName, snapshot.getStdDev(), epochSeconds);
                    checkExpand(request, Expansions.MEDIAN, metricName, snapshot.getMedian(), epochSeconds);
                    checkExpand(request, Expansions.P75, metricName, snapshot.get75thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P95, metricName, snapshot.get95thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P98, metricName, snapshot.get98thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P99, metricName, snapshot.get99thPercentile(), epochSeconds);
                    checkExpand(request, Expansions.P999, metricName, snapshot.get999thPercentile(), epochSeconds);
                }
            }
            request.send();
            log.debug("metrics: req done");
        } catch (Exception e) {
            log.error("Exception in metric reporting", e);
            throw e;
        }
    }

    private void checkExpand(MetricRequest request, Expansions expansion, String name, Number count, Long epoch) {
        if (expansions.contains(expansion)) {
            pushGauge(request, name, count, epoch, expansion.toString());
        }
    }

    private void pushCounter(MetricRequest request, String metricName, Number count, Long epoch, String... path) {
        String name = metricNameFormatter.format(metricName, path);
        DatadogCounter counter = new DatadogCounter(name, count.longValue(), epoch, host, tags);
        try {
            request.addCounter(counter);
        } catch (Exception e) {
            log.error("Error writing counter $name", e);
        }

    }

    private void pushGauge(MetricRequest request, String metricName, Number count, long epoch, String... path) {
        String name = metricNameFormatter.format(metricName, path);
        DatadogGauge gauge = new DatadogGauge(name, count.longValue(), epoch, host, tags);
        try {
            request.addGauge(gauge);
        } catch (Exception e) {
            log.error("Error writing gauge $name", e);
        }
    }


}
