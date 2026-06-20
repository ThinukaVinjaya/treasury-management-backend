package com.batch.treasury_management.security;

import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService for Spring Security Authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("Invalid username or password");
                });

        // Security checks
        if (!user.isActive()) {
            log.warn("Login attempt on deactivated account: {}", username);
            throw new UsernameNotFoundException("Invalid username or password");
        }

        if (user.isDeleted()) {
            log.warn("Login attempt on deleted account: {}", username);
            throw new UsernameNotFoundException("Invalid username or password");
        }

        // Build Spring Security UserDetails
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole())
                        )
                );

        log.debug("User loaded successfully: {} | Role: {}", username, user.getRole());
        return userDetails;
    }
}