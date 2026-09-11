package com.hogwai.nosql.cassandra.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ProjectionBenchmarkSimulation extends Simulation {

    private final String baseUrl = System.getProperty("gatling.baseUrl", "http://localhost:8081");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/ProjectionBenchmark");

    // Scenario 1: Projection query (lightweight - only id, title, genre)
    private final ScenarioBuilder projectionScenario = scenario("Projection - SELECT id, title, genre")
            .exec(
                    http("GET /api/cassandra/movies (projection)")
                            .get("/api/cassandra/movies?genre=Crime")
                            .check(status().is(200))
            );

    // Scenario 2: Full entity query (heavy - SELECT *)
    private final ScenarioBuilder fullEntityScenario = scenario("Full Entity - SELECT *")
            .exec(
                    http("GET /api/cassandra/movies/full (full entity)")
                            .get("/api/cassandra/movies/full?genre=Crime")
                            .check(status().is(200))
            );

    {
        setUp(
                projectionScenario.injectOpen(rampUsers(500).during(30)),
                fullEntityScenario.injectOpen(rampUsers(500).during(30))
        ).protocols(httpProtocol);
    }
}
