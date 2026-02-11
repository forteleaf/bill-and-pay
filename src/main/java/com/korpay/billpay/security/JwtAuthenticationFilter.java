package com.korpay.billpay.security;

import com.korpay.billpay.domain.entity.AuthUser;
import com.korpay.billpay.repository.AuthUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AuthUserRepository authUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String tokenType = tokenProvider.getTokenType(jwt);

                if (JwtTokenProvider.TOKEN_TYPE_PLATFORM.equals(tokenType)) {
                    handlePlatformToken(jwt, request);
                } else {
                    handleTenantToken(jwt, request);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void handlePlatformToken(String jwt, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(jwt);
        String role = tokenProvider.getRoleFromToken(jwt);
        String adminId = tokenProvider.getAdminIdFromToken(jwt);

        String springRole = "ROLE_" + role;
        UserDetails userDetails = User.builder()
                .username(username)
                .password("")
                .authorities(List.of(new SimpleGrantedAuthority(springRole)))
                .build();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("platformAdminId", adminId);
        request.setAttribute("platformRole", role);
        log.debug("Set Platform Authentication for admin: {} (role: {})", username, role);
    }

    private void handleTenantToken(String jwt, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(jwt);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set Authentication for user: {}", username);

        Optional<AuthUser> authUser = authUserRepository.findByUsername(username);
        if (authUser.isPresent()) {
            String rawTenantId = authUser.get().getTenantId();
            String tenantId = rawTenantId.startsWith("tenant_") ? rawTenantId : "tenant_" + rawTenantId;
            request.setAttribute("tenantId", tenantId);
            log.debug("Set tenant context: {}", tenantId);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
