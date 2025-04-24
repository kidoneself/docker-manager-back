package com.dsm.service;

import java.util.List;
import java.util.Map;

public interface DockerAuthService {

    boolean login(String registry, String username, String password);

    boolean logout(String registry);

    boolean isLoggedIn(String registry);

    List<Map<String, String>> getRegistries();

    void addRegistry(String registry, String username, String password);

    void deleteRegistry(String registry);
} 