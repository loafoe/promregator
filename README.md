# Promregator Project

The term "Promregator" is an artifical composite of the two terms "Prometheus" and "aggregator".

Prometheus is referring to the [Prometheus project](https://prometheus.io/), which is toolset intended for retrieving telemetry data of running applications.
It supports monitoring these metrics and triggering alerts based on thresholds.

Aggregator is referring to the aggregator concept, which you encounter several times in the Platform-as-a-Service offering of Cloud Foundry. 
The most prominent case of an aggregator in Cloud Foundry most likely is the [Loggregator](https://docs.cloudfoundry.org/loggregator/architecture.html).

The Promregator project intends to provide an aggregator-like tool for the Cloud Foundry environment, which allows fetching 
the Prometheus metrics of a set of Clound Foundry app instances. Here is the list of major features provided:

* Standard Java-Application implemented using the Spring Framework. There is no need for administrative privileges on your Cloud Foundry installation!
  You can even run this app outside of your Cloud Foundry environment (such as behind an internal firewall)
* Multiple CF apps in multiple Orgs and multiple spaces supported
* Automatic retrieval of App GUIDs and instance identifiers using the CF API (no need to install a CF CLI locally)
* Automatic update of App GUIDs, if newer versions of the monitored CF apps are deployed (delay up to five minutes possible)
* Fetching of multiple Prometheus Metric endpoints is performed in parallel (number of concurrent threads used for retrieving metrics can be configured)
* Support of Authentication Schemes at the CF app's endpoint. The following Authentication schemes are currently available:
  - JWT-based token (with retrieval of JWT from OAuth2 server, JWT is cached)
  - Null Authentication (not recommended for productive environments!)
  The Authentication schemes are easily extensible.
* Configuration using standard Spring properties as defined by the Spring Framework (e.g. using `application.yaml` file).
* Simple HTTP proxy support is available for contacting CF app endpoints.
* All metrics provided from the Cloud Foundry applications are automatically [enriched with additional labels](docs/enrichment.md), indicating their origin (similar to the `job` and `instance` labels [created by Prometheus](https://prometheus.io/docs/concepts/jobs_instances/)).
* [Additional metrics are provided](docs/enrichment.md) supporting you to monitor Promregator and the communication to the Cloud Foundry Applications.
* Promregator's endpoint (`/metrics`) supports GZIP compression, if the clients accepts this.

## Use Case

Prometheus is a great tool to retrieve metrics at a constant rate from a set of targets. It is based on a pull-based fetching mechanism. However,
it is quite simplistic (due to conceptual reasons) with regards to reaching out to endpoints which are not uniquely identified by its URL. Moreover, it
has certain limitations on client-side authentication mechanisms, which are based on the [OAuth2](https://oauth.net/2/)/[JWT](https://jwt.io/) protocol family.

Cloud Foundry provides the capabilty to run nearly arbitrary HTTP-enabled applications (e.g. developed in Java) on a Platform-as-a-Service-enabled environment. 
To provide fail-over of applications and to support scalability, multiple instances of the same application may be run behind a reverse proxy (a.k.a. web dispatcher).
Requests are typically dispatched in a round-robin schedule.
Detailed monitoring of these applications (esp. if talking about custom metrics such as Prometheus supports) is considered "an implementation detail".

These two distinct worlds do not work together properly, due to the following obstacles:
* CF apps are only reachable (from externally without having CF administrative privileges, which is not common on PaaS offerings) 
  via official, world-reachable URLs. The cells, on which on which the CF apps are running, are typically protected by a firewall and the reverse proxy. 
  Developers do not have access via a side-channel to the cells, as this would bypass many security measures taken.
  
  This means that reading the metrics must be performed via the world-reachable URLs, i.e. going through the reverse proxy.
  As the endpoints for retrieving the Prometheus metrics will be world-reachable, and exposing such internal information will be a major security risk for
  such applications, the Prometheus metrics endpoints must be authentication-protected in some way. Often, this is done using OAuth2/JWT-based authentication,
  which is not supported (yet) by Prometheus natively.
* For providing failure-safety and scalability, Clound Foundry supports to run multiple instances of an application, which are registered to the same
  world-reachable URL (see also [Routing](https://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html)). As it is unpredictable for Prometheus
  to which instance a request to an endpoint is dispached by the reverse proxy, the metrics retrieved by the pulling mechanism will hit instances by random each time.
  Thus, a monitoring of all instances is not possible, yet alone a randomily-appearing set of metrics will be visible "once in a while" to Prometheus.

The Promregator wants to fix these both obstacles by providing a tool, which 
* at the one side behaves like a Prometheus client to a Prometheus server,
* while retrieving the source metrics from multiple instances running Prometheus metrics endpoints which are compliant to the Prometheus client protocol.

It tries to hide the complexity of the multi-instance approach provided by Cloud Foundry from both the operator and the Prometheus server.

## Architecture

Promregator is sitting between your Prometheus server on the one hand and talks to your Cloud Foundry apps on the other hand. 
It is converting the Prometheus' pull requests into queries targeting your instances of your apps which are running on Cloud Foundry. 
For Prometheus, Promregator feels like a single client, whilst Promregator may request multiple targets.

## Prerequisites

(Compiling and) Running Promregator requires:
* JDK8 or higher
* Maven 3.3.0 or higher

Further dependencies required are automatically loaded when building the software. An internet connection to [Maven Central](https://search.maven.org/) is necessary for that.

## Installation

Sorry, but there is currently no released binary package available yet, as this project is still in alpha phase.
Howevery, you may create your own package by downloading the source code, unpacking it into a local folder and calling

```bash
mvn clean package
```

The runnable artifact will be available at `target/promregator-0.0.1-SNAPSHOT.jar`. It is a self-contained (e.g. including all library dependencies) JAR file, 
which can be copied around and executed at an arbitrary folder by calling

```bash
java -jar promregator-0.0.1-SNAPSHOT.jar
```

## Configuration (of Promregator)

The configuration may be performed using any variant of the [Spring Property Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) 
approach. 

The suggested approach is to create a configuration YAML file, such as `myconfig.yaml`, and start Promregator with the following command line option:

```bash
java -Dspring.config.location=file:/path/to/your/myconfig.yaml -jar promregator-0.0.1-SNAPSHOT.jar
```

Here is an dummy example for such a configuration yaml file:
```yaml
cf:
  apiHost: api.cf.example.org
  username: myCFUserName
  proxyHost: 192.168.111.1
  proxyPort: 8080

promregator:
  authenticator:
    type: OAuth2XSUAA
    oauth2xsuaa:
      tokenServiceURL: https://jwt.token.server.example.org/oauth/token
      client_id: myOAuth2ClientId
    
  targets:
    - orgName: myCfOrgName
      spaceName: mySpaceName
      applicationName: myApplication1
      
    - orgName: myOtherCfOrgName
      spaceName: myOtherSpaceName
      applicationName: myOtherApplication
```

The documentation of the configuration options can be found [here](docs/config.md).


## Configuration of Prometheus Server
TODO!
[...] job-Tagging must be enabled -- trusted! 
Example excerpt from Prometheus config