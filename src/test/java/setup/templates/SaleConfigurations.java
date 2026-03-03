package setup.templates;

import com.sicarx.settings.client.dto.SalesGeneralConfDto;

public class SaleConfigurations {

    public static SalesGeneralConfDto salesConfigurations() {
        SalesGeneralConfDto saleConf = new SalesGeneralConfDto();
        saleConf.setAllowBulkSalesByProducts(true);
        saleConf.setAskForSellerAtEndSale(false);
        saleConf.setAddProductsStartOfList(true);
        saleConf.setAllowCashChangeAnyPaymentOpt(true);
        saleConf.setAskForProductQuantity(false);
        saleConf.setBreakdownTaxes(false);
        saleConf.setBreakdownTaxesProductCatalogue(false);
        saleConf.setGroupProductsBySku(false);
        saleConf.setShowDialogAutoWeight(true);
        saleConf.setShowProductPricesWithoutTaxes(false);
        saleConf.setSuggestChangeInMainCurrency(true);
        return saleConf;
    }
}
