package setup.templates;

import com.sicarx.cashregister.infra.dto.CashRegClosingDto;
import com.sicarx.cashregister.infra.dto.CashRegTransactionDto;
import com.sicarx.cashregister.infra.dto.CashRegisterCreateDto;
import com.sicarx.cashregister.subdomain.trx.PaymentType;
import com.sicarx.cashregister.subdomain.trx.TransactionType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class CashRegisterTemplates {

    public static CashRegTransactionDto cashTransaction(){
        CashRegTransactionDto transaction = new CashRegTransactionDto();
        transaction.setCashRegisterUuid("uuid");
        transaction.setAmount(new BigDecimal(1000));
        transaction.setPaymentId(PaymentType.CASH);
        transaction.setType(TransactionType.INCOME);
        transaction.setComment("Comentario de Prueba");
        return transaction;
    }

    public static CashRegisterCreateDto cashRegisterCreate(){
        CashRegisterCreateDto cashRegister = new CashRegisterCreateDto();
        cashRegister.setName("CajaPruebas");
        cashRegister.setDescription("Caja Exclusivamente para Pruebas Especificas");
        cashRegister.setUuid(UUID.randomUUID().toString());
        return cashRegister;
    }

    public static CashRegClosingDto cashBalancing(){
        CashRegClosingDto close = new CashRegClosingDto();
        close.setCounted(new HashMap<>());
        close.setLockUuid("lockuuid");
        close.setCashRegisterUuid("uuid");
        close.setComment("Comentario de Prueba para corte de caja");
        return close;
    }
}
