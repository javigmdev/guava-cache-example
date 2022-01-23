package com.java.guavacache;

import com.google.common.cache.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class GuavaCacheTest {
    @Test
    public void whenCacheMiss_thenValueIsComputed() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(loader);
        
        assertEquals(0, cache.size());
        assertEquals("HELLO", cache.getUnchecked("hello"));
        assertEquals(1, cache.size());
    }

    @Test
    public void whenCacheReachMaxSize_thenEviction() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key)  {
                return key.toUpperCase();
            }
        };
        
        LoadingCache<String, String> cache = CacheBuilder.newBuilder().maximumSize(3).build(loader);
        cache.getUnchecked("first");
        cache.getUnchecked("second");
        cache.getUnchecked("third");
        cache.getUnchecked("forth");
        
        assertEquals(3, cache.size());
        assertNull(cache.getIfPresent("first"));
        assertEquals("FORTH", cache.getIfPresent("forth"));
    }

    @Test
    public void whenEntryIdle_thenEviction() throws InterruptedException {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };
        
        LoadingCache<String, String > cache = CacheBuilder.newBuilder()
                .expireAfterAccess(2, TimeUnit.MILLISECONDS)
                .build(loader);
        
        cache.getUnchecked("hello");
        assertEquals(1, cache.size());
        cache.getUnchecked("hello");
        Thread.sleep(300);
        cache.getUnchecked("test");
        assertEquals(1, cache.size());
        assertNull(cache.getIfPresent("hello"));
    }

    @Test
    public void whenEntryLiveTimeExpire_thenEviction() throws InterruptedException {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String > cache = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MILLISECONDS)
                .build(loader);
        
        cache.getUnchecked("hello");
        assertEquals(1, cache.size());
        Thread.sleep(300);
        cache.getUnchecked("test");
        assertEquals(1, cache.size());
        assertNull(cache.getIfPresent("hello"));
    }

    @Test
    public void whenWeakKeyHasNoRef_thenRemoveFromCache() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder().weakKeys().build(loader);
    }

    @Test
    public void whenSoftValue_thenRemoveFromCache() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder().softValues().build(loader);
    }

    @Test
    public void whenNullValue_thenOptional() {
        CacheLoader<String, Optional<String>> loader = new CacheLoader<>() {
            @Override
            public Optional<String> load(String key) {
                return Optional.ofNullable(getSuffix(key));
            }
        };

        LoadingCache<String, Optional<String>> cache = CacheBuilder.newBuilder().build(loader);
        
        assertEquals("txt", cache.getUnchecked("text.txt").get());
        assertFalse(cache.getUnchecked("hello").isPresent());
    }

    private String getSuffix(final String str) {
        int lastIndex = str.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return str.substring(lastIndex + 1);
    }

    @Test
    public void whenLiveTimeEnd_thenRefresh() {
//        Manual refresh
//        String value = loadingCache.get("key");
//        loadingCache.refresh("key");
        
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(1,TimeUnit.MINUTES)
                .build(loader);
    }

    @Test
    public void whenPreloadCache_thenUsePutAll() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(loader);

        Map<String, String> map = new HashMap<>();
        map.put("first", "FIRST");
        map.put("second", "SECOND");
        cache.putAll(map);
        
        assertEquals(2, cache.size());
    }

    @Test
    public void whenEntryRemovedFromCache_thenNotify() {
        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return key.toUpperCase();
            }
        };

        RemovalListener<String, String> listener = n -> {
            if (n.wasEvicted()) {
                String cause = n.getCause().name();
                assertEquals(RemovalCause.SIZE.toString(),cause);
            }
        };
        
        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(3)
                .removalListener(listener)
                .build(loader);

        cache.getUnchecked("first");
        cache.getUnchecked("second");
        cache.getUnchecked("third");
        cache.getUnchecked("last");
        assertEquals(3, cache.size());
    }
}