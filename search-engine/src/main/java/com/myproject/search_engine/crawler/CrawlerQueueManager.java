package com.myproject.search_engine.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

@Slf4j
@Service
public class CrawlerQueueManager {
    private static final String SEEN_SITEMAP = "crawler:seen";
    public static final String SITEMAP_QUEUE = "crawler:queue";
    public static final String REDIS_HOST = "localhost";
    public static final int REDIS_PORT = 6379;

    public Jedis connectToRedisServer() {
        return new Jedis(REDIS_HOST, REDIS_PORT);
    }

    public void enqueueUniqueSitemapUrls(Jedis redis, List<String> sitemapsUrls) {
        for (String sitemapUrl : sitemapsUrls) {
            long isNew = redis.sadd(SEEN_SITEMAP, sitemapUrl);

            if (isNew == 1) {
                log.info("Saved new URL to Redis set [{}]: {}", SEEN_SITEMAP, sitemapUrl);

                redis.lpush(SITEMAP_QUEUE, sitemapUrl);

                log.info("Saved new URL to Redis list [{}]: {}", SITEMAP_QUEUE, sitemapUrl);
            } else {
                log.info("URL already exists in cache, skipping: {}", sitemapUrl);
            }
        }
    }
}
