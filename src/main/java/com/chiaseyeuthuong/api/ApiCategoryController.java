package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.dto.request.CategoryRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-CATEGORY-CONTROLLER")
@RequestMapping("/api/categories")
public class ApiCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse getAllCategories() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách danh mục thành công")
                .data(categoryService.getAllCategories())
                .build();
    }

    @PostMapping("/save")
    public ApiResponse saveCategory(@RequestBody @Valid CategoryRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Lưu danh mục thành công")
                .data(categoryService.saveCategory(request))
                .build();
    }
}
