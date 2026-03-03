package setup.templates;

import com.sicarx.test.dto.CurrencyCreateDto;

import java.math.BigDecimal;

public class SettingsTemplates {

    public static CurrencyCreateDto createIsoCurrency(){
        return new CurrencyCreateDto(
                "USD",
                new BigDecimal(0.050),
                "string",
                "https://storage.googleapis.com/sxb-images/catalogue/currency/USD",
                false,
                false,
                false
        );
    }
}
