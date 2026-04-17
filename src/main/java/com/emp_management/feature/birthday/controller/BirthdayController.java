package com.emp_management.feature.birthday.controller;

import com.emp_management.feature.birthday.dto.BirthdayEmployeeDTO;
import com.emp_management.feature.birthday.dto.BirthdayWishDTO;
import com.emp_management.feature.birthday.dto.SendWishRequest;
import com.emp_management.feature.birthday.service.BirthdayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/birthday")
public class BirthdayController {

private final BirthdayService birthdayService;

public BirthdayController(BirthdayService birthdayService) {
    this.birthdayService = birthdayService;
    
}

    /** GET /api/v1/birthday/today */
    @GetMapping("/today")
    public ResponseEntity<List<BirthdayEmployeeDTO>> getTodayBirthdays() {
        return ResponseEntity.ok(birthdayService.getTodayBirthdays());
    }

    /** GET /api/v1/birthday/weekly */
    @GetMapping("/weekly")
    public ResponseEntity<List<BirthdayEmployeeDTO>> getWeeklyBirthdays() {
        return ResponseEntity.ok(birthdayService.getWeeklyBirthdays());
    }

    /** GET /api/v1/birthday/monthly */
    @GetMapping("/monthly")
    public ResponseEntity<List<BirthdayEmployeeDTO>> getMonthlyBirthdays() {
        return ResponseEntity.ok(birthdayService.getMonthlyBirthdays());
    }

    /** GET /api/v1/birthday/wishes/{userId} */
    @GetMapping("/wishes/{userId}")
    public ResponseEntity<List<BirthdayWishDTO>> getWishes(@PathVariable String userId) {
        return ResponseEntity.ok(birthdayService.getWishes(userId));
    }

    /**
     * GET /api/v1/birthday/wishes/check
     * ?birthdayEmployeeId=1&wishedByEmployeeId=2
     */
    @GetMapping("/wishes/check")
    public ResponseEntity<Boolean> checkAlreadyWished(
            @RequestParam String birthdayEmployeeId,
            @RequestParam String wishedByEmployeeId) {
        return ResponseEntity.ok(birthdayService.hasAlreadyWished(birthdayEmployeeId, wishedByEmployeeId));
    }

   /** POST /api/v1/birthday/wishes */
    @PostMapping("/wishes")
    public ResponseEntity<Void> sendWish(@Valid @RequestBody SendWishRequest req) {
        birthdayService.sendWish(req);
        return ResponseEntity.ok().build();
    }

    
}
