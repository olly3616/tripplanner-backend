package com.voyage.user.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.user.domain.User;
import com.voyage.user.dto.UserResponse;
import com.voyage.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** Returns the currently authenticated user's profile. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return UserResponse.from(user);
    }
}
