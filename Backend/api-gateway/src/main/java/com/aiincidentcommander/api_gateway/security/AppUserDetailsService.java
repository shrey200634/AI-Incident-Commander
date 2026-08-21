package com.aiincidentcommander.api_gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-username:admin}")
    private String adminUsername;

    @Value("${app.security.admin-password:admin123}")
    private String adminPassword;

    private Map<String, AppUser> users;

    @PostConstruct
    private void seedUsers() {
        users = Map.of(
                adminUsername, AppUser.builder()
                        .userName(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .roles(List.of("ADMIN"))
                        .build()
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = users.get(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.builder()
                .username(appUser.getUserName())
                .password(appUser.getPassword())
                .authorities(appUser.getRoles().stream()
                        .map(role -> "ROLE_" + role)
                        .toArray(String[]::new))
                .build();
    }
}