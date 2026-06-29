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
        if (imageUrl == null || imageUrl.isBlank())
            return null;

        try {
            Path directory = Paths.get(imagesPath + "items/");
            Files.createDirectories(directory);

            String filename = sku + ".jpg";
            Path destination = directory.resolve(filename);

            System.out.println(">>> Downloading: " + imageUrl);
            System.out.println(">>> Saving to: " + destination.toAbsolutePath());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            System.out.println(">>> HTTP status: " + response.statusCode());

            if (response.statusCode() == 200) {
                Files.copy(response.body(), destination,
                        StandardCopyOption.REPLACE_EXISTING);
                System.out.println(">>> Saved successfully: " + filename);
                return "/images/items/" + filename;
            }

            System.out.println(">>> Download failed with status: " + response.statusCode());
            return null;

        } catch (IOException | InterruptedException e) {
            System.err.println(">>> EXCEPTION for SKU " + sku + ": " + e.getMessage());
            return null;
        }
    }

    public boolean imageExistsOnDisk(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank())
            return false;
        // imageUrl is like /images/items/YLA4509595-001.jpg
        // imagesPath is like ./images/
        // so we extract just the filename
        String filename = imageUrl.replace("/images/items/", "");
        Path filePath = Paths.get(imagesPath + "items/" + filename);
        return Files.exists(filePath);
    }

}