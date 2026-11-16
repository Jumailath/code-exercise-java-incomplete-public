package com.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.sql.init.data-locations="
})
class UrlsControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM shortened_urls");
        insert("redir", "https://target.com/", "2026-01-01T00:00:00Z");
    }

    private void insert(String alias, String fullUrl, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO shortened_urls (alias, full_url, created_at) VALUES (?, ?, ?)",
                alias, fullUrl, createdAt);
    }

    private static List<String> aliases(JsonNode page) {
        var aliases = new ArrayList<String>();
        page.get("urls").forEach(item -> aliases.add(item.get("alias").asText()));
        return aliases;
    }

    @Test
    void getAlias_ExistingAlias_Returns302WithLocation() {
        var response = restTemplate.getForEntity("/redir", String.class);

        assertThat(response.getStatusCode()).isEqualTo(FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://target.com/");
    }

    @Test
    void getUrls_withPaging_returnsRequestedPageNewestFirst() {
        insert("p1", "https://target.com/p1", "2026-11-02T00:00:00Z");
        insert("p2", "https://target.com/p2", "2026-11-03T00:00:00Z");
        insert("p3", "https://target.com/p3", "2026-11-04T00:00:00Z");

        var response = restTemplate.getForEntity("/urls?page=1&size=2", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        var page = response.getBody();
        assertNotNull(page);
        assertThat(aliases(page)).containsExactly("p1", "redir");
        assertThat(page.get("totalElements").asLong()).isEqualTo(4);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("number").asInt()).isEqualTo(1);
        assertThat(page.get("size").asInt()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=0", "size=-1", "page=-1", "size=abc"})
    void getUrls_withInvalidPaging_Returns200WithDefaultPage(String query) {
        var response = restTemplate.getForEntity("/urls?" + query, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        var page = response.getBody();
        assertThat(page.get("number").asInt()).isEqualTo(0);
        assertThat(page.get("size").asInt()).isEqualTo(5);
    }
}
