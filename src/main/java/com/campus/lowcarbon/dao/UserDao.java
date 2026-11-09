package com.campus.lowcarbon.dao;

import java.util.List;
import java.util.Optional;

import com.campus.lowcarbon.domain.User;

/**
 * 用户数据访问接口（接口隔离：业务层只依赖该接口，不关心底层是 SQLite 还是别的存储）。
 */
public interface UserDao {

    /**
     * 新增用户。
     *
     * @param user 待保存用户（id 会被忽略，由数据库自增）
     * @return 带自增主键的用户
     * @throws com.campus.lowcarbon.common.DuplicateException 用户名重复
     */
    User insert(User user);

    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    /** 全部用户（管理员后台用），按 id 升序。 */
    List<User> findAll();

    /** 全量更新可修改字段。 */
    boolean update(User user);

    boolean deleteById(long id);

    long count();
}
