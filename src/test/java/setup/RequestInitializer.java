package setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sicarx.agent.client.dto.AddressCreateDto;
import com.sicarx.agent.client.dto.AgentCreditCreateDto;
import com.sicarx.agent.client.dto.ClientCreateDto;
import com.sicarx.agent.client.dto.CreditListCreateDto;
import com.sicarx.cashregister.infra.dto.CashRegClosingDto;
import com.sicarx.cashregister.infra.dto.CashRegTransactionDto;
import com.sicarx.cashregister.infra.dto.CashRegisterEditDto;
import com.sicarx.department.client.dto.CategoryCreateDto;
import com.sicarx.department.client.dto.CategoryUpdateDto;
import com.sicarx.department.client.dto.DepartmentCreateDto;
import com.sicarx.department.client.dto.DepartmentUpdateDto;
import com.sicarx.document.dto.cfdi.DatosFacturacionIngresoCreateDto;
import com.sicarx.document.dto.clientpayment.ClientPaymentCancellationDto;
import com.sicarx.document.dto.clientpayment.ClientPaymentDto;
import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;
import com.sicarx.document.dto.orders.OrderDto;
import com.sicarx.document.dto.purchases.PurchaseCancellationDto;
import com.sicarx.document.dto.purchases.PurchaseDto;
import com.sicarx.document.dto.quotations.QuotationCancellationDto;
import com.sicarx.document.dto.quotations.QuotationDto;
import com.sicarx.document.dto.sales.SaleCancellationDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.dto.supplierpayment.SupplierPaymentCancellationDto;
import com.sicarx.document.dto.supplierpayment.SupplierPaymentDto;
import com.sicarx.document.infra.dto.DocCommentEditDto;
import com.sicarx.document.infra.dto.DocEmployeeEditDto;
import com.sicarx.product.client.dto.ProductCreateDto;
import com.sicarx.product.client.dto.ProductUpdateDto;
import com.sicarx.stock.client.dto.StockCreateDto;
import com.sicarx.stock.client.dto.StockUpdateDto;
import com.sicarx.stock.client.dto.WarehouseCreateDto;
import com.sicarx.tax.client.dto.TaxEditDto;
import com.sicarx.test.core.ServiceTestRestTemplateProvider;
import com.sicarx.test.core.TestEnvironmentProvider;
import com.sicarx.test.core.TestSettingsProvider;
import com.sicarx.test.core.model.TestEnvironment;
import com.sicarx.test.core.model.TestSettings;
import com.sicarx.test.dto.*;
import com.sicarx.test.requests.*;
import com.sicarx.unit.client.dto.UnitCreateDto;
import com.sicarx.unit.client.dto.UnitUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import setup.templates.PaymentTemplates;
import setup.templates.SalesTemplates;
import sxb.tests.spring.ServiceTestRestTemplate;


import java.util.ArrayList;


