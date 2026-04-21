package com.emp_management.feature.birthday.service;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.shared.enums.EmployeeStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BirthdayEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;

    public BirthdayEmailService(JavaMailSender mailSender,
                                UserRepository userRepository,
                                EmployeePersonalDetailsRepository personalDetailsRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
    }

    public void sendBirthdayEmails() {
        LocalDate today = LocalDate.now();
        List<User> activeUsers = userRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);

        for (User user : activeUsers) {
            EmployeePersonalDetails details = personalDetailsRepository
                    .findByEmployee_EmpId(user.getEmployee().getEmpId())
                    .orElse(null);

            if (details == null || details.getDateOfBirth() == null) continue;

            LocalDate dob = details.getDateOfBirth();
            if (dob.getMonthValue() == today.getMonthValue()
        && dob.getDayOfMonth() == today.getDayOfMonth()) {

    // ✅ already sent today check
    if (details.getBirthEmailSentDate() != null &&
        details.getBirthEmailSentDate().isEqual(today)) {
        continue;
    }

    try {
        System.out.println("Sending birthday email to: " + user.getEmail());

        sendHtmlEmail(user.getEmail(), details.getFirstName(), details.getLastName());

        // ✅ mark as sent
        details.setBirthEmailSentDate(today);
        personalDetailsRepository.save(details);

    } catch (MessagingException e) {
        System.err.println("Failed to send birthday email to: " + user.getEmail());
    }
}
        }
    }

    private void sendHtmlEmail(String toEmail, String firstName, String lastName)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setFrom("hr@wenxttech.com");
        helper.setSubject("🎂 Happy Birthday, " + firstName + "!");
        helper.setText(buildHtmlTemplate(firstName, lastName), true);

        mailSender.send(message);
    }

    private String buildHtmlTemplate(String firstName, String lastName) {
    String name = firstName + " " + lastName;
    return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/></head>"
        + "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;\">"
        + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;padding:40px 0;\">"
        + "<tr><td align=\"center\">"
        + "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);\">"
        + "<tr><td align=\"center\" style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:50px 40px 40px;\">"
        + "<div style=\"font-size:72px;margin-bottom:10px;\">🎂</div>"
        + "<h1 style=\"margin:0;color:#ffffff;font-size:32px;font-weight:700;\">Happy Birthday!</h1>"
        + "<p style=\"margin:10px 0 0;color:rgba(255,255,255,0.85);font-size:16px;\">Wishing you an amazing day</p>"
        + "</td></tr>"
        + "<tr><td style=\"background:linear-gradient(90deg,#ff6b6b,#feca57,#48dbfb,#ff9ff3,#54a0ff);height:6px;\"></td></tr>"
        + "<tr><td style=\"padding:45px 50px 30px;\">"
        + "<p style=\"margin:0 0 20px;font-size:18px;color:#2d3436;font-weight:600;\">Dear " + name + ",</p>"
        + "<p style=\"margin:0 0 20px;font-size:15px;color:#636e72;line-height:1.8;\">On behalf of the entire <strong style=\"color:#667eea;\">Wenxt Team</strong>, we want to take a moment to celebrate <strong>YOU</strong> today! 🎉</p>"
        + "<p style=\"margin:0 0 30px;font-size:15px;color:#636e72;line-height:1.8;\">Your dedication, hard work, and positive energy make our workplace a better place every single day. Today is your special day — we hope it's filled with joy, laughter, and everything you love!</p>"
        + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
        + "<td style=\"background:linear-gradient(135deg,#f8f9ff 0%,#f0f0ff 100%);border-left:4px solid #667eea;border-radius:8px;padding:20px 25px;\">"
        + "<p style=\"margin:0;font-size:15px;color:#4a4a8a;line-height:1.8;font-style:italic;\">"
        + "\"May this birthday be the beginning of a year full of happiness, good health, and great success. Here's to you and all that you do! 🥳\""
        + "</p></td></tr></table>"
        + "<p style=\"text-align:center;font-size:32px;margin:30px 0;letter-spacing:8px;\">🎈 🎁 🎊 🌟 🎉</p>"
        + "</td></tr>"
        + "<tr><td style=\"background-color:#f8f9fa;padding:25px 50px;border-top:1px solid #e9ecef;\">"
        + "<p style=\"margin:0;font-size:14px;color:#adb5bd;text-align:center;\">With love & best wishes,<br/>"
        + "<strong style=\"color:#667eea;font-size:15px;\">The Wenxt HR Team</strong></p>"
        + "<p style=\"margin:12px 0 0;font-size:12px;color:#ced4da;text-align:center;\">This is an automated birthday greeting from the Wenxt HR System.</p>"
        + "</td></tr>"
        + "</table></td></tr></table></body></html>";
}
}