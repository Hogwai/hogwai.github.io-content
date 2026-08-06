package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes the "full entity load vs targeted access" anti-pattern comparison.
 * <p>
 * Each pair of endpoints demonstrates a data access style — first the anti-pattern
 * (loading the full entity via the Enhanced Client), then the proper approach
 * (projected read or atomic update expression).
 */
@RestController
@RequestMapping("/api/data-access")
public class DataAccessController {

    private final MovieService service;

    public DataAccessController(MovieService service) {
        this.service = service;
    }

    /**
     * Load the entire Movie entity just to check whether the actors set is non-empty.
     */
    @GetMapping("/has-actors/bad")
    public Map<String, Object> hasActorsBad(@RequestParam String genre, @RequestParam String movieId) {
        return service.hasActorsBad(genre, movieId);
    }

    /**
     * Check whether the actors set is non-empty using a projection expression
     * to fetch only the {@code actors} attribute.
     */
    @GetMapping("/has-actors/good")
    public Map<String, Object> hasActorsGood(@RequestParam String genre, @RequestParam String movieId) {
        return service.hasActorsGood(genre, movieId);
    }

    /**
     * Add an actor via read-modify-write: load the full entity, mutate the actors
     * set in memory, then write the entire item back.
     */
    @PutMapping("/add-actor/bad")
    public Map<String, Object> addActorBad(@RequestParam String genre, @RequestParam String movieId,
                                            @RequestParam String actorName) {
        return service.addActorBad(genre, movieId, actorName);
    }

    /**
     * Add an actor atomically with an {@code ADD actors :newActor} update expression.
     * Single round-trip, no read-before-write, no lost update risk.
     */
    @PutMapping("/add-actor/good")
    public Map<String, Object> addActorGood(@RequestParam String genre, @RequestParam String movieId,
                                            @RequestParam String actorName) {
        return service.addActorGood(genre, movieId, actorName);
    }
}
