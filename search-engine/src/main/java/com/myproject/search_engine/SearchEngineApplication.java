package com.myproject.search_engine;

import com.myproject.search_engine.crawler.fetcher.SeedContentFetcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SearchEngineApplication {

    public static void main(String[] args) {
        // 1. Capture the context returned by Spring
        ApplicationContext context = SpringApplication.run(SearchEngineApplication.class, args);

        String reutersSitemapUrl = "https://www.reuters.com/arc/outboundfeeds/sitemap-index/?outputType=xml";

        // 2. Ask Spring to give you the fully built, fully configured fetcher
        SeedContentFetcher seedContentFetcher = context.getBean(SeedContentFetcher.class);

        // 3. Run your test!
        seedContentFetcher.fetchXMLSitemap(reutersSitemapUrl);
    }
}