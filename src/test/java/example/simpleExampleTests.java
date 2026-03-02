package example;

import com.sicarx.cashregister.infra.dto.CashRegBalanceReadDto;
import com.sicarx.cashregister.subdomain.trx.PaymentType;
import com.sicarx.cashregister.subdomain.trx.TransactionType;
import com.sicarx.document.domain.DocumentType;
import com.sicarx.document.domain.OpMode;
import com.sicarx.document.dto.sales.SaleDto;
import com.sicarx.document.infra.dto.PaymentDto;
import com.sicarx.document.infra.dto.RefundDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import setup.RequestInitializer;
import sxb.tests.junit.ResponseDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class simpleExampleTests extends RequestInitializer {
    @Test
    void test1() {
        getCashRegisterRequests().getList().assertReq(OK);
    }

    @Test
    void test2() {
        getCashRegisterRequests().postTransaction().assertReq(HttpStatus.CREATED, c -> {
            c.setAmount(new BigDecimal("123123"));
            c.setUuid(UUID.randomUUID().toString());
            c.setPaymentId(PaymentType.CASH);
            c.setType(TransactionType.INCOME);
            c.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
            c.setComment("Comment");
            c.setIsoCurrency("USD");
            c.setExchangeRate(new BigDecimal("19.276483"));
        });

        getCashRegisterRequests().postTransaction().assertReq(HttpStatus.BAD_REQUEST, c -> {
            c.setAmount(new BigDecimal("123123"));
            c.setUuid("uuid");
            c.setPaymentId(PaymentType.CASH);
            c.setType(TransactionType.INCOME);
            c.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
            c.setComment("Comment");
            c.setIsoCurrency("USD");
            c.setExchangeRate(new BigDecimal("19.276483"));
        });
    }

    @Test
    void test3() {
        getCashRegQueriesRequests().balance().assertReq(OK, getTestSettings().company1().branch1().cashReg1());
    }

    @Test
    void test4() {
        ResponseEntity<CashRegBalanceReadDto[]> response = getCashRegQueriesRequests().balanceList().assertReq(OK);

        Assertions.assertTrue(Arrays.stream(response.getBody()).anyMatch(c -> c.getName().equalsIgnoreCase("CashReg 1")));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/refundCases.csv", useHeadersInDisplayName = true)
    void refund_cancellation_cases(BigDecimal amount, BigDecimal exchangeRate, BigDecimal convertedAmount) throws IOException {
        ResponseDto<SaleDto, String> sale = getSaleRequests().post().assertReq(CREATED, c -> {
            c.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
            c.setTimeZone(TimeZone.getDefault().getID());
            c.setIsoCurrency("MXN");
            c.setUuid(UUID.randomUUID().toString());
            c.setSerie("");
            c.setDecimals(6);
            c.setOpMode(OpMode.MX);
            c.setType(DocumentType.SALE);
            c.setTotal(convertedAmount);
            c.setPayments(List.of(new PaymentDto(PaymentType.CASH, convertedAmount)));
        });

        getSaleRequests().postCancel().assertReq(OK, c -> {
            c.setUuid(sale.originDto().getUuid());
            c.setCashRegisterUuid(getTestSettings().company1().branch1().cashReg1());
            c.setRefund(new RefundDto(
                    PaymentType.CASH, amount, "USD", exchangeRate, convertedAmount));
        });
    }
}
