package com.myproject.search_engine.crawler.fetcher.parser;

import lombok.extern.slf4j.Slf4j;

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
public class SitemapParser {
    public boolean isXMLSitemap(HttpResponse<String> response) {
        if (response == null || response.body() == null || response.body().isBlank()) {
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

        XMLInputFactory factory =  XMLInputFactory.newInstance();

        try {
            // Prevents XXE Attacks
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(response.body()));

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamReader.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    return "urlset".equals(localName) || "sitemapindex".equals(localName);
                }
            }
            return false;
        } catch (XMLStreamException e) {
            log.error("Failed to parse XML: {}", e.getMessage());
            return false;
        }
    }

    public HttpResponse<String> getSitemapContent(URI addressURL) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(addressURL)
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());

            return response;
        } catch (IOException | InterruptedException e) {
            log.error("Response error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
