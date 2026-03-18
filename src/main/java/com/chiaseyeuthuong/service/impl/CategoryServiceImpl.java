package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.dto.request.CategoryRequest;
import com.chiaseyeuthuong.dto.response.CategoryResponse;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Category;
import com.chiaseyeuthuong.repository.CategoryRepository;
import com.chiaseyeuthuong.service.CategoryService;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CATEGORY-SERVICE")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = ResourceNotFoundException.class)
    public long saveCategory(CategoryRequest request) {
        Category category = (request.getId() != null) ? categoryRepository.findById(request.getId()).orElseThrow(() -> new ResourceNotFoundException("Not found")) : new Category();
        BeanUtils.copyProperties(request, category);

        String slug = Slugify.builder().build().slugify(request.getName());
        boolean slugExists = (request.getId() != null) ? categoryRepository.existsBySlugAndIdNot(slug, request.getId()) : categoryRepository.existsBySlug(slug);

        if (slugExists) {
            throw new InvalidDataException("Danh mục đã tồn tại");
        }

        category.setSlug(slug);
        Category result = categoryRepository.save(category);

        log.info("Saved category id {} successfully", result.getId());
        return result.getId();
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        BeanUtils.copyProperties(category, response);
        return response;
    }
}
