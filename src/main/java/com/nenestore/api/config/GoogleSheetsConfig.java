package com.nenestore.api.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleSheetsConfig {

    @Bean
    public Sheets sheetsService() throws IOException, GeneralSecurityException {
        ClassPathResource resource = new ClassPathResource("sheetsCredentials.json");

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(resource.getInputStream())
                .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("nenestore-api")
                .build();
    }
}