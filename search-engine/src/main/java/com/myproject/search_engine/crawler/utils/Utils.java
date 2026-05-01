package com.myproject.search_engine.crawler.utils;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// The purpose of this class is to provide helper methods
@Service
public class Utils {
    public <T extends Record & Serializable> byte[] recordToBytes(T record) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(record);
            oos.flush();
            return baos.toByteArray();
        }
    }
}
