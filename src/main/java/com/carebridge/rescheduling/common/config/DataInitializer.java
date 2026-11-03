package com.carebridge.rescheduling.common.config;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProviderRepository providerRepository;
    private final RoomRepository roomRepository;
    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    public DataInitializer(ProviderRepository providerRepository,
                           RoomRepository roomRepository,
                           PatientRepository patientRepository,
                           SlotRepository slotRepository,
                           AppointmentRepository appointmentRepository) {
        this.providerRepository = providerRepository;
        this.roomRepository = roomRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(String... args) {
        if (providerRepository.count() > 0) {
            return;
        }
        log.info("Seeding CareBridge roster: 4 slots per doctor capacity invariant...");

        // 1. Rooms
        Room r1A = roomRepository.save(new Room(null, "Exam Room 1A", "EKG, General Exam", RoomStatus.AVAILABLE));
        Room r1B = roomRepository.save(new Room(null, "Exam Room 1B", "General Exam, Vitals", RoomStatus.AVAILABLE));
        Room r2 = roomRepository.save(new Room(null, "Procedure Room 2", "Cardio Ultrasound, Echo", RoomStatus.AVAILABLE));
        Room r3 = roomRepository.save(new Room(null, "Orthopedics Lab 3", "X-Ray, Casting", RoomStatus.AVAILABLE));
        Room r4 = roomRepository.save(new Room(null, "Pediatric Room 4", "Pediatric Vitals", RoomStatus.AVAILABLE));

        // 2. Providers (4 slots max each)
        Provider drChen = providerRepository.save(new Provider(null, "Dr. Sarah Chen", "Cardiology", ProviderStatus.AVAILABLE));
        Provider drBrody = providerRepository.save(new Provider(null, "Dr. Marcus Brody", "Cardiology", ProviderStatus.AVAILABLE));
        Provider drRostova = providerRepository.save(new Provider(null, "Dr. Elena Rostova", "Cardiology", ProviderStatus.AVAILABLE));
        Provider drPatel = providerRepository.save(new Provider(null, "Dr. Priya Patel", "General Medicine", ProviderStatus.AVAILABLE));
        Provider drVance = providerRepository.save(new Provider(null, "Dr. Arthur Vance", "General Medicine", ProviderStatus.AVAILABLE));
        Provider drWilson = providerRepository.save(new Provider(null, "Dr. James Wilson", "Orthopedics", ProviderStatus.AVAILABLE));
        Provider drMartinez = providerRepository.save(new Provider(null, "Dr. Lisa Martinez", "Orthopedics", ProviderStatus.AVAILABLE));
        Provider drChang = providerRepository.save(new Provider(null, "Dr. Robert Chang", "Pediatrics", ProviderStatus.AVAILABLE));

        // 3. Patients
        Patient p1 = patientRepository.save(new Patient(null, "Eleanor", "Vance", "(555) 234-8901", PriorityLevel.HIGH));
        Patient p2 = patientRepository.save(new Patient(null, "Robert", "Gomez", "(555) 345-9012", PriorityLevel.NORMAL));
        Patient p3 = patientRepository.save(new Patient(null, "Maya", "Lin", "(555) 456-0123", PriorityLevel.URGENT));
        Patient p4 = patientRepository.save(new Patient(null, "David", "Kim", "(555) 567-1234", PriorityLevel.NORMAL));
        Patient p5 = patientRepository.save(new Patient(null, "Sophia", "Miller", "(555) 678-2345", PriorityLevel.NORMAL));
        Patient p6 = patientRepository.save(new Patient(null, "Lucas", "Bennett", "(555) 789-3456", PriorityLevel.HIGH));
        Patient p7 = patientRepository.save(new Patient(null, "Grace", "Hopper", "(555) 890-4567", PriorityLevel.NORMAL));
        Patient p8 = patientRepository.save(new Patient(null, "Alan", "Turing", "(555) 901-5678", PriorityLevel.HIGH));

        Instant base = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS);

        // Helper to seed exactly 4 slots per doctor
        // Dr. Sarah Chen: 3 booked (p1, p2, p3), 1 available
        seedDoctorSlots(drChen, r1A, base, List.of(p1, p2, p3), 1);

        // Dr. Marcus Brody: 1 booked (p4), 3 available (High Bandwidth to take Chen's patients)
        seedDoctorSlots(drBrody, r2, base.plus(15, ChronoUnit.MINUTES), List.of(p4), 3);

        // Dr. Elena Rostova: 1 booked (p5), 3 available (High Bandwidth)
        seedDoctorSlots(drRostova, r1B, base.plus(30, ChronoUnit.MINUTES), List.of(p5), 3);

        // Dr. Priya Patel: 2 booked (p6, p7), 2 available
        seedDoctorSlots(drPatel, r1B, base.plus(45, ChronoUnit.MINUTES), List.of(p6, p7), 2);

        // Dr. Arthur Vance: 1 booked (p8), 3 available
        seedDoctorSlots(drVance, r1A, base.plus(20, ChronoUnit.MINUTES), List.of(p8), 3);

        // Dr. James Wilson: 3 booked, 1 available
        seedDoctorSlots(drWilson, r3, base.plus(10, ChronoUnit.MINUTES), List.of(), 1); // 3 slots blocked or booked
        // Dr. Lisa Martinez: 1 booked, 3 available
        seedDoctorSlots(drMartinez, r3, base.plus(40, ChronoUnit.MINUTES), List.of(), 4);

        // Dr. Robert Chang: 0 booked, 4 available
        seedDoctorSlots(drChang, r4, base.plus(30, ChronoUnit.MINUTES), List.of(), 4);

        log.info("Initialized every doctor with exactly 4 slots total.");
    }

    private void seedDoctorSlots(Provider doctor, Room room, Instant startTime, List<Patient> bookedPatients, int openSlotsCount) {
        int bookedCount = bookedPatients.size();
        for (int i = 0; i < bookedCount && i < 4; i++) {
            Instant sStart = startTime.plus(i * 60L, ChronoUnit.MINUTES);
            Slot slot = slotRepository.save(new Slot(null, doctor.getId(), room.getId(), sStart, sStart.plus(30, ChronoUnit.MINUTES), SlotStatus.BOOKED));
            appointmentRepository.save(new Appointment(null, bookedPatients.get(i).getId(), doctor.getId(), slot.getId(), AppointmentStatus.BOOKED));
        }

        int remaining = 4 - bookedCount;
        for (int i = 0; i < remaining; i++) {
            Instant sStart = startTime.plus((bookedCount + i) * 60L, ChronoUnit.MINUTES);
            slotRepository.save(new Slot(null, doctor.getId(), room.getId(), sStart, sStart.plus(30, ChronoUnit.MINUTES), SlotStatus.AVAILABLE));
        }
    }
}
