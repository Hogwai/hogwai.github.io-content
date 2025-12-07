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

    @Get("/{subreddit}/exists")
    public boolean exists(@PathVariable String subreddit, @QueryValue String id) {
        return postService.exists(subreddit, id);
    }

    @Get("/{subreddit}/has-posts")
    public boolean hasPosts(@PathVariable String subreddit) {
        return postService.hasPostsForSubreddit(subreddit);
    }
}
