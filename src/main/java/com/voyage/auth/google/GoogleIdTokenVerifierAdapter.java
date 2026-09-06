package com.voyage.auth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Verifies Google ID tokens against Google's public keys (signature, audience,
 * issuer, expiry). Active only when {@code voyage.auth.google.client-id} is set.
 */
@Component
@ConditionalOnProperty(name = "voyage.auth.google.client-id")
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(@Value("${voyage.auth.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleUser verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String name = (String) payload.get("name");
            return new GoogleUser(payload.getSubject(), payload.getEmail(),
                    name != null ? name : payload.getEmail());
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
