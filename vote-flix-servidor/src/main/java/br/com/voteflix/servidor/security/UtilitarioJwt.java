package br.com.voteflix.servidor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
// import io.jsonwebtoken.security.Keys; // Esta linha foi removida

// Imports adicionados
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class UtilitarioJwt {

    // 1. SUBSTITUA PELA CHAVE DA SUA TURMA
    private static final String CHAVE_SECRETA_BASE64 = "bWV1LXNlZ3JlZG8tbXVpdG8tZm9ydGUtcGFyYS12b3RlZmxpeC0yMDI1";

    // 2. Esta linha recria a chave a partir da string acima
    private static final Key CHAVE_SECRETA = new SecretKeySpec(Base64.getDecoder().decode(CHAVE_SECRETA_BASE64),
            SignatureAlgorithm.HS256.getJcaName());

    private static final long TEMPO_EXPIRACAO = 86400000; // 24 horas em milissegundos

    public static String gerarToken(String id, String login, String funcao) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + TEMPO_EXPIRACAO);

        return Jwts.builder()
                .setSubject(login)
                .claim("id", id)
                .claim("funcao", funcao)
                .setIssuedAt(agora)
                .setExpiration(dataExpiracao)
                .signWith(CHAVE_SECRETA)
                .compact();
    }

    /**
     * Valida um token JWT. Se o token for válido, retorna os claims (dados).
     * Caso contrário, retorna null.
     */
    public static Claims validarToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(CHAVE_SECRETA)
                    .build()
                    .parseClaimsJws(token);
            return claimsJws.getBody();
        } catch (Exception e) {
            // Token inválido (expirado, assinatura incorreta, etc.)
            return null;
        }
    }
}