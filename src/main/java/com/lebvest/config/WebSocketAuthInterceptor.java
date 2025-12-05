package com.lebvest.config;

import com.lebvest.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = getTokenFromHeaders(accessor);
            
            if (token != null) {
                try {
                    // Extract username from token
                    String username = jwtService.extractClaim(token, "access", claims -> claims.getSubject());
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Validate token
                    if (jwtService.validateToken(token, "access", userDetails)) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                        log.debug("WebSocket authentication successful for user: {}", username);
                    } else {
                        log.warn("WebSocket connection attempt with invalid token for user: {}", username);
                        // Reject the connection by setting an error
                        accessor.setLeaveMutable(true);
                        accessor.getSessionAttributes().put("STOMP_ERROR", "Invalid token");
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication failed: {}", e.getMessage());
                    // Reject the connection
                    accessor.setLeaveMutable(true);
                    accessor.getSessionAttributes().put("STOMP_ERROR", "Authentication failed");
                }
            } else {
                log.warn("WebSocket connection attempt with missing token");
                // Reject the connection
                accessor.setLeaveMutable(true);
                accessor.getSessionAttributes().put("STOMP_ERROR", "Missing token");
            }
        }
        
        return message;
    }

    private String getTokenFromHeaders(StompHeaderAccessor accessor) {
        // Try to get token from Authorization header
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try to get token from query parameter (for SockJS)
        String query = accessor.getFirstNativeHeader("query");
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        
        return null;
    }
}

