package com.emp_management.feature.birthday.repository;

import com.emp_management.feature.birthday.entity.BirthdayWish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BirthdayWishRepository extends JpaRepository<BirthdayWish, Long> {

    // ✅ FIXED → Long → String
    List<BirthdayWish> findByBirthdayEmployeeIdAndWishYearOrderByCreatedAtDesc(
            String birthdayEmployeeId, int wishYear
    );

    // ✅ Already correct
    boolean existsByBirthdayEmployeeIdAndWishedByEmployeeIdAndWishYear(
            String birthdayEmployeeId,
            String wishedByEmployeeId,
            Integer wishYear
    );
}