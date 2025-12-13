package com.hogwai.service;

import com.hogwai.repository.PostRepository;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public boolean existsByProjection(String subreddit, String id) {
        return postRepository.existsByProjection(subreddit, id);
    }

    public boolean existsByGetItem(String subreddit, String id) {
        return postRepository.existsByGetItem(subreddit, id);
    }
    public boolean hasPostsForSubreddit(String subreddit) {
        return postRepository.hasPostsForSubreddit(subreddit);
    }

    public boolean hasKeywords(String subreddit, String id) {
        return postRepository.hasKeywords(subreddit, id);
    }


}
