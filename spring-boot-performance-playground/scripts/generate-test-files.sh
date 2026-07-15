#!/bin/bash
mkdir -p /tmp/perf-files
dd if=/dev/urandom of=/tmp/perf-files/1MB.dat bs=1M count=1 2>/dev/null
dd if=/dev/urandom of=/tmp/perf-files/10MB.dat bs=1M count=10 2>/dev/null
dd if=/dev/urandom of=/tmp/perf-files/100MB.dat bs=1M count=100 2>/dev/null
echo "Test files generated in /tmp/perf-files/"
ls -lh /tmp/perf-files/
