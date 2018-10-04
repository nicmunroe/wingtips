package com.nike.wingtips.springboot.componenttest.componentscanonly;

import com.nike.wingtips.springboot.WingtipsSpringBootProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.servlet.TracingFilter;
import brave.spring.webmvc.SpanCustomizingAsyncHandlerInterceptor;

@SpringBootApplication
@ComponentScan(basePackages = "com.nike")
@Import(SpanCustomizingAsyncHandlerInterceptor.class)
@RestController
public class ComponentTestMainWithComponentScanOnly extends WebMvcConfigurerAdapter {

    @Autowired
    private SpanCustomizingAsyncHandlerInterceptor tracingInterceptor;

    public ComponentTestMainWithComponentScanOnly() {
    }

//    @Bean
//    @Primary
//    public WingtipsSpringBootProperties wingtipsSpringBootProperties() {
//        WingtipsSpringBootProperties props = new WingtipsSpringBootProperties();
//        props.setWingtipsDisabled("true");
//        return props;
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(tracingInterceptor);
//    }
//
//
//    @Bean
//    public FilterRegistrationBean zipkinTracingFilter() {
//        Filter zipkinTracer = TracingFilter.create(HttpTracing.create(Tracing.newBuilder().build()));
//
//        FilterRegistrationBean frb = new FilterRegistrationBean(zipkinTracer);
//        // Set the order so that the tracing filter is registered first
//        frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        return frb;
//    }

    @GetMapping(path = "/foo/{bar}")
    public String foo(@PathVariable String bar) {
        return "bar=" + bar;
    }

    @RequestMapping(value = "/modules/{moduleBaseName}/**", method = RequestMethod.GET)
    public String moduleStrings(@PathVariable String moduleBaseName, HttpServletRequest request) {
        final String path =
            request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = moduleBaseName + '/' + arguments;
        } else {
            moduleName = moduleBaseName;
        }

        return "module name is: " + moduleName;
    }
}
