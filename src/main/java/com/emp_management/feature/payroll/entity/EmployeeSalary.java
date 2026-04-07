//package com.emp_management.feature.payroll.entity;
//
//import jakarta.persistence.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
//@Entity
//@Table(
//        name = "employee_salary",
//        uniqueConstraints = @UniqueConstraint(
//                columnNames = {"employee_id","effective_from"}
//        )
//)
//public class EmployeeSalary {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "employee_id", nullable = false)
//    private String employeeId;
//
//    @Column(nullable = false, precision = 12, scale = 2)
//    private BigDecimal basicSalary;
//
//    @Column(name = "effective_from", nullable = false)
//    private LocalDate effectiveFrom;
//
//    public Long getId() {
//        return id;
//    }
//
//    public String getEmployeeId() {
//        return employeeId;
//    }
//
//    public BigDecimal getBasicSalary() {
//        return basicSalary;
//    }
//
//    public LocalDate getEffectiveFrom() {
//        return effectiveFrom;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public void setEmployeeId(String employeeId) {
//        this.employeeId = employeeId;
//    }
//
//    public void setBasicSalary(BigDecimal basicSalary) {
//        this.basicSalary = basicSalary;
//    }
//
//    public void setEffectiveFrom(LocalDate effectiveFrom) {
//        this.effectiveFrom = effectiveFrom;
//    }
//}
