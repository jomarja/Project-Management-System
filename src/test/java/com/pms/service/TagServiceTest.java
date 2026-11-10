package com.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.pms.dto.TagRequest;
import com.pms.dto.TagResponse;
import com.pms.entity.Tag;
import com.pms.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void createTag_savesAndReturnsTag() {
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TagResponse response = tagService.createTag(new TagRequest("backend", "#FF5733"));

        assertThat(response).isEqualTo(new TagResponse(1L, "backend", "#FF5733"));
    }

    @Test
    void getAllTags_returnsTagsSortedByName() {
        when(tagRepository.findAll(Sort.by("name"))).thenReturn(List.of(tag(1L, "backend"), tag(2L, "urgent")));

        List<TagResponse> tags = tagService.getAllTags();

        assertThat(tags).extracting(TagResponse::name).containsExactly("backend", "urgent");
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        tag.setColor("#FF5733");
        return tag;
    }
}
