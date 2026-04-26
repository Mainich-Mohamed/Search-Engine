package com.myproject.search_engine.crawler.fetcher;

import com.myproject.search_engine.crawler.CrawlerQueueManager;
import com.myproject.search_engine.crawler.fetcher.parser.SitemapParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class SeedContentFetcher {
    private final static String SEEDS_CONF_PATH = "conf/seeds.yml";

    private final SitemapParser sitemapParser;
    private final CrawlerQueueManager crawlerQueueManager;

    public SeedContentFetcher(SitemapParser sitemapParser, CrawlerQueueManager crawlerQueueManager) {
        this.sitemapParser = sitemapParser;
        this.crawlerQueueManager = crawlerQueueManager;
    }

    public void fetchAllXMLSitemaps(String seedsOrigin) {
        List<String> seeds = getSeedsFromYAML(seedsOrigin);

        for (String seed : seeds) {
            fetchXMLSitemap(seed);
        }
    }

    public List<String> fetchXMLSitemap(String seed) {
        HttpResponse<String> response = sitemapParser.getSitemapContent(seed);

        String xmlBody = response.body();
        InputStream stream = new ByteArrayInputStream(xmlBody.getBytes(StandardCharsets.UTF_8));

        if (sitemapParser.isXMLSitemap(response)) {
            if (sitemapParser.isSitemapIndex(response)) {
                List<String> sitemaps = extractChildSitemaps(stream);

                try (Jedis redisConnection = crawlerQueueManager.connectToRedisServer()) {
                    // Cache the sitemap urls in the Memory
                    crawlerQueueManager.enqueueUniqueSitemapUrls(redisConnection, sitemaps);
                } catch (Exception e) {
                    log.error("Failed to enqueue sitemaps in Redis: {}", e.getMessage());
                }
            } else if (sitemapParser.isUrlSet(response)) {
                // Fetch Sitemap
            }
        }

        return Collections.emptyList();
    }

    public List<String> extractChildSitemaps(InputStream sitemapStream) {
        List<String> sitemaps = new ArrayList<>();

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            // Prevent XXE Attacks
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(sitemapStream);

            boolean inSitemap = false;
            boolean inLoc = false;

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String startName = reader.getLocalName();
                        if ("sitemap".equals(startName)) {
                            inSitemap = true;
                        } else if ("loc".equals(startName)) {
                            inLoc = true;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (inLoc) {
                            String url = reader.getText().trim();
                            if (!url.isEmpty()) {
                                sitemaps.add(url);
                            }
                        }
                        break;
                        case XMLStreamConstants.END_ELEMENT:
                            if (inSitemap) {
                                inSitemap = false;
                            } else if (inLoc) {
                                inLoc = false;
                            }
                        break;
                }
            }

            return sitemaps;
        } catch (XMLStreamException e) {
            log.error("Error while parsing XML Sitemap Index content: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> getSeedsFromYAML(String seedsOrigin) {
        Yaml yaml = new Yaml();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SEEDS_CONF_PATH)) {
            if (inputStream == null) {
                log.error("Could not find seeds.yml in classpath");
                return Collections.emptyList();
            }

            Map<String, Object> map = yaml.load(inputStream);

            return Optional.ofNullable(map)
                    .map(m -> (Map<String, Object>) m.get("crawler"))
                    .map(m -> (Map<String, Object>) m.get ("seed"))
                    .map(m -> (Map<String, Object>) m.get("url"))
                    .map(m -> (List<String>) m.get(seedsOrigin))
                    .orElse(Collections.emptyList());
        } catch (IOException e) {
            log.error("Failed to read the YAML file: {}",e.getMessage());
            return Collections.emptyList();
        }
    }
}
