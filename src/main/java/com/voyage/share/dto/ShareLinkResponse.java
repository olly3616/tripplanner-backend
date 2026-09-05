package com.voyage.share.dto;

import com.voyage.share.domain.ShareLink;
import java.time.Instant;

/** {@code token} is populated only when the link is first created. */
public record ShareLinkResponse(
        Long id,
        String token,
        boolean includeExpenses,
        boolean hasPassword,
        Instant expiresAt,
        Instant createdAt
) {

    public static ShareLinkResponse created(ShareLink link, String rawToken) {
        return new ShareLinkResponse(link.getId(), rawToken, link.isIncludeExpenses(),
                link.hasPassword(), link.getExpiresAt(), link.getCreatedAt());
    }

    public static ShareLinkResponse summary(ShareLink link) {
        return new ShareLinkResponse(link.getId(), null, link.isIncludeExpenses(),
                link.hasPassword(), link.getExpiresAt(), link.getCreatedAt());
    }
}
