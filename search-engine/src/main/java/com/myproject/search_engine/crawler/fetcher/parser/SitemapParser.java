package com.myproject.search_engine.crawler.fetcher.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class SitemapParser {
    // Check if the document is a sitemap of sitemaps
    public boolean isXMLSitemap(HttpResponse<String> response) {
        if (isValidResponse(response)) {
            return false;
        }

        boolean isXmlHeader = response.headers()
                .firstValue("Content-Type")
                .map(String::toLowerCase)
                .map(type -> type.contains("application/xml") || type.contains("text/xml"))
                .orElse(false);

        if (!isXmlHeader) {
            return false;
        }

        String rootElement = getLocalNameXML(response);
        return "urlset".equals(rootElement) || "sitemapindex".equals(rootElement);
    }

    // used to get the first XML tag name
    public String getLocalNameXML(HttpResponse<String> response) {
        XMLInputFactory factory =  XMLInputFactory.newInstance();

        try {
            // Prevents XXE Attacks
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(response.body()));

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamReader.START_ELEMENT) {
                    return reader.getLocalName();
                }
            }

            return null;
        } catch (XMLStreamException e) {
            log.error("Failed to parse XML: {}", e.getMessage());
            return null;
        }
    }


    public boolean isSitemapIndex(HttpResponse<String> response) {
        if (isValidResponse(response)) {
            return false;
        }

        return isXMLSitemap(response) && "sitemapindex".equals(getLocalNameXML(response));
    }

    public boolean isUrlSet(HttpResponse<String> response) {
        if (isValidResponse(response)) {
            return false;
        }

        return isXMLSitemap(response) && "urlset".equals(getLocalNameXML(response));
    }

    public boolean isValidResponse(HttpResponse<String> response) {
        return response == null || response.body() == null || response.body().isBlank();
    }

    public HttpResponse<String> getSitemapContent(String addressURL) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(addressURL))
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response;
        } catch (IOException | InterruptedException e) {
            log.error("Response error: {}", e.getMessage());
            return null;
        }
    }
}
