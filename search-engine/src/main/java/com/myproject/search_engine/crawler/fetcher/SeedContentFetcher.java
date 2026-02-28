package com.myproject.search_engine.crawler.fetcher;

import com.myproject.search_engine.crawler.fetcher.parser.SitemapParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SeedContentFetcher {
    private final SitemapParser sitemapParser;

    public SeedContentFetcher(SitemapParser sitemapParser) {
        this.sitemapParser = sitemapParser;
    }

    public List<String> fetchAllXMLSitemaps(String seedsOrigin) {
        List<String> seeds = getSeedsFromYAML(seedsOrigin);

        for (String seed : seeds) {
            fetchXMLSitemap(seed);
        }
    }

    public List<String> fetchXMLSitemap(String seed) {
        HttpResponse<String> response = sitemapParser.getSitemapContent(seed);

        if (sitemapParser.isXMLSitemap(response)) {
            if (sitemapParser.isSitemapIndex(response)) {

            } else if (sitemapParser.isUrlSet(response)) {

            }
        }

        return Collections.emptyList();
    }

    public List<String> fetchSitemapIndex(HttpResponse<String> response) {

    }

    public List<String> getSeedsFromYAML(String seedsOrigin) {
        Yaml yaml = new Yaml();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("conf/seeds.yml")) {
            if (inputStream == null) {
                log.error("Could not find seeds.yml in classpath");
                return Collections.emptyList();
            }

            Map<String, Object> map = yaml.load(inputStream);

            return Optional.ofNullable(map)
                    .map(m -> (Map<String, Object>) m.get("crawler"))
                    .map(m -> (Map<String, Object>) m.get("seed"))
                    .map(m -> (Map<String, Object>) m.get("url"))
                    .map(m -> (List<String>) m.get(seedsOrigin))
                    .orElse(Collections.emptyList());
        } catch (IOException e) {
            log.error("Failed to read the YAML file: {}",e.getMessage());
            return Collections.emptyList();
        }
    }
}
}
