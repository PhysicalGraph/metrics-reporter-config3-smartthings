package smartthings.cassandra.datadog.metrics3;

interface MetricRequest {

    /**
     * Add a gauge
     */
    void addGauge(DatadogGauge gauge);

    /**
     *
     * Add a counter to the request
     */
    void addCounter(DatadogCounter counter);

    /**
     * Send the request to datadog
     */
    void send();
}
