package multiPayments.R01;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sicarx.agent.client.dto.AgentDto;
import com.sicarx.auth.registry.Client;
import com.sicarx.auth.registry.Settings;
import com.sicarx.document.domain.DocumentType;
import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.infra.dto.CreditPaymentDto;
import com.sicarx.test.core.TestOperations;
import com.sicarx.test.dto.DocumentV2;
import org.junit.jupiter.api.*;
import setup.RequestInitializer;
import sxb.tests.junit.ResponseDto;
import sxb.tests.utils.AssertionsEnhanced;
import sxb.tests.utils.BodyError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

/**
 * P01_TC1_Permission_Tests
 * <p>
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

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());


        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(CREATED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(CREATED, response.responseEntity().getStatusCode(),
                        "Debe responder con éxito (200 OK)")
        );
    }

    @Test
    @Order(2)
    @DisplayName("1.1.2 - Usuario SIN permiso recibe 401 UNAUTHORIZED")
    void testUserWithoutPermission_ReceivesForbidden() throws JsonProcessingException {
        TestOperations.setCustomRoleActions(getTestSettings().company1(),
                List.of(Settings.READ_WAREHOUSE));

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());

        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(UNAUTHORIZED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        BodyError bodyError = getObjectMapper().readValue(response.responseEntity().getBody(), BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(UNAUTHORIZED, response.responseEntity().getStatusCode(),
                        "Debe responder con 401 UNAUTHORIZED para usuario sin permiso"),
                () -> assertEquals(0, bodyError.getCode()),
                () -> assertEquals("UNAUTHORIZED", bodyError.getMessage())
        );
    }

    //TODO Add method to create another branch
    @Test
    @Order(3)
    @DisplayName("1.1.3 - Permiso respeta scope de sucursal")
    void testPermission_respectsBranchOfficeScope() {
        TestOperations.setCustomRoleActions(getTestSettings().company2(),
                List.of(Client.CREDIT_PAYMENT));

        getRestTemplate().withToken(getTestSettings().company2().branch1().jwt());

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());

        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(OK,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(OK, response.responseEntity().getStatusCode(),
                        "Debe responder con éxito (200 OK)")
        );
    }

    @Test
    @Order(4)
    @DisplayName("1.1.4 - Token expirado recibe 401 UNAUTHORIZED")
    void testExpiredToken_ReturnsUnauthorized() throws JsonProcessingException {
        getRestTemplate().withToken("eyJhbGciOiJIUzUxMiIsInQiOjJ9.eyJiIjoxNDI1OTAsImMiOiIzN2NlYmVkNC1kMTQxLTRhMWMtODE5YS0zMDQwNTk1OTIxZWQiLCJleHAiOjE3NzI1NzU4MzIsImkiOiJub25lIiwiaWF0IjoxNzcyNTU5MDE5LCJqdGkiOiJpdmFuQHNpY2FyLm14IiwibiI6IjMiLCJwIjoiQURNSU4iLCJyIjoiNjkwNjI3MWQtZWM4NS00OGRmLWJkYTgtNTRjNWFkNDU5NGRlIiwidSI6Ijc3MjhiYTc2LTI0ZjMtNDBhNS04ODA2LTQzZGJmZDBjZWE0YSIsInVhIjoiSVZBTiIsIngiOjM4OTE2fQ.irBt8gTgfJlNlLUdPeUeh_oqvTs5-be21D2UYPxcKJ9fWpy-601iP3R53obVVHUxccjyQrBY25TtCbNfB7g9jw");

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());

        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(UNAUTHORIZED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        BodyError bodyError = getObjectMapper().readValue(response.responseEntity().getBody(), BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(UNAUTHORIZED, response.responseEntity().getStatusCode(),
                        "Debe responder con 401 UNAUTHORIZED para usuario sin permiso"),
                () -> assertEquals(3, bodyError.getCode()),
                () -> assertEquals("UNAUTHORIZED", bodyError.getMessage())
        );
    }

    @Test
    @Order(5)
    @DisplayName("1.1.5 - Petición sin token recibe 401 UNAUTHORIZED")
    void testNoAuthToken_ReturnsUnauthorized() throws JsonProcessingException {
        getRestTemplate().withToken("");

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());

        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(UNAUTHORIZED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        BodyError bodyError = getObjectMapper().readValue(response.responseEntity().getBody(), BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(UNAUTHORIZED, response.responseEntity().getStatusCode(),
                        "Debe responder con 401 UNAUTHORIZED para usuario sin permiso"),
                () -> assertEquals(5, bodyError.getCode()),
                () -> assertEquals("UNAUTHORIZED", bodyError.getMessage())
        );
    }

    @Test
    @Order(6)
    @DisplayName("1.1.6 - Token inválido/malformado recibe 401 UNAUTHORIZED")
    void testInvalidToken_ReturnsUnauthorized() throws JsonProcessingException {
        TestOperations.setCustomRoleActions(getTestSettings().company1(),
                List.of(Client.CREDIT_PAYMENT));

        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt() + "invalid");

        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("5.00"));
        creditPayment2.setDocumentUuid(creditDocument2.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());


        ResponseDto<MultiClientPaymentDto, String> response = getClientPayRequests().multipay().assertReq(UNAUTHORIZED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(java.util.List.of(creditPayment1, creditPayment2));
                    multiPayment.setSeriesPayments("");
                }
        );

        BodyError bodyError = getObjectMapper().readValue(response.responseEntity().getBody(), BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(UNAUTHORIZED, response.responseEntity().getStatusCode(),
                        "Debe responder con 401 UNAUTHORIZED para usuario sin permiso"),
                () -> assertEquals(3, bodyError.getCode()),
                () -> assertEquals("UNAUTHORIZED", bodyError.getMessage())
        );
    }
}
