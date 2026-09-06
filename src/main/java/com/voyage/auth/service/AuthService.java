package com.voyage.auth.service;

import com.voyage.auth.dto.LoginRequest;
import com.voyage.auth.dto.SignupRequest;
import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.jwt.JwtProperties;
import com.voyage.auth.jwt.JwtTokenProvider;
import com.voyage.auth.token.RefreshTokenStore;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.global.util.SecureTokens;
import com.voyage.user.domain.User;
import com.voyage.user.dto.UserResponse;
import com.voyage.user.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;
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
        String tokenHash = SecureTokens.sha256Hex(rawRefreshToken);
        Long userId = refreshTokenStore.findActiveUserId(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        // Rotate: revoke the presented token before issuing a new pair.
        refreshTokenStore.revoke(tokenHash);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenStore.revoke(SecureTokens.sha256Hex(rawRefreshToken));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = SecureTokens.newToken();
        refreshTokenStore.save(SecureTokens.sha256Hex(rawRefreshToken), user.getId(),
                Duration.ofSeconds(jwtProperties.refreshTokenTtlSeconds()));
        return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenTtlSeconds(), rawRefreshToken);
    }
}
