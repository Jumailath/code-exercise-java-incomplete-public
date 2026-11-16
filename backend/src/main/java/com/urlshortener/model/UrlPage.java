package com.urlshortener.model;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;

public record UrlPage(List<UrlListItem> urls, long totalElements, int totalPages, int size, int number) {

    public static UrlPage map(Page<UrlListItem> urlListItemPage) {
        if(Objects.isNull(urlListItemPage))
        {
            return new UrlPage(List.of(), 0, 0, 0,0);
        }

        return new UrlPage(urlListItemPage.getContent(),
                urlListItemPage.getTotalElements(),
                urlListItemPage.getTotalPages(),
                urlListItemPage.getSize(),
                urlListItemPage.getNumber()
                );
    }
}
