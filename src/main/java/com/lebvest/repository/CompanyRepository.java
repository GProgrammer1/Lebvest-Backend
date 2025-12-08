package com.lebvest.repository;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByUser(User user);
    Optional<Company> findByName(String name);
    Optional<Company> findByUser(User user);
    
    @Query("SELECT DISTINCT c FROM Company c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.socialMedia " +
           "WHERE c.user = :user")
    Optional<Company> findByUserWithBasicRelations(@Param("user") User user);
    
    @Query("SELECT c FROM Company c WHERE (:sector IS NULL OR c.sector = :sector) AND (:location IS NULL OR c.location = :location)")
    Page<Company> findBySectorAndLocation(@Param("sector") CompanySector sector, @Param("location") Location location, Pageable pageable);
    
    Page<Company> findBySector(CompanySector sector, Pageable pageable);
    
    @Query("SELECT c FROM Company c WHERE c.location = :location")
    Page<Company> findByLocation(@Param("location") Location location, Pageable pageable);
    
    // Batch load companies by user IDs
    @Query("SELECT c FROM Company c WHERE c.user.id IN :userIds")
    List<Company> findByUserIds(@Param("userIds") List<Long> userIds);
}
