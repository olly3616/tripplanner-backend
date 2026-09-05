package com.voyage.share.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.share.dto.CreateShareLinkRequest;
import com.voyage.share.dto.ShareLinkResponse;
import com.voyage.share.service.ShareService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareService shareService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShareLinkResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long tripId,
                                    @Valid @RequestBody(required = false) CreateShareLinkRequest request) {
        CreateShareLinkRequest body = request != null
                ? request : new CreateShareLinkRequest(null, null, null);
        return shareService.create(principal.id(), tripId, body);
    }

    @GetMapping
    public List<ShareLinkResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long tripId) {
        return shareService.list(principal.id(), tripId);
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long tripId,
                       @PathVariable Long linkId) {
        shareService.revoke(principal.id(), tripId, linkId);
    }
}
