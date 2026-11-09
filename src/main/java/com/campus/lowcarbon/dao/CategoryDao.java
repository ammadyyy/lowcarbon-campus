package com.campus.lowcarbon.dao;

import java.util.List;
import java.util.Optional;

import com.campus.lowcarbon.domain.Category;

/** 物品分类数据访问接口。 */
public interface CategoryDao {

    Category insert(Category category);

    Optional<Category> findById(long id);

    Optional<Category> findByName(String name);

    /**
     * @param enabledOnly 是否只返回启用中的分类（发布/筛选下拉框使用）
     */
    List<Category> findAll(boolean enabledOnly);

    boolean update(Category category);

    boolean deleteById(long id);

    long count();
}
