package com.statful.micrometer.registry;

import com.statful.client.domain.api.Aggregation;
import com.statful.client.domain.api.AggregationFrequency;
import com.statful.client.domain.api.StatfulClient;
import com.statful.client.domain.api.Tags;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.apache.logging.log4j.util.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.util.StringUtils.isNotEmpty;

public class StatfulMetricRegistry extends StepMeterRegistry {

    private StatfulClient statfulClient;

    private StatfulMetricsProperties properties;

    public StatfulMetricRegistry(StepRegistryConfig config, StatfulClient statfulClient, Clock clock, StatfulMetricsProperties properties) {
        super(config, clock);

        this.statfulClient = statfulClient;
        this.properties = properties;

        applyCustomFilter();

        start(Executors.defaultThreadFactory());
    }

    public StatfulMetricRegistry(StepRegistryConfig config, StatfulClient statfulClient, Clock clock, StatfulMetricsProperties properties, ThreadFactory threadFactory) {
        super(config, clock);

        this.statfulClient = statfulClient;
        this.properties = properties;

        applyCustomFilter();

        start(threadFactory);
    }

    /**
     * Applies two filters, one that looks into the property "acceptedMetrics" and another that looks into the "alias"
     * and "tags" properties in {@link StatfulMetricsProperties}.
     * <p>
     * The first filter matches the prefixes defined in the list against the metrics. If the list is empty all metrics are
     * accepted. If an alias has been applied to the metric beforehand then the filter will also search for the
     * corresponding key in the "alias" property and accept the metric if found.
     * <p>
     * The second filter adds the tags to the metrics that match the keys in "tags" and replaces the name for the metrics
     * that match the keys in "alias"
     */
    private void applyCustomFilter() {
        this.config()
                .meterFilter(mapMetricToAliasAndTagsFilter());
    }

    private MeterFilter mapMetricToAliasAndTagsFilter() {
        return new MeterFilter() {

            @Override
            public Meter.Id map(Meter.Id id) {
                Meter.Id newId = id;

                for (Map.Entry<String, String> entry : properties.getTags().entrySet()) {
                    if (id.getName().startsWith(entry.getKey())) {
                        List<Tag> tags = toListOfTags(entry.getValue());

                        newId = newId.withTags(tags);
                    }
                }

                String metricAlias = getAliasForMetric(newId);

                if (isNotEmpty(metricAlias)) {
                    newId = newId.withName(metricAlias);
                }

                return newId;
            }
        };
    }

    private String getAliasForMetric(Meter.Id id) {
        for (Map.Entry<String, String> entry : properties.getAlias().entrySet()) {
            if (id.getName().startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return Strings.EMPTY;
    }

    private List<Tag> toListOfTags(String tags) {
        String[] tagsAsList = tags.split(";");

        return Arrays.stream(tagsAsList)
                .map(tag -> {
                    String[] splitTag = tag.split("=");

                    return Tag.of(splitTag[0], splitTag[1]);
                })
                .collect(Collectors.toList());
    }

    protected void publish() {
        this.getMeters().forEach((meter) -> {
            if (meter instanceof Timer) {
                this.writeTimer((Timer) meter);
            } else if (!(meter instanceof FunctionTimer) && !(meter instanceof DistributionSummary) && !(meter instanceof TimeGauge)) {
                if (meter instanceof Gauge) {
                    this.writeGauge((Gauge) meter);
                } else if (meter instanceof Counter) {
                    this.writeCounter((Counter) meter);
                } else if (meter instanceof FunctionCounter) {
                    this.writeCounter((FunctionCounter) meter);
                }
            }

        });
    }

    private void writeCounter(FunctionCounter counter) {
        Meter.Id meterId = counter.getId();

        com.statful.client.domain.api.Tags tags = new com.statful.client.domain.api.Tags();
        meterId.getTags().forEach((tag) -> tags.putTag(tag.getKey(), tag.getValue().replace(" ", "_")));

        this.statfulClient.counter(meterId.getName(), Double.valueOf(counter.count()).intValue()).with().tags(tags).send();
    }

    private void writeCounter(Counter counter) {
        Meter.Id meterId = counter.getId();

        com.statful.client.domain.api.Tags tags = new com.statful.client.domain.api.Tags();
        meterId.getTags().forEach((tag) -> tags.putTag(tag.getKey(), tag.getValue().replace(" ", "_")));

        this.statfulClient.counter(meterId.getName(), Double.valueOf(counter.count()).intValue()).with().tags(tags).send();
    }

    private void writeGauge(Gauge gauge) {
        Meter.Id meterId = gauge.getId();
        com.statful.client.domain.api.Tags tags = new com.statful.client.domain.api.Tags();

        meterId.getTags().forEach((tag) -> tags.putTag(tag.getKey(), tag.getValue().replace(" ", "_")));

        this.statfulClient.gauge(meterId.getName(), gauge.value()).with().tags(tags).send();
    }

    private void writeTimer(Timer timer) {
        HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        Meter.Id meterId = timer.getId();
        com.statful.client.domain.api.Tags tags = new Tags();

        meterId.getTags().forEach((tag) -> tags.putTag(tag.getKey(), tag.getValue().replace(" ", "_")));
        for (ValueAtPercentile pValue :histogramSnapshot.percentileValues()) {
            switch (Double.valueOf(pValue.percentile()*100).intValue()) {
                case 90:
                    statfulClient.aggregatedTimer(meterId.getName(), Double.valueOf(pValue.value()).longValue(), Aggregation.P90, AggregationFrequency.FREQ_10).with().tags(tags).send();
                    break;
                case 95:
                    statfulClient.aggregatedTimer(meterId.getName(), Double.valueOf(pValue.value()).longValue(), Aggregation.P95, AggregationFrequency.FREQ_10).with().tags(tags).send();
                    break;
                case 99:
                    statfulClient.aggregatedTimer(meterId.getName(), Double.valueOf(pValue.value()).longValue(), Aggregation.P99, AggregationFrequency.FREQ_10).with().tags(tags).send();
                    break;
            }
        };

        statfulClient.aggregatedTimer(meterId.getName(), timer.count(), Aggregation.COUNT, AggregationFrequency.FREQ_10).with().tags(tags).send();
        statfulClient.aggregatedTimer(meterId.getName(), Double.valueOf(timer.mean(this.getBaseTimeUnit())).longValue(), Aggregation.AVG, AggregationFrequency.FREQ_10).with().tags(tags).send();
        statfulClient.aggregatedTimer(meterId.getName(), Double.valueOf(histogramSnapshot.max(this.getBaseTimeUnit())).longValue(), Aggregation.MAX, AggregationFrequency.FREQ_10).with().tags(tags).send();

    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
