package com.addthis.metrics3.reporter.config;

import com.addthis.metrics.reporter.config.PredicateConfig;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricFilterTransformer
{

    private static final Logger log = LoggerFactory.getLogger(MetricFilterTransformer.class);

    private static class PredicateConfigFilter implements MetricFilter
    {
        final PredicateConfig predicate;

        PredicateConfigFilter(PredicateConfig predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public boolean matches(String name, Metric metric)
        {
            if (predicate.getUseQualifiedName())
            {
                boolean allowed = predicate.allowString(name);
                log.debug("allow qual-metric? {} {} {}", allowed, name, unqualifyMetricName(name));
                return allowed;
            }
            else
            {
                boolean allowed = predicate.allowString(unqualifyMetricName(name));
                log.debug("allow unq-metric? {} {} {}", allowed, name, unqualifyMetricName(name));
                return allowed;
            }
        }
    }

    private static String unqualifyMetricName(String name)
    {
        int location = name.lastIndexOf('.');
        if (location < 0)
        {
            return name;
        }
        else
        {
            return name.substring(location + 1);
        }
    }

    public static MetricFilter generateFilter(PredicateConfig predicate)
    {
        if (predicate == null)
        {
            log.info("no predicate config, using .ALL as filter");
            return MetricFilter.ALL;
        }
        else
        {
            return new PredicateConfigFilter(predicate);
        }
    }

}
