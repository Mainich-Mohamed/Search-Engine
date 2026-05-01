package com.myproject.search_engine.crawler.fetcher.records;

import java.io.Serializable;

public record SitemapEntry(String url, String lastModified) implements Serializable { }