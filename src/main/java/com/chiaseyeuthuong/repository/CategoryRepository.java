package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
