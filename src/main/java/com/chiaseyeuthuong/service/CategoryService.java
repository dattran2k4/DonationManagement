package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.dto.request.CategoryRequest;
import com.chiaseyeuthuong.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    long saveCategory(CategoryRequest categoryRequest);
}
