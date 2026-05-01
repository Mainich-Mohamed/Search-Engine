package com.myproject.search_engine.crawler.fetcher;

import com.myproject.search_engine.crawler.CrawlerQueueManager;
import com.myproject.search_engine.crawler.fetcher.parser.SitemapParser;
import com.myproject.search_engine.crawler.fetcher.records.SitemapEntry;
import lombok.AllArgsConstructor;
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
    private final static String SEEDS_CONF_PATH = "crawler/seeds.yml";

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
        HttpResponse<String> sitemapContent = sitemapParser.getSitemapContent(seed);

        String xmlBody = sitemapContent.body();
        InputStream stream = new ByteArrayInputStream(xmlBody.getBytes(StandardCharsets.UTF_8));

        if (sitemapParser.isXMLSitemap(sitemapContent)) {
            if (sitemapParser.isSitemapIndex(sitemapContent)) {
                List<SitemapEntry> sitemapsUrls = extractChildSitemaps(stream);

                try (Jedis redisConnection = crawlerQueueManager.connectToRedisServer()) {
                    // Cache the sitemap urls in the Memory
                    crawlerQueueManager.enqueueUniqueSitemapUrls(redisConnection, sitemapsUrls);
                } catch (Exception e) {
                    log.error("Failed to enqueue sitemaps in Redis: {}", e.getMessage());
                }
            } else if (sitemapParser.isUrlSet(sitemapContent)) {
                // Fetch Sitemap
                return new ArrayList<>();
            }
        }

        return Collections.emptyList();
    }

    // Allows us to parse the sitemap's urls from the sitemap index
    public List<SitemapEntry> extractChildSitemaps(InputStream sitemapStream) {
        List<SitemapEntry> sitemaps = new ArrayList<>();

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            // Prevent XXE Attacks
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(sitemapStream);

            boolean inSitemap = false;
            boolean inLoc = false;
            boolean inLastMod = false;

            StringBuilder currentUrl = new StringBuilder();
            StringBuilder currentLastMod = new StringBuilder();

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String startName = reader.getLocalName();
                        if ("sitemap".equals(startName)) {
                            inSitemap = true;
                            currentUrl.setLength(0);
                            currentLastMod.setLength(0);
                        } else if ("loc".equals(startName)) {
                            inLoc = true;
                        } else if ("lastmod".equals(startName)) {
                            inLastMod = true;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (inLoc) {
                            currentUrl.append(reader.getText());
                        } else if (inLastMod) {
                            currentLastMod.append(reader.getText());
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        String endName = reader.getLocalName();
                        if ("sitemap".equals(endName)) {
                            inSitemap = false;
                            String parsedUrl = currentUrl.toString().trim();
                            String parsedLastMod = currentLastMod.toString().trim();

                            if (!parsedUrl.isEmpty()) {
                                sitemaps.add(new SitemapEntry(parsedUrl, parsedLastMod));
                            }
                        } else if ("loc".equals(endName)) {
                            inLoc = false;
                        } else if ("lastmod".equals(endName)) {
                            inLastMod = false;
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
