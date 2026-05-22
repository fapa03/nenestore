package com.nenestore.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class ImageService {

    @Value("${storage.images-path}")
    private String imagesPath;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String downloadImage(String imageUrl, String sku) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            Path directory = Paths.get(imagesPath);
            Files.createDirectories(directory);

            String filename = sku + ".jpg";
            Path destination = directory.resolve(filename);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                Files.copy(response.body(), destination,
                        StandardCopyOption.REPLACE_EXISTING);
                return "/images/items/" + filename;
            }

            return null;

        } catch (IOException | InterruptedException e) {
            System.err.println("Image download failed for SKU " + sku + ": " + e.getMessage());
            return null;
        }
    }
}