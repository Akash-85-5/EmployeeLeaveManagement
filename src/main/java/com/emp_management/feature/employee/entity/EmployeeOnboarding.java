package com.emp_management.feature.employee.entity;

import com.emp_management.shared.enums.BiometricVpnStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_onboarding")
public class EmployeeOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    private BiometricVpnStatus biometricStatus;

    @Enumerated(EnumType.STRING)
    private BiometricVpnStatus vpnStatus;

    private LocalDateTime onboardingCompletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public BiometricVpnStatus getBiometricStatus() {
        return biometricStatus;
    }

    public void setBiometricStatus(BiometricVpnStatus biometricStatus) {
        this.biometricStatus = biometricStatus;
    }

    public BiometricVpnStatus getVpnStatus() {
        return vpnStatus;
    }

    public void setVpnStatus(BiometricVpnStatus vpnStatus) {
        this.vpnStatus = vpnStatus;
    }

    public LocalDateTime getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public void setOnboardingCompletedAt(LocalDateTime onboardingCompletedAt) {
        this.onboardingCompletedAt = onboardingCompletedAt;
    }
}
