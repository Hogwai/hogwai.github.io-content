package com.hogwai.g1;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class G1WorkloadTest {

    @Test
    void logsDurationAndResultsOnZeroSeconds() {
        var logger = Logger.getLogger("com.hogwai.g1.G1Workload");
        var originalLevel = logger.getLevel();
        var handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);

        try {
            G1Workload.main(new String[]{"0"});

            var records = handler.getRecords();
            assertTrue(records.size() >= 2,
                    "Expected at least 2 log records (start + end), got " + records.size());

            var first = records.get(0);
            assertTrue(first.getMessage().contains("seconds"),
                    "First log should mention duration, got: " + first.getMessage());

            var last = records.get(records.size() - 1);
            assertTrue(last.getMessage().contains("Iterations"),
                    "Last log should mention Iterations, got: " + last.getMessage());
            assertTrue(last.getMessage().contains("Retained"),
                    "Last log should mention Retained, got: " + last.getMessage());
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
            logger.setUseParentHandlers(true);
        }
    }

    @Test
    void keepsRetentionInvariantsOverOneSecond() {
        G1Workload.WorkloadResult result = new G1Workload().run(1);

        assertTrue(result.iterations() > 0,
                "Expected a positive iteration count, got " + result.iterations());
        assertTrue(result.retainedObjects() <= 1280,
                "Retention exceeded cap: " + result.retainedObjects());
        assertEquals(0, result.retainedObjects() % 16,
                "Retained objects must be a multiple of 16, got " + result.retainedObjects());
    }

    static class CapturingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<LogRecord> getRecords() {
            return records;
        }
    }
}
