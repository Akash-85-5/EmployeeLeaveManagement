package com.emp_management.shared.repository;

import com.emp_management.shared.dto.BranchListDto;
import com.emp_management.shared.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch,Long> {
    @Query("SELECT b.id AS id, b.name AS branchName, b.company.name AS companyName, b.company.country.name AS countryName FROM Branch b")
    List<BranchListDto> findAllBranchDetails();
}
