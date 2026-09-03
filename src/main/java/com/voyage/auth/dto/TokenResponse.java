package com.voyage.auth.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {

    public static TokenResponse of(String accessToken, long accessTokenExpiresIn, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, accessTokenExpiresIn, refreshToken);
    }
}
