package com.myproject.search_engine.crawler.fetcher;

import com.myproject.search_engine.crawler.fetcher.parser.SitemapParser;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;

public class fetchContentFromSeed {
    private final SitemapParser  sitemapParser;

    public fetchContentFromSeed(SitemapParser sitemapParser) {
        this.sitemapParser = sitemapParser;
    }
    public List<URI> fetchXMLSitemap() {
        HttpResponse<String> response = sitemapParser.getSitemapContent();

        if (sitemapParser.isXMLSitemap(response)) {
            // Logic to fetch urls
        }
    }
}
