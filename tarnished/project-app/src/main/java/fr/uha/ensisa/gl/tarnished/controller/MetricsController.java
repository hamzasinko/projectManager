package fr.uha.ensisa.gl.tarnished.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.prometheus.PrometheusMeterRegistry;

@RestController
public class MetricsController {

    @Autowired
    private PrometheusMeterRegistry registry;

    @GetMapping("/metrics")
    public String metrics() {
        return registry.scrape();
    }
}