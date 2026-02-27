package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "user_security_admin")
public class UserSecurityAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 This links to the Employee table
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "vpn_enabled")
    private BiometricVpnStatus vpnEnabled;

    @Column(name = "biometric_registered")
    private BiometricVpnStatus biometricRegistered;

    // ========================
    // GETTERS & SETTERS
    // ========================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BiometricVpnStatus getVpnEnabled() {
        return vpnEnabled;
    }

    public void setVpnEnabled(BiometricVpnStatus vpnEnabled) {
        this.vpnEnabled = vpnEnabled;
    }

    public BiometricVpnStatus getBiometricRegistered() {
        return biometricRegistered;
    }

    public void setBiometricRegistered(BiometricVpnStatus biometricRegistered) {
        this.biometricRegistered = biometricRegistered;
    }

    // ========================
    // HELPER: Update values from another object
    // ========================
    public void updateFrom(UserSecurityAdmin other) {
        if (other.getVpnEnabled() != null) this.vpnEnabled = other.getVpnEnabled();
        if (other.getBiometricRegistered() != null) this.biometricRegistered = other.getBiometricRegistered();
    }
}