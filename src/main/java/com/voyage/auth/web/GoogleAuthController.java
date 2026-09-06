package com.voyage.auth.web;

import com.voyage.auth.dto.GoogleLoginRequest;
import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.service.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping
    public TokenResponse login(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.login(request.idToken());
    }
}
