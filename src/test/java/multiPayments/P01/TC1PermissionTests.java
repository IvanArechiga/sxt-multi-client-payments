package multiPayments.P01;

import com.sicarx.agent.client.dto.AgentDto;
import com.sicarx.auth.registry.Client;
import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.test.core.TestOperations;
import com.sicarx.test.dto.DocumentV2;
import org.junit.jupiter.api.*;
import setup.RequestInitializer;
import sxb.tests.junit.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

/**
 * P01_TC1_Permission_Tests
 *
 * Validate that the multi payment endpoint enforces permissions correctly for different user roles and scenarios.
 * This includes testing with users that have the Client.CREDIT_PAYMENT permission, users without it,
 * and edge cases like expired tokens or tokens from other environments.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TC1PermissionTests extends RequestInitializer {
    static DocumentV2 creditDocument1;
    static DocumentV2 creditDocument2;
    static AgentDto agentCredit = TestOperations.createAgentCredit("MultiPayment Agent", 1, 30, new BigDecimal("9999999999.99"), false);

    @BeforeAll
    static void setUp() {
        ResponseDto<SaleDto, String> saleDto1 = getSaleRequests().post().assertReq(CREATED, c -> {
            c.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                    agentCredit.getUuid(),
                    agentCredit.getName())
            );
            c.setPayments(null);
            c.setCreditDueDate(LocalDate.now().plusDays(5));
        });
        creditDocument1 = TestOperations.getGeneratedDocument(saleDto1);

        ResponseDto<SaleDto, String> saleDto2 = getSaleRequests().post().assertReq(CREATED, c -> {
            c.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                    agentCredit.getUuid(),
                    agentCredit.getName())
            );
            c.setPayments(null);
            c.setCreditDueDate(LocalDate.now().plusDays(5));
        });
        creditDocument2 = TestOperations.getGeneratedDocument(saleDto2);
    }

    @AfterEach
    void afterEach() {
        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());
    }

    @Test
    @Order(1)
    @DisplayName("1.1.1 - Usuario CON permiso puede crear Multi Abonos")
    void testUserWithPermission_CanCreateMultiPayment() {
        TestOperations.setCustomRoleActions(getTestSettings().company1(),
                List.of(Client.CREDIT_PAYMENT));

        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(OK,
                multiPayment -> {
                    multiPayment.setTotal(java.math.BigDecimal.valueOf(457.5));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );

                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());

                    // Configurar pagos de crédito (lista vacía por ahora para prueba de permisos)
                    multiPayment.setCreditPayments(java.util.List.of());
                    multiPayment.setSeriesPayments("");
                }
        );

        // Verificaciones
        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals(OK, response.responseEntity().getStatusCode(),
                "Debe responder con éxito (200 OK)");

    }

    @Test
    @Order(2)
    @DisplayName("1.1.2 - Usuario SIN permiso recibe 403 FORBIDDEN")
    void testUserWithoutPermission_ReceivesForbidden() {

    }

    @Test
    @Order(3)
    @DisplayName("1.1.3 - Permiso respeta scope de sucursal")
    void testPermission_RespectsbranchOfficeScope() {

    }

    @Test
    @Order(4)
    @DisplayName("1.1.4 - Token expirado recibe 401 UNAUTHORIZED")
    void testExpiredToken_ReturnsUnauthorized() {

    }

    @Test
    @Order(5)
    @DisplayName("1.1.5 - Petición sin token recibe 401 UNAUTHORIZED")
    void testNoAuthToken_ReturnsUnauthorized() {

    }

    @Test
    @Order(6)
    @DisplayName("1.1.6 - Token inválido/malformado recibe 401 UNAUTHORIZED")
    void testInvalidToken_ReturnsUnauthorized() {

    }
}
