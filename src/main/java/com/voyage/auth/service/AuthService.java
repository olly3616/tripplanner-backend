package com.voyage.auth.service;

import com.voyage.auth.domain.RefreshToken;
import com.voyage.auth.dto.LoginRequest;
import com.voyage.auth.dto.SignupRequest;
import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.jwt.JwtProperties;
import com.voyage.auth.jwt.JwtTokenProvider;
import com.voyage.auth.repository.RefreshTokenRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.user.domain.User;
import com.voyage.user.dto.UserResponse;
import com.voyage.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        User user = User.createEmailUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.defaultCurrency(),
                request.timezone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!stored.isActive(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        // Rotate: revoke the presented token before issuing a new pair.
        stored.revoke(Instant.now());
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = RefreshTokenGenerator.generate();
        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds());
        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), TokenHasher.sha256Hex(rawRefreshToken), expiresAt));
        return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenTtlSeconds(), rawRefreshToken);
    }
}
