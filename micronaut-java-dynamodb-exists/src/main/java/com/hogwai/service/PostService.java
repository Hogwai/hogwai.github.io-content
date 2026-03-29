package com.hogwai.service;

import com.hogwai.repository.PostRepository;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

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

    public boolean hasKeywordsByGetItem(String subreddit, String id) {
        return postRepository.hasKeywordsByGetItem(subreddit, id);
    }

    public Map<String, Boolean> batchExists(String subreddit, List<String> ids) {
        return postRepository.batchExists(subreddit, ids);
    }
}
