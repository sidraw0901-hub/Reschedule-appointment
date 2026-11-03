package com.carebridge.rescheduling.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String capabilities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Version
    private Long version;

    public Room() {}

    public Room(UUID id, String name, String capabilities, RoomStatus status) {
        this.id = id;
        this.name = name;
        this.capabilities = capabilities;
        this.status = status != null ? status : RoomStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = RoomStatus.AVAILABLE;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String name;
        private String capabilities;
        private RoomStatus status;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder capabilities(String capabilities) { this.capabilities = capabilities; return this; }
        public Builder status(RoomStatus status) { this.status = status; return this; }

        public Room build() {
            return new Room(id, name, capabilities, status);
        }
    }
}
