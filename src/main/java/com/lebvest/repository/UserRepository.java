package com.lebvest.repository;

import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String username);
    
    // Use EntityGraph for eager loading roles - separate from pagination query
    @EntityGraph(attributePaths = {"roles"})
    List<User> findAllByIdIn(List<Long> userIds);
    
    // FIXED: Removed LEFT JOIN FETCH and DISTINCT from paginated queries
    // Use JOIN without FETCH for filtering, then load roles separately with EntityGraph
    
    // Query with role filter (using JOIN without FETCH for proper pagination)
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);
    
    // Query with search filter - using prefix search for better index usage
    // Note: For full-text search, consider using MATCH() AGAINST() instead
    @Query("SELECT u FROM User u " +
           "WHERE (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%'))")
    Page<User> findBySearch(@Param("search") String search, Pageable pageable);
    
    // Query with role and search filters
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role " +
           "AND (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%'))")
    Page<User> findByRoleAndSearch(@Param("role") Role role, @Param("search") String search, Pageable pageable);
    
    // Query for users with enabled/disabled status
    Page<User> findByEnabled(boolean enabled, Pageable pageable);
    
    // Query for users with locked status
    Page<User> findByLocked(boolean locked, Pageable pageable);
    
    // Query with role and enabled filter
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role AND u.enabled = :enabled")
    Page<User> findByRoleAndEnabled(@Param("role") Role role, @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with role and locked filter
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role AND u.locked = :locked")
    Page<User> findByRoleAndLocked(@Param("role") Role role, @Param("locked") boolean locked, Pageable pageable);
    
    // Query with search and enabled filter
    @Query("SELECT u FROM User u " +
           "WHERE (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%')) " +
           "AND u.enabled = :enabled")
    Page<User> findBySearchAndEnabled(@Param("search") String search, @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with search and locked filter
    @Query("SELECT u FROM User u " +
           "WHERE (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%')) " +
           "AND u.locked = :locked")
    Page<User> findBySearchAndLocked(@Param("search") String search, @Param("locked") boolean locked, Pageable pageable);
    
    // Query with role, search, and enabled filter
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role " +
           "AND (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%')) " +
           "AND u.enabled = :enabled")
    Page<User> findByRoleAndSearchAndEnabled(@Param("role") Role role, @Param("search") String search, 
                                             @Param("enabled") boolean enabled, Pageable pageable);
    
    // Query with role, search, and locked filter
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r = :role " +
           "AND (:search IS NULL OR u.name LIKE CONCAT(:search, '%') " +
           "OR u.email LIKE CONCAT(:search, '%')) " +
           "AND u.locked = :locked")
    Page<User> findByRoleAndSearchAndLocked(@Param("role") Role role, @Param("search") String search, 
                                            @Param("locked") boolean locked, Pageable pageable);
}
