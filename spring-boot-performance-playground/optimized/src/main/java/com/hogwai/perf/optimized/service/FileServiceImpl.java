package com.hogwai.perf.optimized.service;

import com.hogwai.perf.common.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import java.nio.file.Path;

@Service
public class FileServiceImpl implements FileService {

    private static final Path FILES_DIR = Path.of("/tmp/perf-files");

    @Override
    public Resource getFile(String fileName) {
        try {
            var file = FILES_DIR.resolve(fileName).normalize();
            if (!file.startsWith(FILES_DIR)) {
                throw new SecurityException("Path traversal detected: " + fileName);
            }
            return new UrlResource(file.toUri());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + fileName, e);
        }
    }

    @Override
    public long getFileSize(String fileName) {
        try {
            return FILES_DIR.resolve(fileName).toFile().length();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file size: " + fileName, e);
        }
    }
}
