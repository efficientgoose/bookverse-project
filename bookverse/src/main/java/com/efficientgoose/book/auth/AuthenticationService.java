package com.efficientgoose.book.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.efficientgoose.book.user.Token;
import com.efficientgoose.book.user.TokenRepository;
import com.efficientgoose.book.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.efficientgoose.book.role.RoleRepository;
import com.efficientgoose.book.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public void register(RegistrationsRequest request) {
        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER was not initialised"));

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        sendValidationEmail(user);

    }

    private void sendValidationEmail(User user) {
        var newToken = generateAndSaveActivationToken(user);
        // send email
        
    }

    private String generateAndSaveActivationToken(User user) {
        // generate a token
        String generatedToken = generateActivationToken(6);
        var token = Token.builder()
                    .token(generatedToken)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .user(user)
                    .build();

        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationToken(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for(int i=0;i<length;i++){
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

}