package setup.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilidad para trabajar con JWT tokens
 */
public class JWTUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Deserializa y muestra el contenido de un JWT token
     * 
     * @param token El JWT token completo
     * @return Objeto JWTDecoded con la información del token
     */
    public static JWTDecoded decodeJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT token inválido. Debe tener 3 partes separadas por '.'");
            }

            // Decodificar header (parte 0)
            String headerJson = decodeBase64(parts[0]);
            JsonNode header = objectMapper.readTree(headerJson);

            // Decodificar payload (parte 1)
            String payloadJson = decodeBase64(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadJson);

            // La firma (parte 2) se mantiene como está
            String signature = parts[2];

            return new JWTDecoded(header, payload, signature);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al decodificar JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Decodifica un string Base64
     */
    private static String decodeBase64(String base64) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Imprime de forma legible el contenido del JWT
     */
    public static void printJWT(String token) {
        JWTDecoded decoded = decodeJWT(token);
        System.out.println(decoded);
    }

    /**
     * Devuelve un nuevo token con el claim `exp` actualizado a `newExpEpochSeconds`.
     * El token se firma utilizando HMAC-SHA512 con la clave proporcionada.
     *
     * @param token token original
     * @param newExpEpochSeconds nuevo valor de exp en segundos epoch
     * @param hmacSecret secreto para firmar (UTF-8)
     * @return nuevo token JWT firmado
     */
    public static String withUpdatedExpiration(String token, long newExpEpochSeconds, String hmacSecret) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("JWT token inválido");

            // Decodificar header y payload
            String headerJson = decodeBase64(parts[0]);
            String payloadJson = decodeBase64(parts[1]);

            JsonNode header = objectMapper.readTree(headerJson);
            ObjectNode payload = (ObjectNode) objectMapper.readTree(payloadJson);

            // Cambiar exp
            payload.put("exp", newExpEpochSeconds);

            // Re-serializar y codificar en Base64 URL-safe sin padding
            String headerB64 = base64UrlEncodeNoPadding(objectMapper.writeValueAsBytes(header));
            String payloadB64 = base64UrlEncodeNoPadding(objectMapper.writeValueAsBytes(payload));

            String signingInput = headerB64 + "." + payloadB64;

            // Calcular firma HMAC-SHA512
            String signatureB64 = hmacSha512Base64Url(signingInput, hmacSecret);

            return signingInput + "." + signatureB64;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar exp del JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience: expirar el token ahora (timestamp actual)
     */
    public static String expireNow(String token, String hmacSecret) {
        long now = Instant.now().getEpochSecond();
        return withUpdatedExpiration(token, now, hmacSecret);
    }

    private static String base64UrlEncodeNoPadding(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacSha512Base64Url(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        mac.init(keySpec);
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncodeNoPadding(sig);
    }

    /**
     * Actualiza el claim `exp` del token al valor de 5 minutos antes del momento actual
     * y devuelve el token reconstruido conservando la firma original.
     *
     * IMPORTANTE: sin la clave secreta original no es posible recalcular una firma
     * válida para tokens con algoritmos HMAC/HS*. Este método conserva la firma
     * original (parte 3) para permitir generar tokens "simulados" en tests, pero
     * la firma será inválida para cualquier verificación criptográfica.
     *
     * @param token token JWT original
     * @return token JWT con `exp` = now - 5 minutos (firma original conservada)
     */
    public static String expireToken5MinutesBeforeNow(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("JWT token inválido");

            String headerJson = decodeBase64(parts[0]);
            String payloadJson = decodeBase64(parts[1]);

            JsonNode header = objectMapper.readTree(headerJson);
            ObjectNode payload = (ObjectNode) objectMapper.readTree(payloadJson);

            long newExp = Instant.now().getEpochSecond() - 300; // 5 minutos antes
            payload.put("exp", newExp);

            String headerB64 = base64UrlEncodeNoPadding(objectMapper.writeValueAsBytes(header));
            String payloadB64 = base64UrlEncodeNoPadding(objectMapper.writeValueAsBytes(payload));

            // Conservamos la firma original (parts[2]) porque no disponemos del secreto
            return headerB64 + "." + payloadB64 + "." + parts[2];
        } catch (Exception e) {
            throw new RuntimeException("Error al expirar token: " + e.getMessage(), e);
        }
    }

    /**
     * Clase que representa un JWT decodificado
     */
    public static class JWTDecoded {
        private final JsonNode header;
        private final JsonNode payload;
        private final String signature;

        public JWTDecoded(JsonNode header, JsonNode payload, String signature) {
            this.header = header;
            this.payload = payload;
            this.signature = signature;
        }

        public JsonNode getHeader() {
            return header;
        }

        public JsonNode getPayload() {
            return payload;
        }

        public String getSignature() {
            return signature;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=============== JWT TOKEN DECODIFICADO ===============\n");
            
            sb.append("\n--- HEADER ---\n");
            sb.append(formatJson(header));
            
            sb.append("\n\n--- PAYLOAD ---\n");
            sb.append(formatJson(payload));
            
            // Decodificar campos específicos del payload
            sb.append("\n\n--- CAMPOS DECODIFICADOS ---\n");
            
            if (payload.has("b")) {
                sb.append("Branch ID (b): ").append(payload.get("b").asText()).append("\n");
            }
            if (payload.has("c")) {
                sb.append("Company UUID (c): ").append(payload.get("c").asText()).append("\n");
            }
            if (payload.has("exp")) {
                long exp = payload.get("exp").asLong();
                sb.append("Expiration (exp): ").append(exp)
                  .append(" -> ").append(formatTimestamp(exp)).append("\n");
            }
            if (payload.has("iat")) {
                long iat = payload.get("iat").asLong();
                sb.append("Issued At (iat): ").append(iat)
                  .append(" -> ").append(formatTimestamp(iat)).append("\n");
            }
            if (payload.has("jti")) {
                sb.append("JWT ID / Email (jti): ").append(payload.get("jti").asText()).append("\n");
            }
            if (payload.has("u")) {
                sb.append("User UUID (u): ").append(payload.get("u").asText()).append("\n");
            }
            if (payload.has("ua")) {
                sb.append("User Alias (ua): ").append(payload.get("ua").asText()).append("\n");
            }
            if (payload.has("p")) {
                sb.append("Profile/Permission (p): ").append(payload.get("p").asText()).append("\n");
            }
            if (payload.has("r")) {
                sb.append("Role UUID (r): ").append(payload.get("r").asText()).append("\n");
            }
            if (payload.has("n")) {
                sb.append("Field n: ").append(payload.get("n").asText()).append("\n");
            }
            if (payload.has("i")) {
                sb.append("Field i: ").append(payload.get("i").asText()).append("\n");
            }
            if (payload.has("x")) {
                sb.append("Field x: ").append(payload.get("x").asText()).append("\n");
            }
            
            sb.append("\n--- SIGNATURE (últimos 20 caracteres) ---\n");
            sb.append("...").append(signature.substring(Math.max(0, signature.length() - 20))).append("\n");
            
            sb.append("\n======================================================\n");
            
            return sb.toString();
        }

        private String formatJson(JsonNode node) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            } catch (Exception e) {
                return node.toString();
            }
        }

        private String formatTimestamp(long timestamp) {
            return DATE_FORMATTER.format(Instant.ofEpochSecond(timestamp));
        }
    }
}

