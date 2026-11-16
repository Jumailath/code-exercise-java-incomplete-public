package com.urlshortener.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlPageTest {

    private static final UrlListItem ABC = item("abc");
    private static final UrlListItem DEF = item("def");

    private static UrlListItem item(String alias) {
        return new UrlListItem(alias, "https://example.com/" + alias, "http://localhost:8080/" + alias);
    }

    @Test
    void map_copiesContentAndPagingFields() {
        Page<UrlListItem> page = new PageImpl<>(List.of(ABC, DEF), PageRequest.of(1, 2), 5);

        var urlPage = UrlPage.map(page);

        assertEquals(List.of(ABC, DEF), urlPage.urls());
        assertEquals(5, urlPage.totalElements());
        assertEquals(3, urlPage.totalPages());
        assertEquals(2, urlPage.size());
        assertEquals(1, urlPage.number());
    }

    @Test
    void map_pageBeyondLastPage_hasNoUrlsButKeepsTotals() {
        Page<UrlListItem> page = new PageImpl<>(List.of(), PageRequest.of(5, 2), 2);

        var urlPage = UrlPage.map(page);

        assertTrue(urlPage.urls().isEmpty());
        assertEquals(2, urlPage.totalElements());
        assertEquals(1, urlPage.totalPages());
        assertEquals(5, urlPage.number());
        assertEquals(2, urlPage.size());
    }

    @Test
    void map_nullPage_hasDefaultValues() {
        var urlPage = UrlPage.map(null);

        assertTrue(urlPage.urls().isEmpty());
        assertEquals(0, urlPage.totalElements());
        assertEquals(0, urlPage.totalPages());
        assertEquals(0, urlPage.number());
        assertEquals(0, urlPage.size());
    }
}
