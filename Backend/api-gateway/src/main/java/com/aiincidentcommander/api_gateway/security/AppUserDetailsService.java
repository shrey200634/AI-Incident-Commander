package com.aiincidentcommander.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;


    private Map<String, AppUser> users;

    @jakarta.annotation.PostConstruct
    private void seedUsers() {
        users = Map.of(
                "admin", AppUser.builder()
                        .userName("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .roles(java.util.List.of("ADMIN"))
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
