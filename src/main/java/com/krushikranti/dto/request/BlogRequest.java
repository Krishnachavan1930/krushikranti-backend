package com.krushikranti.dto.request;

import lombok.Data;

@Data
public class BlogRequest {
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String imageUrl;
    private String category;
    private String tags;
    private String authorName;
    private String status;
    private String metaTitle;
    private String metaDescription;
}
