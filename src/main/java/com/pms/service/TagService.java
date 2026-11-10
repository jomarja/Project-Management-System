package com.pms.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.dto.TagRequest;
import com.pms.dto.TagResponse;
import com.pms.entity.Tag;
import com.pms.repository.TagRepository;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        Tag tag = new Tag();
        tag.setName(request.name());
        tag.setColor(request.color());
        return TagResponse.from(tagRepository.save(tag));
    }

    // All tags in alphabetical order
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll(Sort.by("name")).stream()
                .map(TagResponse::from)
                .toList();
    }
}
