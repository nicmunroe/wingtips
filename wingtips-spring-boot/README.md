# Wingtips - spring-boot

Wingtips is a distributed tracing solution for Java based on the 
[Google Dapper paper](http://static.googleusercontent.com/media/research.google.com/en/us/pubs/archive/36356.pdf). 

This module is a plugin extension module of the core Wingtips library and contains support for distributed tracing in a 
[Spring Boot](https://spring.io/guides/gs/spring-boot/) environment.

## Usage Examples

NOTES:

* The [Wingtips Spring Boot sample project](../samples/sample-spring-boot) shows these features in action.
* All of the features of the [wingtips-spring](../wingtips-spring) module are relevant to a Spring Boot project as 
well - please see that module's readme for more info on its features.
* More details can be found in the javadocs for the various classes found in this `wingtips-spring-boot` module.

### Utilizing `WingtipsSpringBootConfiguration` in a Spring Boot application to configure and setup Wingtips tracing

Ensure that the following `@Configuration` gets registered in your Spring Boot app's `ApplicationContext`:

``` java
@Configuration
@Import(WingtipsSpringBootConfiguration.class)
public class MyAppSpringConfig {
}
``` 

And (optionally) specify configuration from your Spring Boot app's `application.properties`:

``` ini
wingtips.wingtips-disabled=false
wingtips.user-id-header-keys=userid,altuserid
wingtips.span-logging-format=KEY_VALUE
wingtips.server-side-span-tag-strategy=ZIPKIN
```

## Feature details

This `wingtips-spring-boot` module contains the following features/classes:

* **`WingtipsSpringBootConfiguration`** - A Spring `@Configuration` bean that uses `@EnableConfigurationProperties` to
pull in `WingtipsSpringBootProperties` (described below) and uses those properties to set up the following Wingtips 
features:
    - Registers the `RequestTracingFilter` servlet filter with Spring, which handles the incoming-request side of 
    distributed tracing for you automatically. See the description in the
    [wingtips-servlet-api](../wingtips-servlet-api) module for more details. You can provide your own 
    `RequestTracingFilter` to override the default one if needed - simply register the one you want with Spring's 
    `ApplicationContext` (e.g. via `@Bean public RequestTracingFilter filterOverride() {...}` method) and 
    `WingtipsSpringBootConfiguration` will use that one instead of creating a new default one.
    - Sets the span logging representation used by Wingtips to whatever you specify in your 
    `wingtips.span-logging-format` application property (see `WingtipsSpringBootProperties` description below).
    - The `RequestTracingFilter` uses a `ServletRequestTagStrategy` to implement `OpenTracing` tags for `SERVER` 
    requests.  To modify the tag strategy, supply a `@Bean` of type 
    `HttpRequestResponseTagStrategy<HttpServletRequest, HttpServletResponse>` and it will be injected into the 
    `RequestTracingFilter`. 
* **`WingtipsSpringBootProperties`** - The Spring Boot 
[@ConfigurationProperties](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-typesafe-configuration-properties) 
companion for `WingtipsSpringBootConfiguration` (described above) that allows you to customize some Wingtips behaviors 
from your Spring Boot application's properties files. See the 
[Spring Boot sample application's application.properties](../samples/sample-spring-boot/src/main/resources/application.properties) 
for a concrete example. The following properties are supported (all of them optional):
    - **`wingtips.wingtips-disabled`** - Disables the Wingtips `RequestTracingFilter` servlet filter if and only if 
    this property value is set to true. If false or missing then `RequestTracingFilter` will be registered normally.
    - **`wingtips.user-id-header-keys`** - Used to specify the user ID header keys that Wingtips will look for on 
    incoming headers. See `RequestTracingFilter.USER_ID_HEADER_KEYS_LIST_INIT_PARAM_NAME` for more info. This is 
    optional - if not specified then `RequestTracingFilter` will not extract user ID from incoming request headers but 
    will otherwise function properly.
    - **`wingtips.span-logging-format`** - Determines the format Wingtips will use when logging spans. Represents the 
    `Tracer.SpanLoggingRepresentation` enum. Must be either `JSON` or `KEY_VALUE`. If missing then the span logging 
    format will not be changed (defaults to `JSON`).     

For general Wingtips information please see the [base project README.md](../README.md).

## NOTE - `org.springframework:spring-web` and `org.springframework.boot:spring-boot-autoconfigure` dependencies required at runtime

This module does not export any transitive Spring or Spring Boot dependencies to prevent version conflicts with 
whatever Spring Boot environment you're running in. 

This should not affect most users since this library is likely to be used in a Spring Boot environment where the 
`spring-web` and `spring-boot-autoconfigure` dependencies are already on the classpath at runtime, however if you 
receive class-not-found errors related to classes found in `spring-web` or `spring-boot-autoconfigure` then 
you'll need to pull the `org.springframework:spring-web` and/or `org.springframework.boot:spring-boot-autoconfigure` 
dependencies into your project. Library authors who wish to build on functionality in this module might need to do 
this.

This module was built using version `4.3.7.RELEASE` of `spring-web`, and version `1.5.2.RELEASE` of 
`spring-boot-autoconfigure`, but many other versions of Spring and Spring Boot should work fine, both older and newer.
