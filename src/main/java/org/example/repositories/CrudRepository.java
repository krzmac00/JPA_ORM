package org.example.repositories;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    <S extends T> S save(S entity);

    <S extends T> List<S> saveAll(List<S> entities);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findAllById(List<ID> ids);

    long count();

    void delete(T entity);

    void deleteById(ID id);

    void deleteAll(List<? extends T> entities);

    void deleteAllById(List<ID> ids);

    void deleteAll();

    boolean existsById(ID id);
}
