package es.hackUDC.slAIds.rest.common;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtGeneratorImpl implements JwtGenerator {

  private String signKey = "Bu:GW8bgPlEw";

  private long expirationMinutes = 1440;

  @Override
  public String generate(JwtInfo info) {

    return Jwts.builder()
        .claim("userId", info.getUserId())
        .claim("role", info.getRole())
        .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 *
            1000))
        .signWith(SignatureAlgorithm.HS512, signKey.getBytes())
        .compact();

  }

  @Override
  public JwtInfo getInfo(String token) {

    Claims claims = Jwts.parser()
        .setSigningKey(signKey.getBytes())
        .parseClaimsJws(token)
        .getBody();

    return new JwtInfo(
        ((Integer) claims.get("userId")).longValue(),
        claims.getSubject(),
        (String) claims.get("role"));

  }

}
