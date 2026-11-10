package com.pms.dto;

import com.pms.entity.Tag;

// Tag data returned to the client
public record TagResponse(Long id, String name, String color) {

    // Converts a Tag entity into a response object
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
