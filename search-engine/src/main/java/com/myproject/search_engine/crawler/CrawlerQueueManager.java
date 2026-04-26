package com.myproject.search_engine.crawler;

import redis.clients.jedis.Jedis;

import java.util.List;

public class CrawlerQueueManager {
    private static final String SEEN_SITEMAP = "crawler:seen";
    public static final String SITEMAP_QUEUE = "crawler:queue";
    public static final String REDIS_HOST = "localhost";
    public static final int REDIS_PORT = 6379;

    public Jedis connectToRedisServer() {
        return new Jedis(REDIS_HOST, REDIS_PORT);
    }

    public void enqueueUniqueSitemapUrls(Jedis connection, List<String> sitemaps) {
        for (String sitemap : sitemaps) {
            long isNew = connection.sadd(SEEN_SITEMAP, sitemap);

            if (isNew == 1) {
                connection.lpush(SITEMAP_QUEUE, sitemap);
            }
        }
    }
}
