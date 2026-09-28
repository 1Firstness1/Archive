package ru.mirea.archive.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    T save(T entity);

    List<T> findAll();

    Optional<T> findById(ID id);

    void update(T entity);

    boolean deleteById(ID id);
}
