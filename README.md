# metrics-reporter-config3-smartthings
Cassandra metrics3 datadog reporter customization for cassandra 2.2 and 3.x

replaces/supersedes https://github.com/SmartThingsOSS/metrics-reporter-config AND https://github.com/SmartThingsOSS/cassandra-datadog for cassandra 2.2 and (not yet tested) 3.x clusters

note we remove ganglia, graphite, and riemann references. To re-enable:
- compare the ReporterConfig.java with the original project in the reporter-config3 subproject of https://github.com/addthis/metrics-reporter-config
- add the appropriate metrics reporter dependency for ganglia or graphite to the build.gradle file

To deploy this library to a 2.2 or 3.x cassandra cluster:

1) compile with ./gradlew clean build jar from the project root
2) scp the ./build/libs/metrics-reporter-config3-smartthing-3.0.0.jar to the cass nodes lib dir
3) remove/rename-disable the reporter-config-3.0.0.jar (I usually rename to reporter-config-3.0.0.jar.orig.disabled
4) adapt/edit the $cassandra_home/cassandra-reporter.yaml file
5) specify that file in the -Dcassandra.metricsReporterConfigFile option on the cassandra start command

This project has both the configuration override classes and the DatadogReporter3 class that uploads the data

The configuration for the datadog loader is provided by the -Dcassandra.metricsReporterConfigFile=cassandra-reporter.yaml parameter.

In the above example it references the cassandra-reporter.yaml in the $cassandra_home/conf directory

That yaml has the datadog: root which prompts it to be loaded into the datadog instance variable inside the customized ReporterConfig.java source file.

DatadogReporter3 is a lot like the 2.1.x version but works with the considerably reworked classes in metrics3. 

Also, note the package name change: com.addthis.metrics3 rather than com.addthis.metrics

The metric names from 2.1 to 2.2 changed a lot at the tail end of the jmx, with names being swapped in order. 

Bootstrap process:
 1) the -Dcassandra.metricsReporterConfigFile jvm parameter on the cassandra start command triggers the custom reporting jar
 2) the value of that references the cassandra-reporter.yaml file in the cassandra conf directory
 3) that file has a top-level 'datadog' key
 4) the jvm parameter from step 1 triggers the execution of the com.addthis.metrics3.reporter.config.ReporterConfig class that we customized
 5) that class loads the .yaml file. The 'datadog' key in that file references the datadog instance var in ReporterConfig
 6) ReporterConfig invokes DatadogReporter3Config.enable() because of the non-null datadog instance var
 7) DatadogReporter3Config instantiates the DatadogReporter3 class which runs on a timer/schedule based on the yaml conf
 8) DatadogReporter3.report() is invoked for each timer/schedule activation. 


