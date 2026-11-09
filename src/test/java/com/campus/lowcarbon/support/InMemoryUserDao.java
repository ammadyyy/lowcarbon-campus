package com.campus.lowcarbon.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.dao.UserDao;
import com.campus.lowcarbon.domain.User;

/** 内存版用户 DAO，用于隔离测试 Service 层业务规则（不依赖数据库）。 */
public class InMemoryUserDao implements UserDao {

    private final Map<Long, User> storage = new LinkedHashMap<Long, User>();
    private long sequence;

    @Override
    public User insert(User user) {
        if (findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateException("用户名已被占用：" + user.getUsername());
        }
        long id = ++sequence;
        User saved = user.toBuilder().id(id).build();
        storage.put(id, saved);
        return saved;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        for (User user : storage.values()) {
            if (user.getUsername().equals(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<User>(storage.values());
    }

    @Override
    public boolean update(User user) {
        if (!storage.containsKey(user.getId())) {
            return false;
        }
        storage.put(user.getId(), user);
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
