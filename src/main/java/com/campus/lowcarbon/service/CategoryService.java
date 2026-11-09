package com.campus.lowcarbon.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.Validators;
import com.campus.lowcarbon.dao.CategoryDao;
import com.campus.lowcarbon.domain.Category;

/** 分类业务服务：分类由管理员维护，普通用户只读取启用的分类。 */
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private static final int NAME_MAX = 20;
    private static final int DESCRIPTION_MAX = 100;

    private final CategoryDao categoryDao;

    public CategoryService(CategoryDao categoryDao) {
        if (categoryDao == null) {
            throw new IllegalArgumentException("CategoryService 依赖不能为空");
        }
        this.categoryDao = categoryDao;
    }

    /** 用户可见的分类（已按 sort_order 排序）。 */
    public List<Category> listEnabled() {
        return categoryDao.findAll(true);
    }

    /** 后台可见的全部分类。 */
    public List<Category> listAll() {
        return categoryDao.findAll(false);
    }

    /** 新增分类。 */
    public Category create(String name, String description, int sortOrder) {
        String safeName = Validators.requireText(name, "分类名称", 1, NAME_MAX);
        String safeDescription = Validators.optionalText(description, "分类描述", DESCRIPTION_MAX);
        if (categoryDao.findByName(safeName).isPresent()) {
            throw new DuplicateException("分类名称已存在：" + safeName);
        }
        Category saved = categoryDao.insert(Category.builder(safeName)
                .description(safeDescription)
                .sortOrder(sortOrder)
                .enabled(true)
                .build());
        log.info("新增分类：id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /** 修改分类的名称、描述、排序与启用状态。 */
    public Category update(long categoryId, String name, String description, int sortOrder, boolean enabled) {
        Category existing = requireById(categoryId);
        String safeName = Validators.requireText(name, "分类名称", 1, NAME_MAX);
        String safeDescription = Validators.optionalText(description, "分类描述", DESCRIPTION_MAX);
        Optional<Category> sameName = categoryDao.findByName(safeName);
        if (sameName.isPresent() && sameName.get().getId() != categoryId) {
            throw new DuplicateException("分类名称已存在：" + safeName);
        }
        Category updated = existing.toBuilder()
                .name(safeName)
                .description(safeDescription)
                .sortOrder(sortOrder)
                .enabled(enabled)
                .build();
        if (!categoryDao.update(updated)) {
            throw new NotFoundException("分类不存在：" + categoryId);
        }
        log.info("修改分类：id={}, name={}, enabled={}", categoryId, safeName, enabled);
        return updated;
    }

    /** 启用 / 禁用分类（禁用后不能再发布该类物品，历史物品保留）。 */
    public Category setEnabled(long categoryId, boolean enabled) {
        Category existing = requireById(categoryId);
        Category updated = existing.toBuilder().enabled(enabled).build();
        if (!categoryDao.update(updated)) {
            throw new NotFoundException("分类不存在：" + categoryId);
        }
        log.info("{}分类：id={}, name={}", enabled ? "启用" : "禁用", categoryId, existing.getName());
        return updated;
    }

    /** 按主键查询，不存在时抛业务异常。 */
    public Category requireById(long categoryId) {
        Validators.requireId(categoryId, "分类编号");
        Optional<Category> found = categoryDao.findById(categoryId);
        if (!found.isPresent()) {
            throw new NotFoundException("分类不存在：" + categoryId);
        }
        return found.get();
    }

    public Optional<Category> findById(long categoryId) {
        return categoryDao.findById(categoryId);
    }

    public long count() {
        return categoryDao.count();
    }
}
