package com.emp_management.security;


import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        // try email first, fall back to empId — no fragile regex needed
        User user = userRepository.findByEmployee_Email(identifier)
                .or(() -> userRepository.findByEmployee_EmpId(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        return new CustomUserDetails(user);
    }
}
//org.sprin
// gframework.security.core.userdetails.User(
//        user.getEmail(),
//                user.getPasswordHash(),
//authorities