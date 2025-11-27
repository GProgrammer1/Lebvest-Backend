package com.lebvest.filter;

import com.lebvest.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(final JwtService jwtService, final UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    /**
     *
     * @param request: incoming request from which to extract the tokens (if present), validate them, and take appropriate actions
     *
     * @param response: return response
     * @param filterChain: apply filter to request
     * @throws ServletException: throws if there was an error dispatching or forwarding the request
     * @throws IOException: throws if there is a problem writing to response or reading from request
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String jwt;
        String authHeader = request.getHeader("Authorization");
        log.debug("JWT Filter - Request path: {}, Authorization header present: {}", 
                request.getRequestURI(), authHeader != null);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("JWT Filter - No valid Authorization header, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);
        
        try {
            String username = jwtService.extractClaim(jwt, "access", Claims::getSubject);
            log.info("JWT Filter - Extracted username (email) from token: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.info("JWT Filter - User found: {}, roles: {}", username, userDetails.getAuthorities());
                    
                    if (jwtService.validateToken(jwt, "access", userDetails)) {
                        log.info("JWT Filter - Token validated successfully for user: {}", username);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("JWT Filter - Authentication set in SecurityContext for user: {}", username);
                    } else {
                        log.warn("JWT Filter - Token validation failed for user: {}", username);
                    }
                } catch (Exception e) {
                    log.error("JWT Filter - Error loading user by username: {}", username, e);
                }
            } else {
                if (username == null) {
                    log.warn("JWT Filter - Username is null, cannot authenticate");
                } else {
                    log.debug("JWT Filter - Authentication already exists in SecurityContext");
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
