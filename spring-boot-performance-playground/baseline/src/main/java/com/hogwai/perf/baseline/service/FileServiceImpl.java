package com.hogwai.perf.baseline.service;

import com.hogwai.perf.common.service.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.nio.file.Path;

@Service
public class FileServiceImpl implements FileService {

    private static final Path FILES_DIR = Path.of("/tmp/perf-files");

    @Override
    public Resource getFile(String fileName) {
        var file = FILES_DIR.resolve(fileName).toFile();
        return new FileSystemResource(file);
    }

    @Override
    public long getFileSize(String fileName) {
        return FILES_DIR.resolve(fileName).toFile().length();
    }
}
