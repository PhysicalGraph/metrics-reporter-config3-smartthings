/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.addthis.metrics3.reporter.config;

import com.addthis.metrics.reporter.config.AbstractReporterConfig;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;


// CEM: customized for datadog
// ser/d class.  May make a different abstract for commonalities

// Stupid bean for simplicity and snakeyaml, instead of @Immutable
// like any sane person would intend
public class ReporterConfig extends AbstractReporterConfig {
    private static final Logger log = LoggerFactory.getLogger(ReporterConfig.class);

    @Valid
    private List<ConsoleReporterConfig> console;
    @Valid
    private List<CsvReporterConfig> csv;
    @Valid
    private List<DatadogReporter3Config> datadog;

    public List<ConsoleReporterConfig> getConsole() {
        return console;
    }

    public void setConsole(List<ConsoleReporterConfig> console) {
        this.console = console;
    }

    public List<CsvReporterConfig> getCsv() {
        return csv;
    }

    public void setCsv(List<CsvReporterConfig> csv) {
        this.csv = csv;
    }

    public List<DatadogReporter3Config> getDatadog() {
        return datadog;
    }

    public void setDatadog(List<DatadogReporter3Config> dd) {
        this.datadog = dd;
    }

    public boolean enableConsole(MetricRegistry registry) {
        boolean failures = false;
        if (console == null) {
            log.debug("Asked to enable console, but it was not configured");
            return false;
        }
        for (ConsoleReporterConfig consoleConfig : console) {
            if (!consoleConfig.enable(registry)) {
                failures = true;
            }
        }
        return !failures;
    }

    public boolean enableCsv(MetricRegistry registry) {
        boolean failures = false;
        if (csv == null) {
            log.debug("Asked to enable csv, but it was not configured");
            return false;
        }
        for (CsvReporterConfig csvConfig : csv) {
            if (!csvConfig.enable(registry)) {
                failures = true;
            }
        }
        return !failures;
    }

    public boolean enableDatadog(MetricRegistry registry) {
        boolean failures = false;
        if (datadog == null) {
            log.debug("Asked to enable datadog, but it was not configured");
            return false;
        }
        for (DatadogReporter3Config datadogConfig : datadog) {
            if (!datadogConfig.enable(registry)) {
                failures = true;
            }
        }
        return !failures;
    }

    public boolean enableAll(MetricRegistry registry) {
        log.debug("metrics config: perform enable checks");
        boolean enabled = false;
        if (console != null) {
            if (enableConsole(registry)) {
                enabled = true;
            }
        }
        if (csv != null) {
            if (enableCsv(registry)) {
                enabled = true;
            }
        }
        if (datadog != null) {
            log.debug("metrics config: datadog configured");
            if (enableDatadog(registry)) {
                log.debug("metrics config: datadog enabled");
                enabled = true;
            }
        }
        if (!enabled) {
            log.warn("No reporters were successfully enabled");
        }
        return enabled;
    }

    public static ReporterConfig loadFromFileAndValidate(String fileName) throws IOException {
        log.debug(log.isInfoEnabled() ? "metrics config: load/validate " + fileName : "");
        ReporterConfig config = loadFromFile(fileName);
        if (validate(config)) {
            return config;
        } else {
            throw new ReporterConfigurationException("configuration failed validation");
        }
    }

    public static ReporterConfig loadFromFile(String fileName) throws IOException {
        log.debug(log.isInfoEnabled() ? "metrics config: loading " + fileName : "");
        return AbstractReporterConfig.loadFromFile(fileName, ReporterConfig.class);
    }


}
