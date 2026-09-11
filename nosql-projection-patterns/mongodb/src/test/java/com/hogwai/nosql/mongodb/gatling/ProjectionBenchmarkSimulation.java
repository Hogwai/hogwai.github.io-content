package com.hogwai.nosql.mongodb.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ProjectionBenchmarkSimulation extends Simulation {

    private final String baseUrl = System.getProperty("gatling.baseUrl", "http://localhost:8080");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/ProjectionBenchmark");

    // Scenario 1: Projection query (lightweight - only id, title, genre)
    private final ScenarioBuilder projectionScenario = scenario("Projection - MovieTitleView")
            .exec(
                    http("GET /api/mongo/movies (projection)")
                            .get("/api/mongo/movies?genre=Crime")
                            .check(status().is(200))
            );

    // Scenario 2: Full entity query (heavy - all fields + embedded actors)
    private final ScenarioBuilder fullEntityScenario = scenario("Full Entity - Movie")
            .exec(
                    http("GET /api/mongo/movies/full (full entity)")
                            .get("/api/mongo/movies/full?genre=Crime")
                            .check(status().is(200))
            );

    {
        setUp(
                projectionScenario.injectOpen(rampUsers(500).during(30)),
                fullEntityScenario.injectOpen(rampUsers(500).during(30))
        ).protocols(httpProtocol);
    }
}
