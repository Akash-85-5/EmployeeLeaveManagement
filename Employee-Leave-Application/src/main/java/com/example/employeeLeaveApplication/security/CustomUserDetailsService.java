package com.example.employeeLeaveApplication.security;

import com.example.employeeLeaveApplication.feature.auth.entity.User;
import com.example.employeeLeaveApplication.feature.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmployee_Email(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        // 🔥 IMPORTANT: ROLE_ PREFIX
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new CustomUserDetails(user);    }
}
//org.springframework.security.core.userdetails.User(
//        user.getEmail(),
//                user.getPasswordHash(),
//authorities