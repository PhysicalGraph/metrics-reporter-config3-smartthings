# metrics-reporter-config3-smartthings
Cassandra metrics3 datadog reporter customization for cassandra 2.2 and 3.x

replaces/supersedes https://github.com/SmartThingsOSS/metrics-reporter-config

note we remove ganglia, graphite, and riemann references. To re-enable:
- compare the ReporterConfig.java with the original project in the reporter-config3 subproject of https://github.com/addthis/metrics-reporter-config
- add the appropriate metrics reporter dependency for ganglia or graphite to the build.gradle file

1) compile with ./gradlew clean build jar
2) scp the ./build/libs/metrics-reporter-config3-smartthing-3.0.0.jar to the cass nodes lib dir
3) remove/rename-disable the reporter-config-3.0.0.jar (I usually rename to reporter-config-3.0.0.jar.orig.disabled

This project has both the configuration override classes and the DatadogReporter3 class that uploads the data

The configuration for the datadog loader is provided by the -Dcassandra.metricsReporterConfigFile=cassandra-reporter.yaml parameter.

In the above example it references the cassandra-reporter.yaml in the $cassandra_home/conf directory

That yaml has the datadog: root which prompts it to be loaded into the datadog instance variable inside the customized ReporterConfig.java source file.

DatadogReporter3 is a lot like the 2.1.x version but works with the considerably reworked classes in metrics3. 

Also, note the package name change: com.addthis.metrics3 rather than com.addthis.metrics

The metric names changed a lot at the tail end of the jmx, with names being swapped in order. 


