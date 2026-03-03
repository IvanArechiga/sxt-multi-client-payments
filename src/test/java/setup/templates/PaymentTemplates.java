package setup.templates;

import com.sicarx.document.dto.multiclientpayment.MultiClientPaymentDto;

import static com.sicarx.document.domain.DocumentType.MULTI_CLIENT_PAYMENT;

public class PaymentTemplates {

    public static MultiClientPaymentDto getMultiPaymentDtoDefault() {
        MultiClientPaymentDto multiPayment = new MultiClientPaymentDto();
        multiPayment.setUuid(java.util.UUID.randomUUID().toString());
        multiPayment.setTimeZone("America/Mexico_City");
        multiPayment.setDecimals(6);
        multiPayment.setType(MULTI_CLIENT_PAYMENT);
        multiPayment.setSerie("");
        multiPayment.setIsoCurrency("MXN");

        return multiPayment;
    }
}
