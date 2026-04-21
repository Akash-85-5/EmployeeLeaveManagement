package com.emp_management.feature.birthday.dto;

/**
 * Projected birthday employee info returned by all list endpoints.
 * id = User.id (Long) — matches frontend BirthdayEmployee.id: number
 */
public class BirthdayEmployeeDTO {

    private Long id;           // User.id (numeric) — frontend expects number
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String department;
    private String dateOfBirth; // ISO string "YYYY-MM-DD"
    private int age;
    private String profilePicture;

    public BirthdayEmployeeDTO() {}

    public BirthdayEmployeeDTO(Long id, String employeeCode, String firstName,
                               String lastName, String department,
                               String dateOfBirth, int age, String profilePicture) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.profilePicture = profilePicture;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}
