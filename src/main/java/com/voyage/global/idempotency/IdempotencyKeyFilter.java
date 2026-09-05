package com.voyage.global.idempotency;

import com.voyage.auth.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Makes POSTs carrying an {@code Idempotency-Key} header safe to retry: the first
 * call's response (status + body) is stored per user and replayed on any repeat,
 * so an offline client resyncing a queued mutation never creates duplicates.
 * Runs after JWT authentication so the user is known.
 */
@RequiredArgsConstructor
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final int SERVER_ERROR = 500;

    private final IdempotencyRecordRepository repository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getHeader(HEADER);
        Long userId = currentUserId();
        if (!HttpMethod.POST.matches(request.getMethod()) || !StringUtils.hasText(key) || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyRecord> existing = repository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            writeStored(response, existing.get());
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        int status = wrapper.getStatus();
        if (status < SERVER_ERROR) {
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            try {
                repository.save(IdempotencyRecord.of(
                        userId, key, request.getMethod(), request.getRequestURI(), status, body));
            } catch (DataIntegrityViolationException concurrentStore) {
                // Another concurrent request already stored this key; keep this response.
            }
        }
        wrapper.copyBodyToResponse();
    }

    private void writeStored(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (StringUtils.hasText(record.getResponseBody())) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.id();
        }
        return null;
    }
}
