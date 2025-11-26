package com.lebvest.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.access-secret}")
    private String JWT_ACCESS_SECRET;

    @Value("${jwt.refresh-secret}")
    private String JWT_REFRESH_SECRET;


    public boolean validateToken(String token, String type, UserDetails userDetails) {
        var expiration = extractClaim(token, type, Claims::getExpiration);
        var username = extractClaim(token, type, Claims::getSubject);

        return expiration != null && expiration.after(new Date()) && (
                userDetails.getUsername().equals(username)
                );
    }

    public <T> T extractClaim(String token, String type, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = parseToken(token, type);
        return claimsResolver.apply(claims);
    }

    public Claims parseToken(String token, String type) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey(type))
                .build()
                .parseClaimsJws(token)
                .getBody();    }



    public String generateToken(UserDetails userDetails, String type, Long id) {
        String username = userDetails.getUsername();
        log.info("JwtService - Generating token for user: {}, userId: {}, type: {}", username, id, type);
        String token = Jwts
                .builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .claim("userId", id)
                .setExpiration(type.equals("refresh") ?
                        new Date(System.currentTimeMillis() + 24 * 7 * 60 * 60 * 1000) :
                        new Date(System.currentTimeMillis() + 60 * 60 * 1000))
                .signWith(getSigningKey(type))
                .compact();
        log.info("JwtService - Token generated successfully for user: {}", username);
        return token;
    }

    public SecretKey getSigningKey(String type) {
        return type.equals("refresh") ? Keys.hmacShaKeyFor(JWT_REFRESH_SECRET.getBytes()) : Keys.hmacShaKeyFor(JWT_ACCESS_SECRET.getBytes());
    }
}
