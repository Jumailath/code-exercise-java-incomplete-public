package com.urlshortener.service;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UrlShortenerService {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("[a-zA-Z0-9-]{2,64}");

    private static final String ALIAS_CHARS ="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_ALIAS_LENGTH = 7;
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 10;
    private static final String MAX_ALIAS_GENERATION_ATTEMPTS_ERROR_MESSAGE = "Could not generate a unique alias after %s attempts.";

    private final ShortenedUrlRepository repository;

    public UrlShortenerService(ShortenedUrlRepository repository) {
        this.repository = repository;
    }

    public ShortenUrlResponse shorten(ShortenUrlRequest request, String baseUrl) {
        var fullUrl = normalizeUrl(request.getFullUrl());
        var alias = request.getCustomAlias() == null || request.getCustomAlias().isBlank()
                ? generateUniqueAlias()
                : request.getCustomAlias().trim();

        if (!isValidAlias(alias)) {
            throw new IllegalArgumentException(
                    "Alias may only contain letters (A–Z, a–z), numbers (0–9), and hyphens (2–64 characters).");
        }

        if (repository.existsByAlias(alias)) {
            throw new IllegalStateException("The alias '" + alias + "' is already taken.");
        }

        persistAlias(alias, fullUrl);

        return new ShortenUrlResponse(baseUrl + "/" + alias, alias, fullUrl);
    }

    private void persistAlias(String alias, String fullUrl) {
        try {
            repository.save(alias, fullUrl, Instant.now());
        }catch (Exception ex) {
            throw new IllegalStateException("Failed to shorten url for alias '" + alias);
        }
    }

    public String getFullUrl(String alias) {
        return repository.findFullUrlByAlias(alias).orElse(null);
    }

    public Page<UrlListItem> getAll(Pageable pageable, String baseUrl) {
        var items = repository.findAll(baseUrl, pageable.getPageSize(), pageable.getOffset());
        return new PageImpl<>(items, pageable, repository.count());
    }

    public boolean delete(String alias) {
        return repository.deleteByAlias(alias);
    }

    private String generateUniqueAlias() {
        return Stream.generate(UrlShortenerService::generateAlias)
                .limit(MAX_ALIAS_GENERATION_ATTEMPTS)
                .filter(alias -> !repository.existsByAlias(alias))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format(MAX_ALIAS_GENERATION_ATTEMPTS_ERROR_MESSAGE, MAX_ALIAS_GENERATION_ATTEMPTS)));
    }

    private static String generateAlias() {
        return ThreadLocalRandom.current()
                    .ints(GENERATED_ALIAS_LENGTH, 0, ALIAS_CHARS.length())
                    .mapToObj(i -> String.valueOf(ALIAS_CHARS.charAt(i)))
                    .collect(Collectors.joining());
    }

    private static boolean isValidAlias(String alias) {
        return alias != null && ALIAS_PATTERN.matcher(alias).matches();
    }

    private static String normalizeUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            throw new IllegalArgumentException("fullUrl is required.");
        }

        try {
            var uri = new URI(fullUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    path,
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("fullUrl must be a valid URL.");
        }
    }
}
