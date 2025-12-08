package com.lebvest.repository;

import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String username);
    
    // Optimized query with role filter and eager loading of company relations
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role)")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);
    
    // Optimized query with search filter (name or email)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearch(@Param("search") String search, Pageable pageable);
    
    // Optimized query with role and search filters
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role) " +
           "AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByRoleAndSearch(@Param("role") Role role, @Param("search") String search, Pageable pageable);
    
    // Query for users with enabled/disabled status
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.enabled = :enabled")
    Page<User> findByEnabled(@Param("enabled") boolean enabled, Pageable pageable);
    
    // Query for users with locked status
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.locked = :locked")
    Page<User> findByLocked(@Param("locked") boolean locked, Pageable pageable);
    
    // Query with role and enabled filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role) AND u.enabled = :enabled")
    Page<User> findByRoleAndEnabled(@Param("role") Role role, @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with role and locked filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role) AND u.locked = :locked")
    Page<User> findByRoleAndLocked(@Param("role") Role role, @Param("locked") boolean locked, Pageable pageable);
    
    // Query with search and enabled filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.enabled = :enabled")
    Page<User> findBySearchAndEnabled(@Param("search") String search, @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with search and locked filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.locked = :locked")
    Page<User> findBySearchAndLocked(@Param("search") String search, @Param("locked") boolean locked, Pageable pageable);
    
    // Query with role, search, and enabled filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role) " +
           "AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.enabled = :enabled")
    Page<User> findByRoleAndSearchAndEnabled(@Param("role") Role role, @Param("search") String search, 
                                             @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with role, search, and locked filter
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE (:role IS NULL OR r = :role) " +
           "AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND u.locked = :locked")
    Page<User> findByRoleAndSearchAndLocked(@Param("role") Role role, @Param("search") String search, 
                                            @Param("locked") boolean locked, Pageable pageable);
}
