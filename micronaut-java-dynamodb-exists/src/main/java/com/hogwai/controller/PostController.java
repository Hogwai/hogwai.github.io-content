package com.hogwai.controller;

import com.hogwai.service.PostService;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

@Controller("/post")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PostController {
    private final PostService postService;

    public PostController(PostService redditPostService) {
        this.postService = redditPostService;
    }

    @Get("/projection/{subreddit}/exists")
    public boolean exists(@PathVariable String subreddit, @QueryValue String id) {
        return postService.existsByProjection(subreddit, id);
    }

    @Get("/get-item/{subreddit}/exists")
    public boolean existsWithGetItem(@PathVariable String subreddit, @QueryValue String id) {
        return postService.existsByGetItem(subreddit, id);
    }

    @Get("/projection/{subreddit}/has-posts")
    public boolean hasPosts(@PathVariable String subreddit) {
        return postService.hasPostsForSubreddit(subreddit);
    }

    @Get("/{id}/has-keywords")
    public boolean hasKeywords(@PathVariable String id, @QueryValue String subreddit) {
        return postService.hasKeywords(subreddit, id);
    }
}
