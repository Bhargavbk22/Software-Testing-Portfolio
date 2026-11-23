package com.lostfound.repo;

import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByStudentId(String studentId);
    List<User> findByRoles_Name(RoleName name);

    @Query("select u from User u where lower(u.fullName) like lower(concat('%', :q, '%')) " +
           "or lower(u.email) like lower(concat('%', :q, '%')) or lower(u.studentId) like lower(concat('%', :q, '%'))")
    Page<User> search(@Param("q") String q, Pageable pageable);

    @Query("select distinct u from User u join u.roles r where r.name = :role and (lower(u.fullName) like lower(concat('%', :q, '%')) " +
           "or lower(u.email) like lower(concat('%', :q, '%')) or lower(u.studentId) like lower(concat('%', :q, '%')))")
    Page<User> searchByRole(@Param("q") String q, @Param("role") RoleName role, Pageable pageable);
}
