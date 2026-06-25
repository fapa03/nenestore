package com.nenestore.api.service;

import com.nenestore.api.entity.Client;
import com.nenestore.api.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> getClients(String search) {
        String s = (search != null && !search.isBlank()) ? search : null;
        return clientRepository.searchClients(s);
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found: " + id));
    }

    public Client createClient(Client client) {
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        if (client.getLevel() == null || client.getLevel().isBlank()) {
            client.setLevel("NEW");
        }
        return clientRepository.save(client);
    }

    public Client updateClient(Long id, Client updated) {
        Client existing = getClientById(id);
        existing.setName(updated.getName());
        existing.setWhatsapp(updated.getWhatsapp());
        existing.setEmail(updated.getEmail());
        existing.setInstagram(updated.getInstagram());
        existing.setLevel(updated.getLevel());
        existing.setNotes(updated.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        return clientRepository.save(existing);
    }
}