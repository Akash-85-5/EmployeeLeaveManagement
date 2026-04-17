package com.emp_management.feature.birthday.service;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.birthday.dto.BirthdayEmployeeDTO;
import com.emp_management.feature.birthday.dto.BirthdayWishDTO;
import com.emp_management.feature.birthday.dto.SendWishRequest;
import com.emp_management.feature.birthday.entity.BirthdayWish;
import com.emp_management.feature.birthday.repository.BirthdayWishRepository;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.shared.enums.EmployeeStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
public class BirthdayService {

    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final BirthdayWishRepository birthdayWishRepository;

    public BirthdayService(UserRepository userRepository,
                           EmployeePersonalDetailsRepository personalDetailsRepository,
                           BirthdayWishRepository birthdayWishRepository) {
        this.userRepository = userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
        this.birthdayWishRepository = birthdayWishRepository;
    }

    public List<BirthdayEmployeeDTO> getTodayBirthdays() {
        LocalDate today = LocalDate.now();
        return getBirthdaysForRange(today, today);
    }

    public List<BirthdayEmployeeDTO> getWeeklyBirthdays() {
        LocalDate today = LocalDate.now();
        return getBirthdaysForRange(today, today.plusDays(6));
    }

    public List<BirthdayEmployeeDTO> getMonthlyBirthdays() {
        LocalDate today = LocalDate.now();
        return getBirthdaysForRange(today, today.withDayOfMonth(today.lengthOfMonth()));
    }

    // ✅ FIXED → Long → String conversion
    public List<BirthdayWishDTO> getWishes(String birthdayEmployeeId) {
        int year = Year.now().getValue();
        return birthdayWishRepository
                .findByBirthdayEmployeeIdAndWishYearOrderByCreatedAtDesc(
                        String.valueOf(birthdayEmployeeId), year)
                .stream()
                .map(this::toWishDTO)
                .toList();
    }

    // ✅ CLEANED
    public void sendWish(SendWishRequest request) {

        int year = Year.now().getValue();

        String birthdayEmployeeId = request.getBirthdayEmployeeId(); // String
        String wishedById = request.getWishedByEmployeeId(); // String

        boolean alreadyWished = birthdayWishRepository
                .existsByBirthdayEmployeeIdAndWishedByEmployeeIdAndWishYear(
                        birthdayEmployeeId, wishedById, year);

        if (alreadyWished) {
            throw new IllegalStateException("You have already wished this employee this year.");
        }

        User wisher = userRepository.findByEmployee_EmpId(wishedById)
                .orElseThrow(() -> new IllegalArgumentException("Wisher not found"));

        EmployeePersonalDetails wisherDetails = personalDetailsRepository
                .findByEmployee_EmpId(wisher.getEmployee().getEmpId())
                .orElse(null);

        String wisherName = (wisherDetails != null)
                ? wisherDetails.getFirstName() + " " + wisherDetails.getLastName()
                : wisher.getEmployee().getName();

        BirthdayWish wish = new BirthdayWish();

        // ✅ IMPORTANT FIX
        wish.setBirthdayEmployeeId(birthdayEmployeeId);
        wish.setWishedByEmployeeId(wishedById);

        wish.setWishedByName(wisherName);
        wish.setWishedByRole(wisher.getEmployee().getRole().getRoleName());
        wish.setWishMessage(request.getWishMessage());
        wish.setWishYear(year);
        wish.setSystemWish(false);

        birthdayWishRepository.save(wish);
    }

    // ✅ CORRECT
    public boolean hasAlreadyWished(String birthdayEmployeeId, String wishedByEmployeeId) {
        return birthdayWishRepository.existsByBirthdayEmployeeIdAndWishedByEmployeeIdAndWishYear(
                birthdayEmployeeId, wishedByEmployeeId, Year.now().getValue());
    }

    // ✅ FULL FIXED
    public void sendSystemWishes() {

        LocalDate today = LocalDate.now();
        int currentYear = Year.now().getValue();

        List<User> activeUsers = userRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);

