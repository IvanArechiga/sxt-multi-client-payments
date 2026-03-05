package multiPayments.R02;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sicarx.agent.client.dto.AgentDto;
import com.sicarx.auth.registry.Client;
import com.sicarx.document.domain.DocumentType;
import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.infra.dto.CreditPaymentDto;
import com.sicarx.settings.client.dto.SeriesDto;
import com.sicarx.test.core.TestOperations;
import com.sicarx.test.dto.DocumentV2;
import com.sicarx.test.dto.SerieDto;
import jdk.jfr.Threshold;
import org.junit.jupiter.api.*;
import setup.RequestInitializer;
import sxb.tests.junit.ResponseDto;
import sxb.tests.utils.AssertionsEnhanced;
import sxb.tests.utils.BodyError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

/**
 * R02_TC1_Serie_And_Folio_Configuration_Tests
 * <p>
 * Validates serie and folio configuration for multi-client payments including:
 * - Serie creation
 * - Consecutive folio assignment
 * - Validation of serie deletion when in use
 * - Payload validation for serie names
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TC1SerieAndFolioConfigurationTests extends RequestInitializer {
    
    static AgentDto agentCredit = TestOperations.createAgentCredit("R02 MultiPayment Agent", 1, 30, new BigDecimal("9999999999.99"), false);
    static DocumentV2 creditDocument1;
    static DocumentV2 creditDocument2;
    static final String TEST_SERIE = "NCRE";
    static final Long INITIAL_FOLIO = 100L;

    @BeforeAll
    static void setUp() {
        // Clean up any existing test series
        cleanUpSerie(TEST_SERIE);
        
        // Create credit documents for testing
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

    @AfterAll
    static void tearDown() {
        cleanUpSerie(TEST_SERIE);
    }

    @AfterEach
    void afterEach() {
        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());
    }

    private static void cleanUpSerie(String serieName) {
        try {
            SerieDto[] serieList = getSerieRequests().serieList()
                    .assertReq(OK, MULTI_CLIENT_PAYMENT.getType())
                    .getBody();

            if (serieList != null) {
                Arrays.stream(serieList)
                        .filter(c -> c.getSerie().equals(serieName))
                        .findFirst()
                        .ifPresent(s -> {
                            try {
                                getSerieRequests().delete().assertReq(OK, s.getDocumentType(), s.getSerie());
                            } catch (Exception e) {
                                System.out.println("Error deleting serie " + serieName + ": " + e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1.1.1 - Crear nueva serie")
    void testCreateNewSerie() {
        getSerieRequests().post().assertReq(CREATED, c -> {
            c.setSerie(TEST_SERIE);
            c.setDocumentType(MULTI_CLIENT_PAYMENT.getType());
            c.setFolio(INITIAL_FOLIO);
        });

        SerieDto[] serieList = getSerieRequests().serieList()
                .assertReq(OK, MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(serieList, "La lista de series no debe ser nula");
        
        Arrays.stream(serieList)
                .filter(d -> d.getSerie().equals(TEST_SERIE))
                .findFirst()
                .ifPresentOrElse(
                        serie -> {
                            AssertionsEnhanced.assertEquals(TEST_SERIE, serie.getSerie(), 
                                    "La serie NCRE no se creó correctamente");
                            AssertionsEnhanced.assertEquals(
                                    MULTI_CLIENT_PAYMENT.getType(),
                                    serie.getDocumentType(), 
                                    "El tipo de documento de la serie es incorrecto");
                            AssertionsEnhanced.assertEquals(INITIAL_FOLIO, serie.getFolio(), 
                                    "El folio inicial de la serie es incorrecto");
                        },
                        () -> AssertionsEnhanced.fail("La serie NCRE no existe después de crearla")
                );
    }

    @Test
    @Order(2)
    @DisplayName("1.1.2 - Folio Consecutivo")
    void testConsecutiveFolio() throws InterruptedException {
        int initFolio = Objects.requireNonNull(getSerieRequests().getSerie().assertReq(OK, MULTI_CLIENT_PAYMENT.getType(), "")
                .getBody()).getFolio().intValue();

        // Create first multi-payment
        CreditPaymentDto creditPayment1 = new CreditPaymentDto();
        creditPayment1.setAmount(new BigDecimal("10.00"));
        creditPayment1.setDocumentUuid(creditDocument1.getUuid());
        creditPayment1.setDocumentType(DocumentType.SALE);
        creditPayment1.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment12 = new CreditPaymentDto();
        creditPayment12.setAmount(new BigDecimal("5.00"));
        creditPayment12.setDocumentUuid(creditDocument2.getUuid());
        creditPayment12.setDocumentType(DocumentType.SALE);
        creditPayment12.setBranchId(getTestSettings().company1().branch1().getBranchId());

        ResponseDto<MultiClientPaymentDto, String> response1 = getClientPayRequests().multipay().assertReq(CREATED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("15.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(List.of(creditPayment1, creditPayment12));
                    multiPayment.setSeriesPayments("");
                    multiPayment.setSerie("");
                }
        );

        DocumentV2 firstDocument = TestOperations.getGeneratedDocument(response1);

        assertEquals(initFolio, firstDocument.getFolio(), "El folio del primer documento debe ser el folio inicial");

        // Create second multi-payment
        CreditPaymentDto creditPayment2 = new CreditPaymentDto();
        creditPayment2.setAmount(new BigDecimal("10.00"));
        creditPayment2.setDocumentUuid(creditDocument1.getUuid());
        creditPayment2.setDocumentType(DocumentType.SALE);
        creditPayment2.setBranchId(getTestSettings().company1().branch1().getBranchId());

        CreditPaymentDto creditPayment22 = new CreditPaymentDto();
        creditPayment22.setAmount(new BigDecimal("10.00"));
        creditPayment22.setDocumentUuid(creditDocument2.getUuid());
        creditPayment22.setDocumentType(DocumentType.SALE);
        creditPayment22.setBranchId(getTestSettings().company1().branch1().getBranchId());

        Thread.sleep(1000); // Sleep to ensure timestamp difference for folio incrementation if needed
        ResponseDto<MultiClientPaymentDto, String> response2 = getClientPayRequests().multipay().assertReq(CREATED,
                multiPayment -> {
                    multiPayment.setTotal(new BigDecimal("20.00"));
                    multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                            agentCredit.getUuid(),
                            agentCredit.getName())
                    );
                    multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                    multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                    multiPayment.setCreditPayments(List.of(creditPayment2, creditPayment22));
                    multiPayment.setSeriesPayments("");
                    multiPayment.setSerie("");
                }
        );

        DocumentV2 secondDocument = TestOperations.getGeneratedDocument(response2);

        assertEquals(initFolio + 1, secondDocument.getFolio(), "El folio del primer documento debe ser el folio inicial + 1");

        // Get the series folio from backend to verify incrementation
        SerieDto[] updatedSerieList = getSerieRequests().serieList()
                .assertReq(OK, MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(updatedSerieList, "La lista de series actualizada no debe ser nula");
        
        SerieDto updatedSerie = Arrays.stream(updatedSerieList)
                .filter(s -> s.getSerie().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("La serie debe existir"));

        AssertionsEnhanced.assertAll(
                () -> assertNotNull(firstDocument, "El primer documento no debe ser nulo"),
                () -> assertNotNull(secondDocument, "El segundo documento no debe ser nulo"),
                () -> assertEquals(CREATED, response1.responseEntity().getStatusCode(),
                        "La primera petición debe retornar 201 CREATED"),
                () -> assertEquals(CREATED, response2.responseEntity().getStatusCode(),
                        "La segunda petición debe retornar 201 CREATED"),
                () -> assertEquals(initFolio + 1, updatedSerie.getFolio() - 1,
                        "El folio de la serie debe haberse incrementado a " + (initFolio + 1) +
                        " después de crear dos multi-abonos"),
                () -> assertNotEquals(firstDocument.getUuid(), secondDocument.getUuid(),
                        "Los UUIDs de los documentos deben ser diferentes")
        );
    }

    @Test
    @Order(3)
    @DisplayName("1.1.3 - Eliminar serie en uso")
    void testDeleteSerieInUse() throws JsonProcessingException {
        // Try to delete the serie that was used in previous test
        org.springframework.http.ResponseEntity<String> response = getSerieRequests().delete().assertReq(BAD_REQUEST, 
                MULTI_CLIENT_PAYMENT.getType(),
                TEST_SERIE);

        String responseBody = response.getBody();
        assertNotNull(responseBody, "El cuerpo de la respuesta no debe ser nulo");
        
        BodyError bodyError = getObjectMapper().readValue(responseBody, BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertEquals(BAD_REQUEST, response.getStatusCode(),
                        "Debe responder con 400 BAD REQUEST al intentar eliminar una serie en uso"),
                () -> assertNotNull(bodyError, "El body de error no debe ser nulo"),
                () -> assertTrue(
                        bodyError.getMessage().toLowerCase().contains("use") || 
                        bodyError.getMessage().toLowerCase().contains("usado") ||
                        bodyError.getMessage().toLowerCase().contains("used"),
                        "El mensaje debe indicar que la serie está en uso")
        );

        // Verify the serie still exists
        SerieDto[] serieList = getSerieRequests().serieList()
                .assertReq(OK, MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(serieList, "La lista de series no debe ser nula");
        assertTrue(
                Arrays.stream(serieList).anyMatch(s -> s.getSerie().equals(TEST_SERIE)),
                "La serie debe permanecer activa después del intento de eliminación"
        );
    }

    @Test
    @Order(4)
    @DisplayName("1.1.4 - (Backend) Validar Payload Serie con caracteres especiales")
    void testValidateSeriePayloadWithSpecialCharacters() throws JsonProcessingException {
        String invalidSerieName = "NCRE@#";

        ResponseDto<?, String> response = getSerieRequests().post().assertReq(BAD_REQUEST, c -> {
            c.setSerie(invalidSerieName);
            c.setDocumentType(MULTI_CLIENT_PAYMENT.getType());
            c.setFolio(1L);
        });

        String responseBody = response.responseEntity().getBody();
        assertNotNull(responseBody, "El cuerpo de la respuesta no debe ser nulo");
        
        BodyError bodyError = getObjectMapper().readValue(responseBody, BodyError.class);

        AssertionsEnhanced.assertAll(
                () -> assertEquals(BAD_REQUEST, response.responseEntity().getStatusCode(),
                        "Debe responder con 400 BAD REQUEST para serie con caracteres especiales"),
                () -> assertNotNull(bodyError, "El body de error no debe ser nulo"),
                () -> assertTrue(
                        bodyError.getMessage().toLowerCase().contains("format") || 
                        bodyError.getMessage().toLowerCase().contains("valid") ||
                        bodyError.getMessage().toLowerCase().contains("invalid") ||
                        bodyError.getMessage().toLowerCase().contains("caracter"),
                        "El mensaje debe indicar error de validación de formato")
        );

        // Verify the invalid serie was not created
        SerieDto[] serieList = getSerieRequests().serieList()
                .assertReq(OK, MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        if (serieList != null) {
            assertFalse(
                    Arrays.stream(serieList).anyMatch(s -> s.getSerie().equals(invalidSerieName)),
                    "La serie con caracteres especiales no debe existir"
            );
        }
    }
}







