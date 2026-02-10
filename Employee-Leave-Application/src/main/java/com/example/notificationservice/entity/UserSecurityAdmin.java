package com.example.notificationservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_security_admin")
public class UserSecurityAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "vpn_enabled")
    private String vpnEnabled; // Y/N

    @Column(name = "biometric_registered")
    private String biometricRegistered; // Y/N

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getVpnEnabled() { return vpnEnabled; }
    public void setVpnEnabled(String vpnEnabled) { this.vpnEnabled = vpnEnabled; }

    public String getBiometricRegistered() { return biometricRegistered; }
    public void setBiometricRegistered(String biometricRegistered) { this.biometricRegistered = biometricRegistered; }
}
