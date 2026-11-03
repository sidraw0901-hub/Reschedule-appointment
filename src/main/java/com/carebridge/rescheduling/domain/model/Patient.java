package com.carebridge.rescheduling.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priorityLevel;

    public Patient() {}

    public Patient(UUID id, String firstName, String lastName, String phone, PriorityLevel priorityLevel) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.priorityLevel = priorityLevel != null ? priorityLevel : PriorityLevel.NORMAL;
    }

    @PrePersist
    protected void onCreate() {
        if (this.priorityLevel == null) {
            this.priorityLevel = PriorityLevel.NORMAL;
        }
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String firstName;
        private String lastName;
        private String phone;
        private PriorityLevel priorityLevel;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder priorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; return this; }

        public Patient build() {
            return new Patient(id, firstName, lastName, phone, priorityLevel);
        }
    }
}
