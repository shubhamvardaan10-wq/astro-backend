package com.astro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Durable Kundli Horoscope Vault Service.
 *
 * Persists horoscope profiles locally in a structured JSON database,
 * ensuring profiles survive application restarts without requiring external database clusters.
 */
@Service
public class KundliVaultService {

    private static final Logger log = LoggerFactory.getLogger(KundliVaultService.class);

    public record VaultItem(
        String id,
        String name,
        String category,
        String dob,
        String time,
        String city,
        String createdAt
    ) {}

    private final Map<String, VaultItem> vaultStore = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final Path storageFile;

    @Autowired
    public KundliVaultService(
            ObjectMapper mapper,
            @Value("${astro.vault.file:data/vault_profiles.json}") String storagePath) {
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.storageFile = Paths.get(storagePath).toAbsolutePath().normalize();
        loadFromDisk();
    }

    public KundliVaultService() {
        this(new ObjectMapper(), "data/vault_profiles.json");
    }

    public synchronized VaultItem saveChart(String id, String name, String category, String dob, String time, String city) {
        String profileId = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        VaultItem item = new VaultItem(
            profileId,
            name != null ? name : "Untitled Profile",
            category != null ? category : "Personal",
            dob,
            time,
            city,
            Instant.now().toString()
        );
        vaultStore.put(profileId, item);
        persistToDisk();
        return item;
    }

    public List<VaultItem> listCharts() {
        return new ArrayList<>(vaultStore.values());
    }

    public Optional<VaultItem> getChart(String id) {
        return Optional.ofNullable(vaultStore.get(id));
    }

    public synchronized boolean deleteChart(String id) {
        boolean removed = vaultStore.remove(id) != null;
        if (removed) {
            persistToDisk();
        }
        return removed;
    }

    private void loadFromDisk() {
        try {
            if (Files.isRegularFile(storageFile) && Files.size(storageFile) > 2) {
                List<VaultItem> items = mapper.readValue(storageFile.toFile(), new TypeReference<List<VaultItem>>() {});
                for (VaultItem item : items) {
                    if (item != null && item.id() != null) {
                        vaultStore.put(item.id(), item);
                    }
                }
                log.info("Loaded {} saved horoscope profiles from persistent vault at {}", vaultStore.size(), storageFile);
                return;
            }
        } catch (Exception e) {
            log.warn("Could not load vault profiles from {}: {}. Starting with demo defaults.", storageFile, e.getMessage());
        }

        // Seed with demo profiles on first initialization
        saveChart("self-108", "Native User", "Family", "1990-12-15", "14:30:00", "New Delhi");
        saveChart("spouse-109", "Beloved Partner", "Family", "1992-08-22", "09:15:00", "Mumbai");
    }

    private void persistToDisk() {
        try {
            if (storageFile.getParent() != null) {
                Files.createDirectories(storageFile.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), listCharts());
        } catch (IOException e) {
            log.error("Failed to persist horoscope vault to disk at {}: {}", storageFile, e.getMessage());
        }
    }
}