public class RequestInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RequestInitializer.class);

    private static TestSettings testSettings;
    private static TestEnvironment testEnvironment;
    private static ServiceTestRestTemplate restTemplate;
    private static SaleRequests saleRequests;
    private static PurchasesRequests purchaseRequests;
    private static QuotationsRequests quotationsRequests;
    private static SalesReportRequests salesReportRequests;
    private static SerieRequests serieRequests;
    private static CompanyRequests companyRequests;
    private static CashRegisterRequests cashRegisterRequests;
    private static CashRegQueriesRequests cashRegQueriesRequests;
    private static CashRegBalancingQueriesRequests cashRegBalancingQueriesRequests;
    private static AgentClientRequests agentClientRequests;
    private static CreditListRequests creditRequests;
    private static CategoryRequests catReq;
    private static DepartmentRequests depReq;
    private static UnitRequests unitReq;
    private static ProductRequests prodReq;
    private static StockRequests stockReq;
    private static PriceListRequests priceListRequests;
    private static ClientPaymentRequests clientPayRequests;
    private static CfdiRequests cfdiRequests;
    private static ClientPaymentRequests paymentRequests;
    private static DocumentQueriesRequests documentGraph;
    private static SupplierPaymentRequests supplierPayRequest;
    private static CreditListRequests creditListRequest;
    private static OrdersRequests ordersRequests;
    private static SettingsRequests settingsRequests;
    private static TaxRequest taxRequest;

    static BranchOfficeRequests branchOfficeRequests;


    private static ObjectMapper objectMapper;

    static {
        initializeRequests();
    }

    public static void initializeRequests() {
        logger.info("Inicializacion de Requests Iniciada");
        //TODO reemplazar con ObjectMapper personalizado segun sus necesidades, por ejemplo, con formateos de fechas, etc.
        objectMapper = new ObjectMapper();

        testSettings = TestSettingsProvider.instanceOf();
        testEnvironment = TestEnvironmentProvider.instanceOf();
        logger.info("Entorno de: %s".formatted(testEnvironment.environment().name()));

        restTemplate = ServiceTestRestTemplateProvider.instanceOf();
        restTemplate.getHeaders().set("Content-Type", "application/json");
        restTemplate.withToken(testSettings.company1().branch1().jwt());
        restTemplate.setPrintRequestBody(true);
        restTemplate.setPrintResponseBody(true);

        saleRequests = new SaleRequests();
        saleRequests.post().setDtoSupplier(SalesTemplates::getSaleDtoDefault);
        saleRequests.post().setJsonDtoPath("json/simpleSaleMinimumFields.json");
        saleRequests.postCancel().setDtoSupplier(SaleCancellationDto::new);
        saleRequests.editSeller().setDtoSupplier(DocEmployeeEditDto::new);
        saleRequests.editComment().setDtoSupplier(DocCommentEditDto::new);

        purchaseRequests = new PurchasesRequests();
        purchaseRequests.post().setDtoSupplier(PurchaseDto::new);
        purchaseRequests.post().setJsonDtoPath("json/simplePurchaseMinimumFields.json.json");
        purchaseRequests.postCancel().setDtoSupplier(PurchaseCancellationDto::new);

        quotationsRequests = new QuotationsRequests();
        quotationsRequests.post().setDtoSupplier(QuotationDto::new);
        quotationsRequests.post().setJsonDtoPath("json/okQuotation.json");
        quotationsRequests.postCancel().setDtoSupplier(QuotationCancellationDto::new);

        serieRequests = new SerieRequests();
        serieRequests.post().setDtoSupplier(() -> new SerieCreateDto("A", 1L, 1));

        companyRequests = new CompanyRequests();
        companyRequests.post().setJsonDtoPath("json/userCreate.json");
        companyRequests.post().generateDtoFromJson("json/userCreate.json", ObjectNode.class);

        branchOfficeRequests = new BranchOfficeRequests();
        branchOfficeRequests.post().setDtoSupplier(BranchOfficeCreateDto::new);


        clientPayRequests = new ClientPaymentRequests();
        clientPayRequests.post().setDtoSupplier(ClientPaymentDto::new);
        clientPayRequests.multipay().setDtoSupplier(PaymentTemplates::getMultiPaymentDtoDefault);
        clientPayRequests.post().setJsonDtoPath("json/ClientPayment.json");
        clientPayRequests.cancel().setDtoSupplier(ClientPaymentCancellationDto::new);

        supplierPayRequest = new SupplierPaymentRequests();
        supplierPayRequest.post().setDtoSupplier(SupplierPaymentDto::new);
        supplierPayRequest.cancel().setDtoSupplier(SupplierPaymentCancellationDto::new);

        cashRegisterRequests = new CashRegisterRequests();
        cashRegisterRequests.postTransaction().setDtoSupplier(CashRegTransactionDto::new);
        cashRegisterRequests.patch().setDtoSupplier(CashRegisterEditDto::new);
        cashRegisterRequests.postClose().setDtoSupplier(CashRegClosingDto::new);

        cashRegQueriesRequests = new CashRegQueriesRequests();

        cashRegBalancingQueriesRequests = new CashRegBalancingQueriesRequests();

        salesReportRequests = new SalesReportRequests();

        catReq = new CategoryRequests();
        catReq.create().setDtoSupplier(CategoryCreateDto::new);
        catReq.createInDepartment().setDtoSupplier(CategoryCreateDto::new);
        catReq.patch().setDtoSupplier(CategoryUpdateDto::new);

        depReq = new DepartmentRequests();
        depReq.create().setDtoSupplier(DepartmentCreateDto::new);
        depReq.patch().setDtoSupplier(DepartmentUpdateDto::new);

        unitReq = new UnitRequests();
        unitReq.create().setDtoSupplier(UnitCreateDto::new);
        unitReq.patch().setDtoSupplier(UnitUpdateDto::new);

        prodReq = new ProductRequests();
        prodReq.create().setDtoSupplier(ProductCreateDto::new);
        prodReq.patch().setDtoSupplier(ProductUpdateDto::new);
        prodReq.batchDeleteSkuSupplierProduct().setDtoSupplier(ArrayList::new);

        stockReq = new StockRequests();
        stockReq.create().setDtoSupplier(StockCreateDto::new);
        stockReq.patch().setDtoSupplier(StockUpdateDto::new);
        stockReq.createWarehouse().setDtoSupplier(WarehouseCreateDto::new);
        stockReq.editStockWarehouse().setDtoSupplier(StockUpdateDto::new);

        creditRequests = new CreditListRequests();

        priceListRequests = new PriceListRequests();

        agentClientRequests = new AgentClientRequests();
        agentClientRequests.post().setDtoSupplier(ClientCreateDto::new);
        agentClientRequests.patch().setDtoSupplier(() -> new JsonNode[0]);
        agentClientRequests.createAddress().setDtoSupplier(() -> new AddressCreateDto[]{new AddressCreateDto()});
        agentClientRequests.createAddressPredetermined().setDtoSupplier(() -> new AddressCreateDto[]{new AddressCreateDto()});

        cfdiRequests = new CfdiRequests();
        cfdiRequests.post().setDtoSupplier(DatosFacturacionIngresoCreateDto::new);

        cfdiRequests.postRep().setJsonDtoPath("json/RecepcionPago.json");
        paymentRequests = new ClientPaymentRequests();
        paymentRequests.post().setDtoSupplier(ClientPaymentDto::new);

        paymentRequests.cancel().setDtoSupplier(ClientPaymentCancellationDto::new);

        documentGraph = new DocumentQueriesRequests();
        documentGraph.list().setDtoSupplier(DocumentGraph::new);
        documentGraph.generated().setDtoSupplier(DocumentGeneratedGraph::new);

        creditListRequest = new CreditListRequests();
        creditListRequest.post().setDtoSupplier(CreditListCreateDto::new);
        creditListRequest.assignAgentCreditDataInList().setDtoSupplier(AgentCreditCreateDto::new);
        creditListRequest.patch().setDtoSupplier(() -> new ObjectNode[]{
                JsonNodeFactory.instance.objectNode()}
        );
        creditListRequest.editAgentCreditDataInList().setDtoSupplier(() -> new ObjectNode[]{
                JsonNodeFactory.instance.objectNode()}
        );

        ordersRequests = new OrdersRequests();
        ordersRequests.post().setDtoSupplier(OrderDto::new);

        settingsRequests = new SettingsRequests();

        taxRequest = new TaxRequest();
        taxRequest.patch().setDtoSupplier(TaxEditDto::new);
        taxRequest.put().setDtoSupplier(TaxCreateDto::new);

        logger.info("Inicializacion de Requests Finalizada");
    }

    @BeforeEach
    void resetToken() {
        restTemplate.withToken(RequestInitializer.getTestSettings().company1().branch1().jwt());
    }

    public static TestSettings getTestSettings() {
        return testSettings;
    }

    public static TestEnvironment getTestEnvironment() {
        return testEnvironment;
    }

    public static ServiceTestRestTemplate getRestTemplate() {
        return restTemplate;
    }

    public static SaleRequests getSaleRequests() {
        return saleRequests;
    }

    public static PurchasesRequests getPurchaseRequests() {
        return purchaseRequests;
    }

    public static QuotationsRequests getQuotationsRequests() {
        return quotationsRequests;
    }

    public static SalesReportRequests getSalesReportRequests() {
        return salesReportRequests;
    }

    public static SerieRequests getSerieRequests() {
        return serieRequests;
    }

    public static CompanyRequests getCompanyRequests() {
        return companyRequests;
    }

    public static CashRegisterRequests getCashRegisterRequests() {
        return cashRegisterRequests;
    }

    public static CashRegQueriesRequests getCashRegQueriesRequests() {
        return cashRegQueriesRequests;
    }

    public static CashRegBalancingQueriesRequests getCashRegBalancingQueriesRequests() {
        return cashRegBalancingQueriesRequests;
    }

    public static AgentClientRequests getAgentClientRequests() {
        return agentClientRequests;
    }

    public static CreditListRequests getCreditRequests() {
        return creditRequests;
    }

    public static CategoryRequests getCatReq() {
        return catReq;
    }

    public static DepartmentRequests getDepReq() {
        return depReq;
    }

    public static UnitRequests getUnitReq() {
        return unitReq;
    }

    public static ProductRequests getProdReq() {
        return prodReq;
    }

    public static StockRequests getStockReq() {
        return stockReq;
    }

    public static PriceListRequests getPriceListRequests() {
        return priceListRequests;
    }

    public static ClientPaymentRequests getClientPayRequests() {
        return clientPayRequests;
    }

    public static CfdiRequests getCfdiRequests() {
        return cfdiRequests;
    }

    public static ClientPaymentRequests getPaymentRequests() {
        return paymentRequests;
    }

    public static DocumentQueriesRequests getDocumentGraph() {
        return documentGraph;
    }

    public static SupplierPaymentRequests getSupplierPayRequest() {
        return supplierPayRequest;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static CreditListRequests getCreditListRequest() {
        return creditListRequest;
    }

    public static BranchOfficeRequests branchOfficeRequests() {
        return branchOfficeRequests;
    }

    public static OrdersRequests getOrdersRequests() {
        return ordersRequests;
    }

    public static SettingsRequests getSettingsRequests() {
        return settingsRequests;
    }

    public static TaxRequest getTaxRequest() {
        return taxRequest;
    }
}