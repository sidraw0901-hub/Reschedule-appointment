package com.carebridge.rescheduling.domain.repository;

import com.carebridge.rescheduling.domain.model.Room;
import com.carebridge.rescheduling.domain.model.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByStatus(RoomStatus status);
}
