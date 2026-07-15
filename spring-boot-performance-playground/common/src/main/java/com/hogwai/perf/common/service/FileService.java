package com.hogwai.perf.common.service;

import org.springframework.core.io.Resource;

public interface FileService {
    Resource getFile(String fileName);
    long getFileSize(String fileName);
}
