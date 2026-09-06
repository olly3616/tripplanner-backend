package com.voyage.auth.service;

import com.voyage.auth.dto.TokenResponse;
import com.voyage.auth.google.GoogleTokenVerifier;
import com.voyage.auth.google.GoogleUser;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.user.domain.User;
import com.voyage.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google sign-in for a stateless API: the client sends a Google ID token, we
 * verify it and find-or-create the matching user, then issue our own tokens.
 * If Google is not configured, the endpoint reports it is unavailable.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final ObjectProvider<GoogleTokenVerifier> verifierProvider;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public TokenResponse login(String idToken) {
        GoogleTokenVerifier verifier = verifierProvider.getIfAvailable();
        if (verifier == null) {
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
        }
        GoogleUser googleUser = verifier.verify(idToken);
        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> userRepository.save(User.createEmailUser(
                        googleUser.email(), null, googleUser.name(), null, null)));
        return tokenIssuer.issue(user);
    }
}
