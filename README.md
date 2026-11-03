# CareBridge — AI-Powered Reactive Appointment Rescheduling Engine

CareBridge is a reactive scheduling coordination platform designed to handle clinic disruptions (e.g., provider illness, exam room equipment failure) autonomously. When an outage occurs, CareBridge intercepts the disruption, identifies affected appointments, calculates eligible candidate slots based on real-time doctor bandwidth (strictly enforcing the **4 Slots Max Capacity Invariant**), and presents recommendations to front-desk staff for 1-click atomic approval.

---

## 4 Slots Max Capacity Invariant & Rebalancing Workflow

1. **Strict 4-Slot Policy**:
   - Every doctor has a maximum of **4 slots total** (booked + available).
   - Each doctor card displays **4 discrete slot indicators**:
     - 🟥 **Booked Slot**
     - 🟩 **Available Slot** (ready to accept rebookings)
     - ⬜ **Offline / Out of Service**
2. **Disruption & Recommendation**:
   - When Doctor A goes `UNAVAILABLE`, their booked appointments enter triage.
   - The engine discovers colleagues in the same specialty who have open slots (`< 4 booked`).
   - AI advisor ranks the best match and drafts a compassionate phone script for clinic staff.
3. **Cascading Disruption Self-Healing (Secondary Outage Edge Case)**:
   - **What if Doctor B ALSO becomes unavailable?**
     - **Case 1 (Disrupted after rebooking)**: Patient transferred from Doctor A $\rightarrow$ Doctor B. Later, Doctor B also calls in sick. The system automatically detects the disruption on previously `RESCHEDULED` visits, returns them to triage, and autonomously routes them to **Doctor C** (e.g. Dr. Elena Rostova).
     - **Case 2 (Disrupted while pending approval)**: Doctor B goes offline before front-desk staff approves the recommendation. The system immediately expires Doctor B's suggestion, raises a 409 Conflict if an approval is in flight, and auto-routes the patient to Doctor C.
4. **Atomic Reassignment & Slot Count Increment**:
   - Upon coordinator approval:
     - The appointment is transferred to the receiving doctor (Doctor B or Doctor C).
     - **The receiving doctor's booked slot count increases** (e.g., from 1/4 to 2/4).
     - **The receiving doctor's available bandwidth decreases** (e.g., from 3 open to 2 open).
     - **Original doctor's name is completely removed** and replaced by the receiving doctor (`NEW DOCTOR (+1 SLOT)`).
5. **Doctor Restoration & Re-entry**:
   - When Doctor A is toggled back to `AVAILABLE`, their cancelled slots reopen as available.
   - Doctor A is now active again and **ready to accept appointments/tasks** from any other disrupted doctor!

---

## 1-Command Startup

```bash
mvn spring-boot:run
```
*(Or run the fat JAR directly: `java -jar target/rescheduling-engine-1.0.0-SNAPSHOT.jar`)*

Open **`http://localhost:8080/`** in your browser.

---

## 5-Minute Hackathon Demo Script

1. **0:00 - 1:00 | The 4-Slot Capacity Architecture**:
   - Open `http://localhost:8080/`.
   - Point to the **4-Slot Indicators** on each doctor:
     - **Dr. Sarah Chen** (Cardiology): 3 of 4 slots booked (🟥🟥🟥🟩), 1 slot free.
     - **Dr. Marcus Brody** (Cardiology): 1 of 4 slots booked (🟥🟩🟩🟩), **3 slots free to accept overflow!**
     - **Dr. Elena Rostova** (Cardiology): 1 of 4 slots booked (🟥🟩🟩🟩), **3 slots free!**
2. **1:00 - 2:00 | Toggle Doctor Unavailable**:
   - On **Dr. Sarah Chen's** card, click the toggle switch to set her **Unavailable**.
   - Her 3 patients (Eleanor Vance, Robert Gomez, Maya Lin) instantly enter `⚠ RESCHEDULE REQUIRED`.
   - The autonomous worker detects that **Dr. Marcus Brody** and **Dr. Elena Rostova** have open capacity (`< 4 slots`).
3. **2:00 - 3:30 | Review & Reassign**:
   - Click **"Review & Rebook"** for Eleanor Vance.
   - Point out:
     - Original doctor (Dr. Sarah Chen) is struck out.
     - Target assigned doctor: **Dr. Marcus Brody** in Procedure Room 2.
     - 94% confidence match and read-aloud staff script.
4. **3:30 - 4:30 | Atomic Transfer & Slot Count Increment**:
   - Click **"Confirm Rebook & Increment Slot"**.
   - Watch the live updates:
     - Eleanor Vance's doctor is now **Dr. Marcus Brody** (`NEW DOCTOR (+1 SLOT)`). Dr. Chen's name is gone.
     - Look up at **Dr. Marcus Brody's Card**: his booked count increased from **1 of 4 to 2 of 4** (🟥🟥🟩🟩)!
5. **4:30 - 5:00 | Restore Doctor to Accept Tasks**:
   - Toggle **Dr. Sarah Chen** back to **Available**.
   - Her card turns green, her available slots reopen, and she is now eligible to absorb rebooked appointments if another doctor calls in sick!

---

## Verification
All 5 test suites pass with 100% success (6 automated test cases):
- `CascadingDisruptionTest`: Proves multi-level cascading outage (Doctor A $\rightarrow$ Doctor B $\rightarrow$ Doctor C) and pending suggestion expiration when receiving doctor goes offline.
- `ConcurrencyAcceptanceTest`: Proves zero double bookings under simultaneous coordinator race conditions via conditional CAS.
- `EndToEndReactiveLoopTest`: Full reactive loop verified (Outbox $\rightarrow$ Worker $\rightarrow$ AI advisor $\rightarrow$ CAS Rebooking).
- `RuleBasedStrategyTest` & `DisruptionServiceTest`.
