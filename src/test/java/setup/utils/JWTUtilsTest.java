package setup.utils;

import org.junit.jupiter.api.Test;
import java.time.Instant;

public class JWTUtilsTest {

    @Test
    void testDecodeJWT() {
        String token = "eyJhbGciOiJIUzUxMiIsInQiOjJ9.eyJiIjoxNDI1OTAsImMiOiIzN2NlYmVkNC1kMTQxLTRhMWMtODE5YS0zMDQwNTk1OTIxZWQiLCJleHAiOjE3NzI1ODc4MTksImkiOiJub25lIiwiaWF0IjoxNzcyNTU5MDE5LCJqdGkiOiJpdmFuQHNpY2FyLm14IiwibiI6IjMiLCJwIjoiQURNSU4iLCJyIjoiNjkwNjI3MWQtZWM4NS00OGRmLWJkYTgtNTRjNWFkNDU5NGRlIiwidSI6Ijc3MjhiYTc2LTI0ZjMtNDBhNS04ODA2LTQzZGJmZDBjZWE0YSIsInVhIjoiSVZBTiIsIngiOjM4OTE2fQ.XXHKjgRLN4rtNOeZ-OppEVhILNmI7i-_jdtJVO3-ajjX9T4geb2Z6yhVcVjqN_HqUdGvTTSZTerYoGU9rrFc0A";
        
        JWTUtils.printJWT(token);
        
        // También acceso directo a campos
        JWTUtils.JWTDecoded decoded = JWTUtils.decodeJWT(token);
        System.out.println("\n=== ACCESO DIRECTO A CAMPOS ===");
        System.out.println("Email (jti): " + decoded.getPayload().get("jti").asText());
        System.out.println("User Alias (ua): " + decoded.getPayload().get("ua").asText());
        System.out.println("Profile (p): " + decoded.getPayload().get("p").asText());
        System.out.println("Company UUID (c): " + decoded.getPayload().get("c").asText());
        System.out.println("Branch ID (b): " + decoded.getPayload().get("b").asInt());
    }

    @Test
    void testUpdateExpiration() {
        String token = "eyJhbGciOiJIUzUxMiIsInQiOjJ9.eyJiIjoxNDI1OTAsImMiOiIzN2NlYmVkNC1kMTQxLTRhMWMtODE5YS0zMDQwNTk1OTIxZWQiLCJleHAiOjE3NzI1ODc4MTksImkiOiJub25lIiwiaWF0IjoxNzcyNTU5MDE5LCJqdGkiOiJpdmFuQHNpY2FyLm14IiwibiI6IjMiLCJwIjoiQURNSU4iLCJyIjoiNjkwNjI3MWQtZWM4NS00OGRmLWJkYTgtNTRjNWFkNDU5NGRlIiwidSI6Ijc3MjhiYTc2LTI0ZjMtNDBhNS04ODA2LTQzZGJmZDBjZWE0YSIsInVhIjoiSVZBTiIsIngiOjM4OTE2fQ.XXHKjgRLN4rtNOeZ-OppEVhILNmI7i-_jdtJVO3-ajjX9T4geb2Z6yhVcVjqN_HqUdGvTTSZTerYoGU9rrFc0A";

        // Secreto de prueba (en producción debes usar la clave real)
        String testSecret = "test-secret-key";

        // Nuevo exp: ahora - 60 segundos (expirado)
        long newExp = Instant.now().getEpochSecond() - 60;
        String newToken = JWTUtils.withUpdatedExpiration(token, newExp, testSecret);

        JWTUtils.JWTDecoded decoded = JWTUtils.decodeJWT(newToken);
        long expAfter = decoded.getPayload().get("exp").asLong();

        System.out.println("Original exp (approx): 1772587819");
        System.out.println("New exp: " + expAfter);

        // Verificar que el exp fue actualizado
        org.junit.jupiter.api.Assertions.assertEquals(newExp, expAfter);

        // También probar expireNow
        String expiredNow = JWTUtils.expireNow(token, testSecret);
        System.out.println("expiredNow = " + expiredNow);
        JWTUtils.JWTDecoded dec2 = JWTUtils.decodeJWT(expiredNow);
        long expNow = dec2.getPayload().get("exp").asLong();
        long now = Instant.now().getEpochSecond();
        // expNow debe ser aproximadamente <= now (pequeña diferencia por ejecución)
        org.junit.jupiter.api.Assertions.assertTrue(expNow <= now + 1);
    }

    @Test
    void testExpireToken5MinutesBeforeNow() {
        String token = "eyJhbGciOiJIUzUxMiIsInQiOjJ9.eyJiIjoxNDI1OTAsImMiOiIzN2NlYmVkNC1kMTQxLTRhMWMtODE5YS0zMDQwNTk1OTIxZWQiLCJleHAiOjE3NzI1ODc4MTksImkiOiJub25lIiwiaWF0IjoxNzcyNTU5MDE5LCJqdGkiOiJpdmFuQHNpY2FyLm14IiwibiI6IjMiLCJwIjoiQURNSU4iLCJyIjoiNjkwNjI3MWQtZWM4NS00OGRmLWJkYTgtNTRjNWFkNDU5NGRlIiwidSI6Ijc3MjhiYTc2LTI0ZjMtNDBhNS04ODA2LTQzZGJmZDBjZWE0YSIsInVhIjoiSVZBTiIsIngiOjM4OTE2fQ.XXHKjgRLN4rtNOeZ-OppEVhILNmI7i-_jdtJVO3-ajjX9T4geb2Z6yhVcVjqN_HqUdGvTTSZTerYoGU9rrFc0A";

        String newToken = JWTUtils.expireToken5MinutesBeforeNow(token);
        JWTUtils.JWTDecoded decoded = JWTUtils.decodeJWT(newToken);
        long exp = decoded.getPayload().get("exp").asLong();
        long expected = Instant.now().getEpochSecond() - 300;

        System.out.println("exp after: " + exp + " expected approx: " + expected);

        // Permitir diferencia de hasta 2 segundos por ejecución
        org.junit.jupiter.api.Assertions.assertTrue(Math.abs(exp - expected) <= 2,
                "El claim exp debe ser ahora-300s (5 minutos antes)");
    }
}

