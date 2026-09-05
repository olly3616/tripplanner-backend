package com.voyage.share.web;

import com.voyage.share.dto.PublicTripSummary;
import com.voyage.share.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated read-only trip view. Authorization is the share token itself. */
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class PublicShareController {

    private final ShareService shareService;

    @GetMapping("/{token}")
    public PublicTripSummary view(@PathVariable String token,
                                  @RequestParam(required = false) String password) {
        return shareService.getPublicSummary(token, password);
    }
}
