package com.myproject.search_engine;

import com.myproject.search_engine.crawler.fetcher.SeedContentFetcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SearchEngineApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(SearchEngineApplication.class, args);

        String reutersSitemapUrl = "https://www.reuters.com/arc/outboundfeeds/sitemap-index/?outputType=xml";

        SeedContentFetcher seedContentFetcher = context.getBean(SeedContentFetcher.class);

        seedContentFetcher.fetchXMLSitemap(reutersSitemapUrl);
    }
}