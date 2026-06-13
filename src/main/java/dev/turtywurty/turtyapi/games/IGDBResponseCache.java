package dev.turtywurty.turtyapi.games;

import com.api.igdb.exceptions.RequestException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class IGDBResponseCache {
    private final int maximumSize;
    private final long ttlNanos;
    private final LongSupplier nanoTime;
    private final Map<String, CacheEntry> entries;
    private final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    public IGDBResponseCache(int maximumSize, Duration ttl) {
        this(maximumSize, ttl, System::nanoTime);
    }

    public IGDBResponseCache(int maximumSize, Duration ttl, LongSupplier nanoTime) {
        this.maximumSize = Math.max(0, maximumSize);
        this.ttlNanos = Math.max(0, ttl.toNanos());
        this.nanoTime = nanoTime;
        this.entries = new LinkedHashMap<>(16, 0.75F, true);
    }

    public String getOrLoad(String key, RequestLoader loader) throws RequestException {
        if (!isEnabled()) {
            return loader.load();
        }

        String cached = get(key);
        if (cached != null) {
            this.hits.incrementAndGet();
            return cached;
        }

        this.misses.incrementAndGet();
        var loading = new CompletableFuture<String>();
        CompletableFuture<String> existing = this.inFlight.putIfAbsent(key, loading);
        if (existing != null) {
            return await(existing);
        }

        try {
            String value = loader.load();
            put(key, value);
            loading.complete(value);
            return value;
        } catch (RequestException | RuntimeException | Error exception) {
            loading.completeExceptionally(exception);
            throw exception;
        } finally {
            this.inFlight.remove(key, loading);
        }
    }

    public synchronized void clear() {
        this.entries.clear();
    }

    public synchronized CacheStats stats() {
        removeExpired();
        return new CacheStats(
                this.hits.get(),
                this.misses.get(),
                this.evictions.get(),
                this.entries.size()
        );
    }

    private boolean isEnabled() {
        return this.maximumSize > 0 && this.ttlNanos > 0;
    }

    private synchronized String get(String key) {
        CacheEntry entry = this.entries.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.expiresAtNanos() <= this.nanoTime.getAsLong()) {
            this.entries.remove(key);
            return null;
        }

        return entry.value();
    }

    private synchronized void put(String key, String value) {
        this.entries.put(key, new CacheEntry(value, this.nanoTime.getAsLong() + this.ttlNanos));
        while (this.entries.size() > this.maximumSize) {
            String eldestKey = this.entries.keySet().iterator().next();
            this.entries.remove(eldestKey);
            this.evictions.incrementAndGet();
        }
    }

    private synchronized void removeExpired() {
        long now = this.nanoTime.getAsLong();
        this.entries.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= now);
    }

    private static String await(CompletableFuture<String> future) throws RequestException {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RequestException requestException) {
                throw requestException;
            }

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw exception;
        }
    }

    @FunctionalInterface
    public interface RequestLoader {
        String load() throws RequestException;
    }

    public record CacheStats(long hits, long misses, long evictions, int size) {
    }

    private record CacheEntry(String value, long expiresAtNanos) {
    }
}
