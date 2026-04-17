//package com.emp_management.feature.skillset.entity;
//
//import jakarta.persistence.*;
//
//@Entity
//public class AppliedPlace {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String place;
//
//    @ManyToOne
//    @JoinColumn(name = "skill_id")
//    private Skill skill;
//
//    // GETTERS & SETTERS
//
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getPlace() { return place; }
//    public void setPlace(String place) { this.place = place; }
//
//    public Skill getSkill() { return skill; }
//    public void setSkill(Skill skill) { this.skill = skill; }
//}