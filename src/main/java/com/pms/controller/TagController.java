package com.pms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pms.dto.TagRequest;
import com.pms.dto.TagResponse;
import com.pms.service.TagService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody TagRequest request) {
        return tagService.createTag(request);
    }

    @GetMapping
    public List<TagResponse> getAllTags() {
        return tagService.getAllTags();
    }
}
