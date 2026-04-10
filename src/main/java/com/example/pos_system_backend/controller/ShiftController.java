package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = "*")
public class ShiftController extends BaseController {

    private final ShiftRepository shiftRepository;
    private final ShiftCashMovementRepository movementRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public ShiftController(ShiftRepository shiftRepository,
            ShiftCashMovementRepository movementRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            JwtUtils jwtUtils) {
        this.shiftRepository = shiftRepository;
        this.movementRepository = movementRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    // ==================== HELPER: Check if user is admin ====================
    private boolean isAdmin(String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            return false;
        try {
            String token = auth.substring(7);
            String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            return user != null && user.getRole() != null && user.getRole().getId() == 1L;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== GET ALL SHIFTS ====================
    @GetMapping
    public ResponseEntity<?> getAllShifts(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String date) {
        try {
            List<Shift> shifts;

            if (isAdmin(auth)) {
                // ✅ Admin sees ALL shifts across all branches
                if (date != null && !date.isBlank()) {
                    shifts = shiftRepository.findByBusinessDateOrderByOpenedAtDesc(LocalDate.parse(date));
                } else {
                    shifts = shiftRepository.findAllByOrderByOpenedAtDesc();
                }
            } else {
                // ✅ Other roles see only their branch
                Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
                if (date != null && !date.isBlank()) {
                    shifts = shiftRepository.findByBranchIdAndBusinessDateOrderByOpenedAtDesc(
                            effectiveBranchId, LocalDate.parse(date));
                } else {
                    shifts = shiftRepository.findByBranchIdOrderByOpenedAtDesc(effectiveBranchId);
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Shift s : shifts)
                result.add(buildShiftMap(s));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== GET SHIFT BY ID ====================
    @GetMapping("/{id}")
    public ResponseEntity<?> getShiftById(@PathVariable Long id) {
        try {
            Shift shift = shiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Shift not found with id: " + id));
            return ResponseEntity.ok(buildShiftMap(shift));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== GET CURRENT OPEN SHIFT ====================
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentShift(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            Optional<Shift> shiftOpt = shiftRepository.findByBranchIdAndStatus(effectiveBranchId, "OPEN");

            if (shiftOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasOpenShift", false, "message", "No open shift found"));
            }

            Shift shift = shiftOpt.get();
            return ResponseEntity.ok(Map.of(
                    "hasOpenShift", true,
                    "shift", buildShiftMap(shift)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== OPEN SHIFT ====================
    @PostMapping("/open")
    @Transactional
    public ResponseEntity<?> openShift(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            Long currentUserId = getCurrentUserId(auth);

            // ✅ Business Rule: One open shift per branch at a time
            if (shiftRepository.existsByBranchIdAndStatus(effectiveBranchId, "OPEN")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "There is already an open shift for this branch."));
            }

            Shift shift = new Shift();
            shift.setBranchId(effectiveBranchId);
            shift.setShiftName(((String) body.getOrDefault("shiftName", "CUSTOM")).toUpperCase());
            shift.setBusinessDate(LocalDate.parse(
                    body.getOrDefault("businessDate", LocalDate.now().toString()).toString()));
            shift.setCashierId(currentUserId);
            shift.setOpenedBy(currentUserId);
            shift.setOpeningFloat(new BigDecimal(body.getOrDefault("openingFloat", "0").toString()));
            shift.setNote((String) body.get("note"));
            shift.setStatus("OPEN");

            // Generate unique shift code
            String shiftCode = "SHIFT-"
                    + shift.getBusinessDate().toString().replace("-", "")
                    + "-" + shift.getShiftName()
                    + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
            shift.setShiftCode(shiftCode);

            Shift saved = shiftRepository.save(shift);

            return ResponseEntity.ok(Map.ofEntries(
                    Map.entry("message", "Shift opened successfully"),
                    Map.entry("shiftId", saved.getId()),
                    Map.entry("shiftCode", saved.getShiftCode()),
                    Map.entry("shiftName", saved.getShiftName()),
                    Map.entry("openingFloat", saved.getOpeningFloat()),
                    Map.entry("openedAt", saved.getOpenedAt().toString())));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== CLOSE SHIFT ====================
    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<?> closeShift(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            Long currentUserId = getCurrentUserId(auth);

            Shift shift = shiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Shift not found with id: " + id));

            if (!"OPEN".equals(shift.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Shift is already closed."));
            }

            // ✅ Accept "closingCash" OR "closingCountedCash" from frontend
            String rawCash = body.containsKey("closingCash")
                    ? body.get("closingCash").toString()
                    : body.getOrDefault("closingCountedCash", "0").toString();
            BigDecimal closingCountedCash = new BigDecimal(rawCash);

            // ✅ Calculate all totals from DB
            BigDecimal totalCashSales = coalesce(shiftRepository.getTotalCashSalesByShift(id));
            BigDecimal totalNonCashSales = coalesce(shiftRepository.getTotalNonCashSalesByShift(id));
            BigDecimal totalExpense = coalesce(shiftRepository.getTotalCashExpenseByShift(id));
            BigDecimal totalCashIn = coalesce(shiftRepository.getTotalCashInByShift(id));
            BigDecimal totalCashRemoved = coalesce(shiftRepository.getTotalCashRemovedByShift(id));
            BigDecimal totalPaidOut = coalesce(shiftRepository.getTotalPaidOutByShift(id));

            // ✅ Business formula:
            // expected_cash = opening_float + total_cash_sales + cash_in - total_expense -
            // cash_removed - paid_out
            BigDecimal expectedCash = shift.getOpeningFloat()
                    .add(totalCashSales)
                    .add(totalCashIn)
                    .subtract(totalExpense)
                    .subtract(totalCashRemoved)
                    .subtract(totalPaidOut);

            // ✅ cash_difference = counted - expected
            BigDecimal cashDifference = closingCountedCash.subtract(expectedCash);

            shift.setClosedBy(currentUserId);
            shift.setClosedAt(LocalDateTime.now());
            shift.setTotalCashSales(totalCashSales);
            shift.setTotalNonCashSales(totalNonCashSales);
            shift.setTotalSales(totalCashSales.add(totalNonCashSales));
            shift.setTotalExpense(totalExpense);
            shift.setExpectedCash(expectedCash);
            shift.setClosingCountedCash(closingCountedCash);
            shift.setCashDifference(cashDifference);
            shift.setStatus("CLOSED");

            if (body.get("note") != null) {
                shift.setNote(body.get("note").toString());
            }

            Shift saved = shiftRepository.save(shift);

            return ResponseEntity.ok(Map.ofEntries(
                    Map.entry("message", "Shift closed successfully"),
                    Map.entry("shiftId", saved.getId()),
                    Map.entry("shiftCode", saved.getShiftCode()),
                    Map.entry("openingFloat", saved.getOpeningFloat()),
                    Map.entry("totalCashSales", totalCashSales),
                    Map.entry("totalNonCashSales", totalNonCashSales),
                    Map.entry("totalSales", saved.getTotalSales()),
                    Map.entry("totalExpense", totalExpense),
                    Map.entry("totalCashIn", totalCashIn),
                    Map.entry("totalCashRemoved", totalCashRemoved),
                    Map.entry("totalPaidOut", totalPaidOut),
                    Map.entry("expectedCash", expectedCash),
                    Map.entry("closingCountedCash", closingCountedCash),
                    Map.entry("cashDifference", cashDifference),
                    Map.entry("closedAt", saved.getClosedAt().toString())));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== ADD CASH MOVEMENT ====================
    @PostMapping("/{id}/movements")
    @Transactional
    public ResponseEntity<?> addCashMovement(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            Long currentUserId = getCurrentUserId(auth);

            Shift shift = shiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Shift not found with id: " + id));

            if (!"OPEN".equals(shift.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot add movement to a closed shift."));
            }

            String type = body.get("type") != null ? body.get("type").toString().toUpperCase() : null;
            if (type == null || type.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Movement type is required."));
            }

            List<String> validTypes = List.of("SAFE_DROP", "PAID_OUT", "CASH_OUT", "FLOAT_ADD", "CASH_IN");
            if (!validTypes.contains(type)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid type. Must be one of: " + validTypes));
            }

            BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount must be greater than 0."));
            }

            ShiftCashMovement movement = new ShiftCashMovement();
            movement.setShiftId(id);
            movement.setBranchId(shift.getBranchId());
            movement.setType(MovementType.valueOf(type));
            movement.setAmount(amount);
            movement.setReason(body.getOrDefault("reason", "").toString());
            movement.setReferenceNo(body.get("referenceNo") != null ? body.get("referenceNo").toString() : null);
            movement.setCreatedBy(currentUserId);

            movementRepository.save(movement);

            return ResponseEntity.ok(Map.of(
                    "message", "Cash movement recorded successfully",
                    "type", type,
                    "amount", movement.getAmount()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== GET MOVEMENTS BY SHIFT ====================
    @GetMapping("/{id}/movements")
    public ResponseEntity<?> getMovements(@PathVariable Long id) {
        try {
            List<ShiftCashMovement> movements = movementRepository.findByShiftIdOrderByCreatedAtDesc(id);
            List<Map<String, Object>> result = new ArrayList<>();
            for (ShiftCashMovement m : movements) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.getId());
                map.put("type", m.getType());
                map.put("amount", m.getAmount());
                map.put("reason", m.getReason());
                map.put("referenceNo", m.getReferenceNo());
                map.put("createdBy", m.getCreatedBy());
                map.put("createdAt", m.getCreatedAt());
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== GET LIVE SHIFT SUMMARY ====================
    @GetMapping("/{id}/summary")
    public ResponseEntity<?> getShiftSummary(@PathVariable Long id) {
        try {
            Shift shift = shiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Shift not found"));

            BigDecimal totalCashSales = coalesce(shiftRepository.getTotalCashSalesByShift(id));
            BigDecimal totalNonCashSales = coalesce(shiftRepository.getTotalNonCashSalesByShift(id));
            BigDecimal totalExpense = coalesce(shiftRepository.getTotalCashExpenseByShift(id));
            BigDecimal totalCashIn = coalesce(shiftRepository.getTotalCashInByShift(id));
            BigDecimal totalCashRemoved = coalesce(shiftRepository.getTotalCashRemovedByShift(id));
            BigDecimal totalPaidOut = coalesce(shiftRepository.getTotalPaidOutByShift(id));

            BigDecimal expectedCash = shift.getOpeningFloat()
                    .add(totalCashSales)
                    .add(totalCashIn)
                    .subtract(totalExpense)
                    .subtract(totalCashRemoved)
                    .subtract(totalPaidOut);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("shiftId", shift.getId());
            summary.put("shiftCode", shift.getShiftCode());
            summary.put("shiftName", shift.getShiftName());
            summary.put("status", shift.getStatus());
            summary.put("businessDate", shift.getBusinessDate());
            summary.put("openedAt", shift.getOpenedAt());
            summary.put("closedAt", shift.getClosedAt());
            summary.put("openingFloat", shift.getOpeningFloat());
            summary.put("totalCashSales", totalCashSales);
            summary.put("totalNonCashSales", totalNonCashSales);
            summary.put("totalSales", totalCashSales.add(totalNonCashSales));
            summary.put("totalExpense", totalExpense);
            summary.put("totalCashIn", totalCashIn);
            summary.put("totalCashRemoved", totalCashRemoved);
            summary.put("totalPaidOut", totalPaidOut);
            summary.put("expectedCash", expectedCash);
            summary.put("closingCountedCash", shift.getClosingCountedCash());
            summary.put("cashDifference", shift.getCashDifference());

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentUserId(String authHeader) {
        try {
            if (authHeader == null)
                throw new RuntimeException("Authorization header is missing");
            if (authHeader.startsWith("Bearer "))
                authHeader = authHeader.substring(7);
            String username = jwtUtils.extractUsername(authHeader);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            return user.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from token: " + e.getMessage());
        }
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Map<String, Object> buildShiftMap(Shift s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("shiftCode", s.getShiftCode());
        map.put("shiftName", s.getShiftName());
        map.put("branchId", s.getBranchId());
        map.put("cashierId", s.getCashierId());
        map.put("openedBy", s.getOpenedBy());
        map.put("closedBy", s.getClosedBy());
        map.put("businessDate", s.getBusinessDate());
        map.put("openedAt", s.getOpenedAt());
        map.put("closedAt", s.getClosedAt());
        map.put("openingFloat", s.getOpeningFloat());
        map.put("closingCountedCash", s.getClosingCountedCash());
        map.put("expectedCash", s.getExpectedCash());
        map.put("cashDifference", s.getCashDifference());
        map.put("totalSales", s.getTotalSales());
        map.put("totalCashSales", s.getTotalCashSales());
        map.put("totalNonCashSales", s.getTotalNonCashSales());
        map.put("totalExpense", s.getTotalExpense());
        map.put("status", s.getStatus());
        map.put("note", s.getNote());
        return map;
    }
}