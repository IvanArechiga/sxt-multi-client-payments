package setup.templates;

import com.sicarx.agent.client.dto.AgentType;
import com.sicarx.document.dto.orders.OrderDto;
import com.sicarx.document.dto.purchases.PurchaseDto;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.infra.dto.AgentDto;
import com.sicarx.test.core.TestOperations;
import sxb.tests.utils.JsonFileCache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SalesTemplates {

    public static Map<String, Object> refundBody(){
        Map refund = new HashMap();
        refund.put("paymentId", null);
        refund.put("amount", null);
        refund.put("coment", null);
        refund.put("isoCurrency", null);
        refund.put("exchangeRate", null);
        refund.put("convertedAmount", null);
        return refund;
    }

    public static OrderDto getSaleOrderDtoDefault() {
        OrderDto orderDto = JsonFileCache.toObject("json/simpleOrderMinimumFields.json", OrderDto.class);

        orderDto.setUuid(UUID.randomUUID().toString());
        return orderDto;
    }

    public static SaleDto getSaleDeliveryDtoDefault() {
        SaleDto saleDto = JsonFileCache.toObject("json/simpleDeliveryMinimumFields.json", SaleDto.class);

        saleDto.setUuid(UUID.randomUUID().toString());
        return saleDto;
    }

    public static SaleDto getSaleDtoDefault() {
        SaleDto saleDto = JsonFileCache.toObject("json/simpleSaleMinimumFields.json", SaleDto.class);

        saleDto.setUuid(UUID.randomUUID().toString());
        return saleDto;
    }

    public static PurchaseDto getPurchaseDtoDefault() {
        PurchaseDto purchaseDto = JsonFileCache.toObject("json/simplePurchaseMinimumFields.json", PurchaseDto.class);

        AgentDto supplier = TestOperations.createDocumentAgent("supplier", new BigDecimal("999999999.99"), AgentType.SUPPLIER);

        purchaseDto.setUuid(UUID.randomUUID().toString());
        purchaseDto.setAgent(supplier);
        return purchaseDto;
    }
}
