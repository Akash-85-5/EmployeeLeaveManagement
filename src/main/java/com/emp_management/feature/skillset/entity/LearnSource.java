//package com.emp_management.feature.skillset.entity;
//
//
//import jakarta.persistence.*;
//
//@Entity
//public class LearnSource {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    private String source;
//    @ManyToOne
//    @JoinColumn(name = "skill_id")
//    private Skill skill;
//
//    // GETTERS & SETTERS
//
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getSource() { return source; }
//    public void setSource(String source) { this.source = source; }
//
//    public Skill getSkill() { return skill; }
//    public void setSkill(Skill skill) { this.skill = skill; }
//}