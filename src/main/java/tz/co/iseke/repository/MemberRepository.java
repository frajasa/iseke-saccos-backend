package tz.co.iseke.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.Member;
import tz.co.iseke.enums.MemberStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    
    Optional<Member> findByMemberNumber(String memberNumber);
    
    Optional<Member> findByNationalId(String nationalId);
    
    Optional<Member> findByEmail(String email);
    
    Optional<Member> findByPhoneNumber(String phoneNumber);
    
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);
    
    @Query("SELECT m FROM Member m WHERE " +
           "LOWER(CONCAT(m.firstName, ' ', COALESCE(m.middleName, ''), ' ', m.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.memberNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Member> searchMembers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    boolean existsByMemberNumber(String memberNumber);
    
    boolean existsByNationalId(String nationalId);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberNumber LIKE :prefix%")
    long countByMemberNumberPrefix(@Param("prefix") String prefix);

    @Query("SELECT m FROM Member m WHERE " +
           "LOWER(m.firstName) = LOWER(:firstName) AND LOWER(m.lastName) = LOWER(:lastName) AND m.dateOfBirth = :dateOfBirth")
    List<Member> findPotentialDuplicates(@Param("firstName") String firstName,
                                          @Param("lastName") String lastName,
                                          @Param("dateOfBirth") LocalDate dateOfBirth);

    @Query("SELECT m FROM Member m WHERE " +
           "(LOWER(m.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND m.dateOfBirth = :dateOfBirth")
    List<Member> findSimilarMembers(@Param("name") String name, @Param("dateOfBirth") LocalDate dateOfBirth);
}