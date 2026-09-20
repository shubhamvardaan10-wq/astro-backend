package com.astro.service;

import com.astro.model.BirthRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KundliVaultService {

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

    public KundliVaultService() {
        // Pre-populate demo profiles
        saveChart("self-108", "Native User", "Family", "1990-12-15", "14:30:00", "New Delhi");
        saveChart("spouse-109", "Beloved Partner", "Family", "1992-08-22", "09:15:00", "Mumbai");
    }

    public VaultItem saveChart(String id, String name, String category, String dob, String time, String city) {
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
        return item;
    }

    public List<VaultItem> listCharts() {
        return new ArrayList<>(vaultStore.values());
    }

    public Optional<VaultItem> getChart(String id) {
        return Optional.ofNullable(vaultStore.get(id));
    }

    public boolean deleteChart(String id) {
        return vaultStore.remove(id) != null;
    }
}
