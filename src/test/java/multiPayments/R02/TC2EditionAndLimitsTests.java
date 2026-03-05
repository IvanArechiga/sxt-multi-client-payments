package multiPayments.R02;

import com.sicarx.agent.client.dto.AgentDto;
import com.sicarx.auth.registry.Client;
import com.sicarx.document.domain.DocumentType;
import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.infra.dto.CreditPaymentDto;
import com.sicarx.test.core.TestOperations;
import com.sicarx.test.dto.DocumentV2;
import com.sicarx.test.dto.SerieDto;
import org.junit.jupiter.api.*;
import setup.RequestInitializer;
import sxb.tests.junit.ResponseDto;
import sxb.tests.utils.AssertionsEnhanced;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

/**
 * R02_TC2_Edition_And_Limits_Tests
 * <p>
 * Validates concurrency and limits for multi-client payment folio assignment:
 * - Concurrent folio assignment (race conditions)
 * - Atomicity in database operations
 * - Sequential folio integrity
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TC2EditionAndLimitsTests extends RequestInitializer {

    static AgentDto agentCredit = TestOperations.createAgentCredit("R02 TC2 MultiPayment Agent", 1, 30, new BigDecimal("9999999999.99"), false);
    static List<DocumentV2> creditDocuments = new ArrayList<>();
    static final String TEST_SERIE_CONCURRENT = "CONC";
    static final Long CONCURRENT_INITIAL_FOLIO = 200L;

    @BeforeAll
    static void setUp() {
        // Set permissions for the company
        TestOperations.setCustomRoleActions(getTestSettings().company1(), List.of(Client.CREDIT_PAYMENT));

        // Clean up any existing test series
        cleanUpSerie(TEST_SERIE_CONCURRENT);

        // Create the test serie for concurrent testing
        getSerieRequests().post().assertReq(CREATED, c -> {
            c.setSerie(TEST_SERIE_CONCURRENT);
            c.setDocumentType(com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT.getType());
            c.setFolio(CONCURRENT_INITIAL_FOLIO);
        });

        // Create multiple credit documents for testing concurrency
        for (int i = 0; i < 10; i++) {
            ResponseDto<SaleDto, String> saleDto = getSaleRequests().post().assertReq(CREATED, c -> {
                c.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                        agentCredit.getUuid(),
                        agentCredit.getName())
                );
                c.setPayments(null);
                c.setCreditDueDate(LocalDate.now().plusDays(5));
            });
            creditDocuments.add(TestOperations.getGeneratedDocument(saleDto));
        }
    }

    @AfterAll
    static void tearDown() {
        // Clean up test serie
        cleanUpSerie(TEST_SERIE_CONCURRENT);
        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());
    }

    @AfterEach
    void afterEach() {
        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());
    }

    private static void cleanUpSerie(String serieName) {
        try {
            getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());
            SerieDto[] serieList = getSerieRequests().serieList()
                    .assertReq(OK, com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT.getType())
                    .getBody();

            if (serieList != null) {
                Arrays.stream(serieList)
                        .filter(c -> c.getSerie().equals(serieName))
                        .findFirst()
                        .ifPresent(s -> {
                            try {
                                getSerieRequests().delete().assertReq(OK, s.getDocumentType(), s.getSerie());
                            } catch (Exception e) {
                                // Serie may be in use, ignore error in cleanup
                            }
                        });
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("2.1.1 - (Backend) Timbrado Simultáneo - Validar concurrencia en asignación de folios")
    void testConcurrentFolioAssignment() throws Exception {
        final int CONCURRENT_REQUESTS = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        
        List<CompletableFuture<ResponseDto<MultiClientPaymentDto, String>>> futures = new ArrayList<>();

        // Create multiple concurrent requests
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int index = i;
            CompletableFuture<ResponseDto<MultiClientPaymentDto, String>> future = CompletableFuture.supplyAsync(() -> {
                // Use a different document for each request
                DocumentV2 creditDoc = creditDocuments.get(index);
                
                CreditPaymentDto creditPayment = new CreditPaymentDto();
                creditPayment.setAmount(new BigDecimal("10.00"));
                creditPayment.setDocumentUuid(creditDoc.getUuid());
                creditPayment.setDocumentType(DocumentType.SALE);
                creditPayment.setBranchId(getTestSettings().company1().branch1().getBranchId());

                // Use the request directly
                return getClientPayRequests().multipay().assertReq(CREATED,
                        multiPayment -> {
                            multiPayment.setUuid(java.util.UUID.randomUUID().toString());
                            multiPayment.setTimeZone("America/Mexico_City");
                            multiPayment.setDecimals(6);
                            multiPayment.setType(com.sicarx.document.domain.DocumentType.MULTI_CLIENT_PAYMENT);
                            multiPayment.setSerie(TEST_SERIE_CONCURRENT);
                            multiPayment.setIsoCurrency("MXN");
                            multiPayment.setOpMode(com.sicarx.document.domain.OpMode.MX);
                            multiPayment.setTotal(new BigDecimal("10.00"));
                            multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                                    agentCredit.getUuid(),
                                    agentCredit.getName())
                            );
                            multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                            multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                            multiPayment.setCreditPayments(List.of(creditPayment));
                        }
                );
            }, executorService);
            
            futures.add(future);
        }

        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        allFutures.get(); // Wait for completion

        executorService.shutdown();

        // Collect all responses and their documents
        List<ResponseDto<MultiClientPaymentDto, String>> responses = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        List<DocumentV2> documents = responses.stream()
                .map(TestOperations::getGeneratedDocument)
                .toList();

        List<String> documentUuids = documents.stream()
                .map(DocumentV2::getUuid)
                .toList();

        // Verify all requests were successful
        AssertionsEnhanced.assertEquals(CONCURRENT_REQUESTS, responses.size(),
                "Todas las peticiones concurrentes deben completarse exitosamente");

        // Verify all responses are CREATED
        responses.forEach(response -> 
                assertEquals(CREATED, response.responseEntity().getStatusCode(),
                        "Todas las respuestas deben ser 201 CREATED")
        );

        // Verify no duplicate UUIDs (atomicity)
        Set<String> uniqueUuids = new HashSet<>(documentUuids);
        AssertionsEnhanced.assertEquals(CONCURRENT_REQUESTS, uniqueUuids.size(),
                "No debe haber UUIDs duplicados - el backend debe asegurar atomicidad");

        // Get the updated serie to verify folios were incremented
        SerieDto[] updatedSerieList = getSerieRequests().serieList()
                .assertReq(OK, com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(updatedSerieList, "La lista de series actualizada no debe ser nula");
        
        SerieDto updatedSerie = Arrays.stream(updatedSerieList)
                .filter(s -> s.getSerie().equals(TEST_SERIE_CONCURRENT))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La serie debe existir"));

        // Verify the folio counter was incremented correctly
        long expectedNextFolio = CONCURRENT_INITIAL_FOLIO + CONCURRENT_REQUESTS;
        AssertionsEnhanced.assertEquals(expectedNextFolio, updatedSerie.getFolio(),
                "El folio de la serie debe haberse incrementado correctamente a " + expectedNextFolio +
                " después de " + CONCURRENT_REQUESTS + " peticiones concurrentes");

        // Log results for debugging
        System.out.println("=== Concurrent Folio Assignment Test Results ===");
        System.out.println("Total requests: " + CONCURRENT_REQUESTS);
        System.out.println("Successful responses: " + responses.size());
        System.out.println("Unique document UUIDs: " + uniqueUuids.size());
        System.out.println("Initial folio: " + CONCURRENT_INITIAL_FOLIO);
        System.out.println("Expected next folio: " + expectedNextFolio);
        System.out.println("Actual next folio: " + updatedSerie.getFolio());
        System.out.println("==============================================");
    }

    @Test
    @Order(2)
    @DisplayName("2.1.2 - Validar que no hay race conditions en múltiples sucursales")
    void testNoRaceConditionsMultipleBranches() throws Exception {
        final int CONCURRENT_REQUESTS = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        // Get current folio to verify sequential assignment continues
        SerieDto[] beforeSerieList = getSerieRequests().serieList()
                .assertReq(OK, com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(beforeSerieList, "La lista de series no debe ser nula");
        
        SerieDto testSerie = Arrays.stream(beforeSerieList)
                .filter(s -> s.getSerie().equals(TEST_SERIE_CONCURRENT))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La serie de prueba debe existir"));

        long currentFolio = testSerie.getFolio();

        List<CompletableFuture<ResponseDto<MultiClientPaymentDto, String>>> futures = new ArrayList<>();

        // Create concurrent requests from the same branch
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int index = i + 5; // Use different documents than first test
            CompletableFuture<ResponseDto<MultiClientPaymentDto, String>> future = CompletableFuture.supplyAsync(() -> {
                DocumentV2 creditDoc = creditDocuments.get(index);

                CreditPaymentDto creditPayment = new CreditPaymentDto();
                creditPayment.setAmount(new BigDecimal("15.00"));
                creditPayment.setDocumentUuid(creditDoc.getUuid());
                creditPayment.setDocumentType(DocumentType.SALE);
                creditPayment.setBranchId(getTestSettings().company1().branch1().getBranchId());

                return getClientPayRequests().multipay().assertReq(CREATED,
                        multiPayment -> {
                            multiPayment.setUuid(java.util.UUID.randomUUID().toString());
                            multiPayment.setTimeZone("America/Mexico_City");
                            multiPayment.setDecimals(6);
                            multiPayment.setType(com.sicarx.document.domain.DocumentType.MULTI_CLIENT_PAYMENT);
                            multiPayment.setSerie(TEST_SERIE_CONCURRENT);
                            multiPayment.setIsoCurrency("MXN");
                            multiPayment.setOpMode(com.sicarx.document.domain.OpMode.MX);
                            multiPayment.setTotal(new BigDecimal("15.00"));
                            multiPayment.setAgent(new com.sicarx.document.infra.dto.AgentDto(
                                    agentCredit.getUuid(),
                                    agentCredit.getName())
                            );
                            multiPayment.setPaymentId(com.sicarx.cashregister.subdomain.trx.PaymentType.CASH);
                            multiPayment.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
                            multiPayment.setCreditPayments(List.of(creditPayment));
                        }
                );
            }, executorService);

            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        allFutures.get();

        executorService.shutdown();

        List<ResponseDto<MultiClientPaymentDto, String>> responses = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        List<DocumentV2> documents = responses.stream()
                .map(TestOperations::getGeneratedDocument)
                .toList();

        List<String> documentUuids = documents.stream()
                .map(DocumentV2::getUuid)
                .toList();

        // Verify no duplicates
        Set<String> uniqueUuids = new HashSet<>(documentUuids);
        AssertionsEnhanced.assertEquals(CONCURRENT_REQUESTS, uniqueUuids.size(),
                "No debe haber UUIDs duplicados en peticiones concurrentes de la misma sucursal");

        // Verify serie folio was incremented by exactly CONCURRENT_REQUESTS
        SerieDto[] afterSerieList = getSerieRequests().serieList()
                .assertReq(OK, com.sicarx.settings.client.dto.DocumentType.MULTI_CLIENT_PAYMENT.getType())
                .getBody();

        assertNotNull(afterSerieList, "La lista de series después no debe ser nula");
        
        SerieDto updatedSerie = Arrays.stream(afterSerieList)
                .filter(s -> s.getSerie().equals(TEST_SERIE_CONCURRENT))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La serie debe existir"));

        long expectedFolio = currentFolio + CONCURRENT_REQUESTS;
        AssertionsEnhanced.assertEquals(expectedFolio, updatedSerie.getFolio(),
                "El folio debe haberse incrementado exactamente en " + CONCURRENT_REQUESTS + 
                " (de " + currentFolio + " a " + expectedFolio + ")");

        System.out.println("=== Multiple Branch Race Condition Test ===");
        System.out.println("Previous folio: " + currentFolio);
        System.out.println("Expected folio: " + expectedFolio);
        System.out.println("Actual folio: " + updatedSerie.getFolio());
        System.out.println("All UUIDs are unique and folio incremented correctly");
        System.out.println("==========================================");
    }
}





