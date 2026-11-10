package com.pms.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pms.dto.TagRequest;
import com.pms.dto.TagResponse;
import com.pms.service.TagService;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @MockitoBean
    private TagService tagService;

    private final MockMvc mockMvc;

    @Autowired
    TagControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createTag_returns201WithCreatedTag() throws Exception {
        when(tagService.createTag(new TagRequest("backend", "#FF5733")))
                .thenReturn(new TagResponse(1L, "backend", "#FF5733"));

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "backend", "color": "#FF5733" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("backend"))
                .andExpect(jsonPath("$.color").value("#FF5733"));
    }

    @Test
    void createTag_returns400WhenColorIsNotHexCode() throws Exception {
        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "backend", "color": "red" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("Color must be a hex code like #FF5733"));

        verifyNoInteractions(tagService);
    }

    @Test
    void getAllTags_returnsTags() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of(
                new TagResponse(1L, "backend", "#FF5733"),
                new TagResponse(2L, "urgent", "#C70039")));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("urgent"));
    }
}
