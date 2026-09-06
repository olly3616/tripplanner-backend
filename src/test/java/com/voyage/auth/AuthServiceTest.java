package com.voyage.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.auth.dto.LoginRequest;
import com.voyage.auth.dto.SignupRequest;
import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.jwt.JwtProperties;
import com.voyage.auth.jwt.JwtTokenProvider;
import com.voyage.auth.service.AuthService;
import com.voyage.auth.token.RefreshTokenStore;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.user.domain.User;
import com.voyage.user.dto.UserResponse;
import com.voyage.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenStore, passwordEncoder,
                jwtTokenProvider, new JwtProperties("secret", 900, 1_209_600));
    }

    @Test
    void signup_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("minji@voyage.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.signup(new SignupRequest("minji@voyage.com", "password1", "Minji", null, null)));

        assertEquals(ErrorCode.EMAIL_DUPLICATED, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_encodesPasswordAndAppliesDefaults() {
        when(userRepository.existsByEmail("minji@voyage.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response =
                authService.signup(new SignupRequest("minji@voyage.com", "password1", "Minji", null, null));

        assertEquals("minji@voyage.com", response.email());
        assertEquals("KRW", response.defaultCurrency());
        assertEquals("Asia/Seoul", response.timezone());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("ENCODED", saved.getValue().getPasswordHash());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = User.createEmailUser("minji@voyage.com", "ENCODED", "Minji", null, null);
        when(userRepository.findByEmail("minji@voyage.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "ENCODED")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("minji@voyage.com", "wrong")));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@voyage.com")).thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("ghost@voyage.com", "password1")));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void login_success_returnsTokensAndPersistsRefresh() {
        User user = User.createEmailUser("minji@voyage.com", "ENCODED", "Minji", null, null);
        when(userRepository.findByEmail("minji@voyage.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password1", "ENCODED")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(), any())).thenReturn("ACCESS");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = authService.login(new LoginRequest("minji@voyage.com", "password1"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("ACCESS", response.accessToken());
        assertEquals(900L, response.accessTokenExpiresIn());
        assertNotNull(response.refreshToken());
        verify(refreshTokenStore).save(any(), any(), any());
    }
}