        for (User user : activeUsers) {

            EmployeePersonalDetails details = personalDetailsRepository
                    .findByEmployee_EmpId(user.getEmployee().getEmpId())
                    .orElse(null);

            if (details == null || details.getDateOfBirth() == null) continue;

            LocalDate dob = details.getDateOfBirth();

            if (dob.getMonthValue() == today.getMonthValue()
                    && dob.getDayOfMonth() == today.getDayOfMonth()) {

                String birthdayEmployeeId = String.valueOf(user.getEmployee().getEmpId()); // ✅ FIX
                String systemSenderId = "0";

                boolean alreadySent = birthdayWishRepository
                        .existsByBirthdayEmployeeIdAndWishedByEmployeeIdAndWishYear(
                                birthdayEmployeeId,
                                systemSenderId,
                                currentYear);

                if (!alreadySent) {
                    BirthdayWish wish = new BirthdayWish();

                    wish.setBirthdayEmployeeId(birthdayEmployeeId); // ✅ FIX
                    wish.setWishedByEmployeeId(systemSenderId);

                    wish.setWishedByName("HR System");
                    wish.setWishedByRole("SYSTEM");
                    wish.setWishMessage("Wishing you a very Happy Birthday! 🎂 May this special day bring you joy, success, and everything you deserve. From the entire team!");
                    wish.setWishYear(currentYear);
                    wish.setSystemWish(true);

                    birthdayWishRepository.save(wish);
                }
            }
        }
    }

    private List<BirthdayEmployeeDTO> getBirthdaysForRange(LocalDate from, LocalDate to) {

        List<User> activeUsers = userRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);
        int currentYear = LocalDate.now().getYear();
        List<BirthdayEmployeeDTO> result = new ArrayList<>();

        for (User user : activeUsers) {

            EmployeePersonalDetails details = personalDetailsRepository
                    .findByEmployee_EmpId(user.getEmployee().getEmpId())
                    .orElse(null);

            if (details == null || details.getDateOfBirth() == null) continue;

            LocalDate dob = details.getDateOfBirth();
            LocalDate birthdayThisYear;

            try {
                birthdayThisYear = LocalDate.of(currentYear, dob.getMonthValue(), dob.getDayOfMonth());
            } catch (Exception e) {
                birthdayThisYear = LocalDate.of(currentYear, dob.getMonthValue(), 28);
            }

            if (!birthdayThisYear.isBefore(from) && !birthdayThisYear.isAfter(to)) {

                int age = Period.between(dob, LocalDate.now()).getYears();

                result.add(new BirthdayEmployeeDTO(
                        user.getId(),
                        user.getEmployee().getEmpId(),
                        details.getFirstName(),
                        details.getLastName(),
                        user.getEmployee().getDepartment().getDepartmentName(),
                        dob.toString(),
                        age,
                        null
                ));
            }
        }

        result.sort((a, b) -> {
            LocalDate da = LocalDate.parse(a.getDateOfBirth());
            LocalDate db2 = LocalDate.parse(b.getDateOfBirth());
            int cmp = Integer.compare(da.getMonthValue(), db2.getMonthValue());
            if (cmp != 0) return cmp;
            return Integer.compare(da.getDayOfMonth(), db2.getDayOfMonth());
        });

        return result;
    }

    private BirthdayWishDTO toWishDTO(BirthdayWish wish) {

        BirthdayWishDTO dto = new BirthdayWishDTO();

        dto.setId(wish.getId());
        dto.setBirthdayEmployeeId(wish.getBirthdayEmployeeId());
        dto.setWishedByEmployeeId(wish.getWishedByEmployeeId());
        dto.setWishedByName(wish.getWishedByName());
        dto.setWishedByRole(wish.getWishedByRole());
        dto.setWishMessage(wish.getWishMessage());
        dto.setWishYear(wish.getWishYear());
        dto.setSystemWish(wish.isSystemWish());
        dto.setCreatedAt(wish.getCreatedAt());

        return dto;
    }
}