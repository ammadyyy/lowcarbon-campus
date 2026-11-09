package com.campus.lowcarbon.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.dao.CategoryDao;
import com.campus.lowcarbon.domain.Category;

/** 内存版分类 DAO。 */
public class InMemoryCategoryDao implements CategoryDao {

    private final Map<Long, Category> storage = new LinkedHashMap<Long, Category>();
    private long sequence;

    @Override
    public Category insert(Category category) {
        if (findByName(category.getName()).isPresent()) {
            throw new DuplicateException("分类名称已存在：" + category.getName());
        }
        long id = ++sequence;
        Category saved = category.toBuilder().id(id).build();
        storage.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Category> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Category> findByName(String name) {
        for (Category category : storage.values()) {
            if (category.getName().equals(name)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Category> findAll(boolean enabledOnly) {
        List<Category> result = new ArrayList<Category>();
        for (Category category : storage.values()) {
            if (!enabledOnly || category.isEnabled()) {
                result.add(category);
            }
        }
        Collections.sort(result, new Comparator<Category>() {
            @Override
            public int compare(Category left, Category right) {
                int byOrder = Integer.compare(left.getSortOrder(), right.getSortOrder());
                return byOrder != 0 ? byOrder : Long.compare(left.getId(), right.getId());
            }
        });
        return result;
    }

    @Override
    public boolean update(Category category) {
        if (!storage.containsKey(category.getId())) {
            return false;
        }
        storage.put(category.getId(), category);
        return true;
    }

    @Override
    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }

    @Override
    public long count() {
        return storage.size();
    }
}
