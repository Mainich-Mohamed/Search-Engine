package com.myproject.search_engine.crawler;

import com.myproject.search_engine.crawler.fetcher.records.SitemapEntry;
import com.myproject.search_engine.crawler.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class CrawlerQueueManager {
    private static final String SITEMAP_LASTMOD_HASH = "crawler:sitemap:lastmod";
    public static final String SITEMAP_QUEUE = "crawler:queue";

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private final Utils utils;

    public CrawlerQueueManager(Utils utils) {
        this.utils = utils;
    }

    public Jedis connectToRedisServer() {
        return new Jedis(redisHost, redisPort);
    }

    public void enqueueUniqueSitemapUrls(Jedis redis, List<SitemapEntry> sitemapsEntries) throws IOException {
        for (SitemapEntry sitemapEntry : sitemapsEntries) {
            String storedLastMod = redis.hget(SITEMAP_LASTMOD_HASH, sitemapEntry.url());
            String currentLastMod = sitemapEntry.lastModified();

            if (storedLastMod == null || !storedLastMod.equals(currentLastMod)) {
                redis.hset(SITEMAP_LASTMOD_HASH, sitemapEntry.url(), currentLastMod != null ? currentLastMod : "");

                byte[] serializedSitemapEntry = utils.recordToBytes(sitemapEntry);
                redis.lpush(SITEMAP_QUEUE.getBytes(), serializedSitemapEntry);
            }
        }
    }
}
