# statful-micrometer-registry

Configuration for gathering metrics from springboot actuator and send them to Statful. 
 
## Table of Contents

* [Supported Versions](#supported-versions)
* [Requirements](#requirements)
* [Quick Start](#quick-start)
* [Examples](#examples)
* [Reference](#reference)
* [Authors](#authors)
* [License](#license)

## Supported Versions

| Statful client version | Tested Java versions  | Tested Spring Boot versions
|:---|:---|:---|
| 2.x.x | `Java 8` | `2.1.0.RELEASE` |

## Requirements

This client has the following requirements:

* [springboot-actuator](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-actuator) in order to collect system, datasource, http, etc. metrics.
* [statful-client-java](https://github.com/statful/statful-client-java) in order to send metrics into Statful.

## Quick start
To use this simply add the maven dependency:

```
<dependency>
   <groupId>com.statful.client.framework</groupId>
   <artifactId>statful-micrometer-registry</artifactId>
   <version>${statful-micrometer-registry.version}</version>
</dependency>
```

And then config a `StatfulMetricRegistry` bean in a `@Configuration` class. Keep in mind than you need to enable `StatfulMetricsProperties` using `@EnableConfigurationProperties`:

```
@Configuration
@EnableConfigurationProperties(value = StatfulMetricsProperties.class)
public class StatfulRegistryConfiguration {

    @Resource
    private StatfulMetricRegistryConfig statfulMetricRegistryConfig;

    @Resource
    private StatfulClient statfulClient;

    @Bean
    public MeterRegistry metricRegistry(StatfulMetricsProperties properties) {
        return new StatfulMetricRegistry(statfulMetricRegistryConfig, statfulClient, Clock.SYSTEM, properties);
    }
}
``` 

And enable `@ComponentScan` for the configuration package.

Enable metric collection by setting the following property:

```
statful.client.springboot.metrics.enabled=true
```

## Examples

You can see the metrics output by running the client in dry-run mode, it will look similar to:

```
5:33:28.455 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.cache.entry.memory,app=tel,environment=development,cache=rate-limit-account-buckets-state,ownership=owned,name=rate-limit-account-buckets-state,cacheManager=cacheManager 0.0 1563892408 last,10 100
15:33:28.457 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.memory.max,app=tel,area=heap,environment=development,id=PS\ Old\ Gen 2.772434944E9 1563892408 last,10 100
15:33:28.488 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.timer.jvm.gc.pause,app=tel,action=end\ of\ minor\ GC,cause=Metadata\ GC\ Threshold,unit=ms,environment=development 0 1563892408 avg,p90,count,10 100
15:33:28.488 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.buffer.memory.used,app=tel,environment=development,id=direct 1.0 1563892408 last,10 100
15:33:28.489 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.memory.max,app=tel,area=nonheap,environment=development,id=Metaspace -1.0 1563892408 last,10 100
15:33:28.494 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.memory.used,app=tel,area=heap,environment=development,id=PS\ Eden\ Space 1.25688704E8 1563892408 last,10 100
15:33:28.495 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.threads.daemon,app=tel,environment=development 19.0 1563892408 last,10 100
15:33:28.496 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.system.cpu.usage,app=tel,environment=development 0.851063829787234 1563892408 last,10 100
15:33:28.501 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.threads.states,app=tel,environment=development,state=blocked 1.0 1563892408 last,10 100
15:33:28.502 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.threads.states,app=tel,environment=development,state=new 0.0 1563892408 last,10 100
15:33:28.503 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.counter.jvm.gc.memory.allocated,app=tel,environment=development 0 1563892408 count,sum,10 100
15:33:28.503 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.hikaricp.connections.idle,app=tel,pool=HikariPool-1,environment=development 2.0 1563892408 last,10 100

```

### Extra Configuration

There are a few ways that you can customize your metrics by simply setting
some application properties.

##### Global metric prefix 
This will set a prefix for all your metrics 

```
statful.client.springboot.metrics.prefix=springboot
```

##### Metric alias
This will replace every metric that contains the prefix with the chosen alias:

```
statful.metrics.properties.alias.system.cpu=cpu
statful.metrics.properties.alias.jvm.memory=memory
```

##### Metric tags
A list of tags to be added to the metrics that contain the prefix:

```
statful.metrics.properties.tags.system.cpu=environment=prod
statful.metrics.properties.tags.jvm.memory=unit=Gb
```
To add multiple tags use ';' as the separator:
```
statful.metrics.properties.tags.jvm.memory=unit=Gb;environment=prod
```

##### Filter Metrics

Using Spring Boot's [Per-meter properties](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#_per_meter_properties)  it's possible to filter out certain metrics:

###### application.yml
```
management: 
  metrics:
    enable:
      cpu: false
      memory: false
      jvm: true
      logback: false
      http: true
      process: false
```
###### application.properties
```
management.metrics.enable.cpu=false
management.metrics.enable.memory=false
management.metrics.enable.jvm=true
management.metrics.enable.logback=false
management.metrics.enable.http=true
management.metrics.enable.proccess=false
```

With the configuration:

```
statful.metrics.properties.alias.jvm.memory=memory
statful.metrics.properties.tags.jvm.memory=environment=prod
```
An example of the output this configuration would generate is:

```
15:06:03.519 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=heap,environment=prod,id=PS\ Survivor\ Space 2.883584E7 1563890763 last,10 100
15:06:03.519 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.timer.jvm.gc.pause,app=tel,action=end\ of\ minor\ GC,cause=Allocation\ Failure,unit=ms,environment=development 0 1563890763 avg,p90,count,10 100
15:06:03.519 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.counter.jvm.classes.unloaded,app=tel,environment=development 0 1563890763 count,sum,10 100
15:06:03.520 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=nonheap,environment=prod,id=Metaspace -1.0 1563890763 last,10 100
15:06:03.520 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=heap,environment=prod,id=PS\ Eden\ Space 2.15711232E8 1563890763 last,10 100
15:06:03.520 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.gc.live.data.size,app=tel,environment=development 0.0 1563890763 last,10 100
15:06:03.521 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=nonheap,environment=prod,id=Metaspace 1.02457344E8 1563890763 last,10 100
15:06:03.522 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.threads.states,app=tel,environment=development,state=timed-waiting 61.0 1563890763 last,10 100
15:06:03.522 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=heap,environment=prod,id=PS\ Old\ Gen 6.3446216E7 1563890763 last,10 100
15:06:03.523 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=heap,environment=prod,id=PS\ Survivor\ Space 2.8820288E7 1563890763 last,10 100
15:06:03.523 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.buffer.count,app=tel,environment=development,id=direct 2.0 1563890763 last,10 100
15:06:03.523 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.buffer.total.capacity,app=tel,environment=development,id=direct 8192.0 1563890763 last,10 100
15:06:03.524 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.buffer.count,app=tel,environment=development,id=mapped 0.0 1563890763 last,10 100
15:06:03.524 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.jvm.buffer.total.capacity,app=tel,environment=development,id=mapped 0.0 1563890763 last,10 100
15:06:03.524 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=nonheap,environment=prod,id=Compressed\ Class\ Space 1.441792E7 1563890763 last,10 100
15:06:03.525 [pool-3-thread-1] DEBUG c.s.c.c.sender.BufferedMetricsSender - Dry metric: application.gauge.memory,app=tel,area=nonheap,environment=prod,id=Code\ Cache 2.1168128E7 1563890763 last,10 100

```

> In order to be able to define metric tags as above the springboot [configuration-metadata-annotation-processor](http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/html/configuration-metadata.html#configuration-metadata-annotation-processor) is used. If you're using AspectJ in your project please refer to the documentation on the expected annotation processor behaviour.

## Reference

### Collected Metrics

Metrics are collected according to what's described in the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-metrics.html#production-ready-metrics-meter) and modelled into Statful format with the following considerations:

* The type of metric is inferred by the metric's meaning with is decided both from the Spring Boot documentation and the actual code that exports data
* Timer values are calculated by applying a mean to the values collected in the specified interval
* Metrics that represent a value aggregated over a period of time are sent to Statful as previously aggregated
 
## Authors

[Mindera - Software Craft](https://github.com/Mindera)

## License

Statful Spring Boot Client is available under the MIT license. See the [LICENSE](https://raw.githubusercontent.com/statful/statful-micrometer-registry/master/LICENSE) file for more information.


