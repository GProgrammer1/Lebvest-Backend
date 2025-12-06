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
import org.springframework.security.core.Authentication;
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
        try {
            String requestPath = request.getRequestURI();
            String method = request.getMethod();
            log.info("JWT Filter - {} request to: {}", method, requestPath);
            
            // Log content type for multipart requests
            String contentType = request.getContentType();
            log.info("JWT Filter - Content-Type: {}", contentType);
            
            String authHeader = request.getHeader("Authorization");
            log.info("JWT Filter - Authorization header present: {}", authHeader != null);
            if (authHeader != null) {
                log.info("JWT Filter - Authorization header starts with Bearer: {}", authHeader.startsWith("Bearer "));
            }
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.info("JWT Filter - No valid Authorization header, allowing request to proceed");
                filterChain.doFilter(request, response);
                return;
            }
            
            String jwt = authHeader.substring(7);
            String username = null;
            try {
                username = jwtService.extractClaim(jwt, "access", Claims::getSubject);
                log.info("JWT Filter - Extracted username (email) from token: {}", username);
            } catch (Exception e) {
                log.warn("JWT Filter - Failed to extract username from token, continuing without authentication: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

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
                    // Log current authentication for debugging
                    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                    if (existingAuth != null) {
                        log.info("JWT Filter - Existing authentication: {}, authorities: {}", 
                                existingAuth.getName(), existingAuth.getAuthorities());
                    }
                }
            }
            
            // Log SecurityContext state before proceeding
            Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
            log.info("JWT Filter - Final SecurityContext authentication: {}", 
                    finalAuth != null ? finalAuth.getName() + " with authorities: " + finalAuth.getAuthorities() : "null");
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT Filter - Unexpected error in filter chain", e);
            filterChain.doFilter(request, response);
        }
    }
}
