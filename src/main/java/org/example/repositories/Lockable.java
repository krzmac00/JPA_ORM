package org.example.repositories;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface Lockable<T, ID> {
    Optional<T> lockedFindById(ID id, LockModeType lockModeType);

}
