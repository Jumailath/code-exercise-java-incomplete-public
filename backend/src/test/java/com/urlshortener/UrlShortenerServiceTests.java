package com.urlshortener;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerServiceTests {

    private static final String BASE_URL = "http://localhost:8080";
    public static final String FULL_URL = "https://example.com";
    public static final String CUSTOM_ALIAS = "my-alias";
    public static final String CUSTOM_ALIAS1 = "example";
    private UrlShortenerService service;

    public static Stream<Arguments> validPageRequest() {
        //Ordered by created_at desc, alias will be in order ["alias5", "alias4", "alias3", "alias2", "alias1"]
        return Stream.of(Arguments.of(PageRequest.of(1,2), List.of("alias3", "alias2"), BASE_URL+"/alias3"),
                Arguments.of(PageRequest.of(1,1), List.of("alias4"), BASE_URL+"/alias4"),
                Arguments.of(PageRequest.of(0,5), List.of("alias5", "alias4", "alias3", "alias2", "alias1"), BASE_URL+"/alias5"),
                Arguments.of(PageRequest.of(1,3), List.of("alias2", "alias1"), BASE_URL+"/alias2"));
    }

    @BeforeEach
    void setUp() {
        var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        var jdbcTemplate = new JdbcTemplate(dataSource);
        var repository = new ShortenedUrlRepository(jdbcTemplate);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        service = new UrlShortenerService(repository);
    }

    @Test
    void shorten_WithValidUrl_ReturnsShortUrlResponse() {
        var request = buildRequest("https://example.com/test", null);

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertNotNull(response.getAlias());
        assertEquals("https://example.com/test", response.getFullUrl());
        assertTrue(response.getShortUrl().startsWith(BASE_URL + "/"));
    }

    @Test
    void shorten_WithCustomAlias_ReturnsCustomAlias() {
        var request = buildRequest(FULL_URL, CUSTOM_ALIAS);

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertEquals(CUSTOM_ALIAS, response.getAlias());
        assertEquals(BASE_URL + "/my-alias", response.getShortUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",
            "My-Alias-123",
            "a234567890123456789012345678901234567890123456789012345678901234"
    })
    void shorten_withValidAsciiAlias_isAccepted(String alias) {
        var response = service.shorten(buildRequest(FULL_URL, alias), BASE_URL);

        assertEquals(alias, response.getAlias());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab_c",
            "ab.c",
            "ab c",
            "ab/c",
            "a",
            "a2345678901234567890123456789012345678901234567890123456789012345"
    })
    void shorten_withAliasOutsideAsciiPattern_isRejectedAndNotSaved(String alias) {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.shorten(buildRequest(FULL_URL, alias), BASE_URL));

        assertTrue(ex.getMessage().contains("A–Z"));
        assertNull(service.getFullUrl(alias));
    }

    @Test
    void getFullUrl_ReturnsExistingUrl() {
        var request = buildRequest(FULL_URL, CUSTOM_ALIAS1);
        service.shorten(request, BASE_URL);

        String fullUrl = service.getFullUrl(CUSTOM_ALIAS1);

        assertEquals("https://example.com/", fullUrl);
    }

    @Test
    void delete_withValidAlias_returnsTrue() {
        var request = buildRequest(FULL_URL, CUSTOM_ALIAS);
        service.shorten(request, BASE_URL);

        assertNotNull(service.getFullUrl(CUSTOM_ALIAS));

        assertTrue(service.delete(CUSTOM_ALIAS));
    }

    @Test
    void delete_withInvalidAlias_returnsFalse() {
        assertFalse(service.delete(CUSTOM_ALIAS));
    }

    @ParameterizedTest
    @MethodSource("validPageRequest")
    void getAll_returnsRequestedPage(Pageable pageable, List<String> aliasList, String shortUrl) {
        shortenAliases("alias1", "alias2", "alias3", "alias4", "alias5");

        var page = service.getAll(pageable, BASE_URL);

        assertEquals(aliasList, page.getContent().stream().map(UrlListItem::getAlias).toList());
        assertEquals(5, page.getTotalElements());
        assertNotNull(page.getContent());
        assertEquals(shortUrl, page.getContent().get(0).getShortUrl());
    }

    @Test
    void getAll_sortedByCreatedAtDesc_returnsNewestFirst() {
        shortenAliases("alias1", "alias2", "alias3", "alias4", "alias5");

        var page = service.getAll(PageRequest.of(0, 3   ), BASE_URL);

        assertEquals(List.of("alias5", "alias4", "alias3"), page.getContent().stream().map(UrlListItem::getAlias).toList());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void getAll_beyondLastPage_returnsEmptyListWithTotal() {
        shortenAliases("a1", "a2");

        var page = service.getAll(PageRequest.of(5, 2), BASE_URL);

        assertTrue(page.getContent().isEmpty());
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void getAll_withInvalidArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.getAll(PageRequest.of(-1, 10), BASE_URL));
        assertThrows(IllegalArgumentException.class, () -> service.getAll(PageRequest.of(0, 0), BASE_URL));
    }

    private void shortenAliases(String... aliases) {
        for (var alias : aliases) {
            service.shorten(buildRequest(FULL_URL, alias), BASE_URL);
        }
    }

    private ShortenUrlRequest buildRequest(String fullUrl, String customAlias) {
        var request = new ShortenUrlRequest();
        request.setFullUrl(fullUrl);
        request.setCustomAlias(customAlias);
        return request;
    }
}
