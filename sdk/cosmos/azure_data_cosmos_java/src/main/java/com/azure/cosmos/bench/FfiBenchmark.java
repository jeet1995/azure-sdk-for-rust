// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.bench;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.strategy.CosmosClientStrategy;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing JNI vs JNA per-call FFI overhead.
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar -p backend=jni,jna}</p>
 *
 * <p>This isolates FFI cost from network RTT by measuring the full
 * createItem/readItem round-trip including Cosmos DB latency,
 * then comparing the delta between JNI and JNA paths.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class FfiBenchmark {

    @Param({"jni", "jna"})
    private String backend;

    private CosmosClientStrategy client;

    private static final String DATABASE = "benchDb";
    private static final String CONTAINER = "benchContainer";
    private static final String PARTITION_KEY = "benchPk";
    private static final String ITEM_JSON = "{\"id\":\"bench-item\",\"pk\":\"benchPk\",\"data\":\"hello\"}";
    private static final String ITEM_ID = "bench-item";

    @Setup(Level.Trial)
    public void setup() throws CosmosException {
        String endpoint = System.getProperty("cosmos.endpoint", "https://localhost:8081");
        String key = System.getProperty("cosmos.key", "");
        System.setProperty("libcosmos.backend", backend);
        client = CosmosClientStrategy.create(endpoint, key);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        if (client != null) {
            client.close();
        }
    }

    @Benchmark
    public void createItem() throws CosmosException {
        client.createItem(DATABASE, CONTAINER, PARTITION_KEY, ITEM_JSON);
    }

    @Benchmark
    public CosmosItemResponse readItem() throws CosmosException {
        return client.readItem(DATABASE, CONTAINER, ITEM_ID, PARTITION_KEY);
    }
}
