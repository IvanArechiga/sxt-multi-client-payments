package multiPayments.R01;

import com.sicarx.auth.registry.Client;
import com.sicarx.auth.registry.Queries;
import com.sicarx.settings.client.dto.DocumentType;
import com.sicarx.test.core.TestOperations;
import com.sicarx.test.dto.SerieDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import setup.RequestInitializer;
import sxb.tests.utils.AssertionsEnhanced;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

class TC2PermissionTests extends RequestInitializer {

    @BeforeAll
    static void setup() {
        SerieDto[] serieList = getSerieRequests().serieList().assertReq(OK, DocumentType.MULTI_CLIENT_PAYMENT.getType()).getBody();

        if (serieList != null) {
            Arrays.stream(serieList).filter(c -> c.getSerie().equals("A")).findFirst().ifPresent(s -> {
                getSerieRequests().delete().assertReq(OK, s.getDocumentType(), s.getSerie());
            });
        }
    }

    @Test
    @DisplayName("2.1.1 - Creación de Serie")
    void testCreateAutomaticSeries() {
        TestOperations.setCustomRoleActions(getTestSettings().company1(), List.of(Client.CREDIT_PAYMENT));

        getSerieRequests().post().assertReq(HttpStatus.CREATED, c -> {
            c.setSerie("A");
            c.setDocumentType(DocumentType.MULTI_CLIENT_PAYMENT.getType());
            c.setFolio(1L);
        });

        getRestTemplate().withToken(getTestSettings().company1().branch1().jwt());

        SerieDto[] serieList = getSerieRequests().serieList().assertReq(OK, DocumentType.MULTI_CLIENT_PAYMENT.getType()).getBody();

        assert serieList != null;
        Arrays.stream(serieList).filter(d -> d.getSerie().equals("A")).findFirst().ifPresentOrElse(
                serie -> {
                    AssertionsEnhanced.assertEquals("A", serie.getSerie(), "La serie A no se creó correctamente");
                    AssertionsEnhanced.assertEquals(DocumentType.MULTI_CLIENT_PAYMENT.getType(), serie.getDocumentType(), "El tipo de documento de la serie A es incorrecto");
                    AssertionsEnhanced.assertEquals(1L, serie.getFolio(), "El folio de la serie A es incorrecto");
                    getSerieRequests().delete().assertReq(OK, serie.getDocumentType(), serie.getSerie());
                },
                () -> AssertionsEnhanced.fail("La serie A no Existe")
        );

    }

    @Test
    @DisplayName("2.1.2- Creación de Serie Default")
    void testCreateAutomaticDefaultSeries() {
        TestOperations.setCustomRoleActions(getTestSettings().company3(), List.of(Queries.EDIT_SALE_SELLER));
        getRestTemplate().withToken(getTestSettings().company3().branch1().jwt());

        getSerieRequests().post().assertReq(HttpStatus.CREATED, c -> {
            c.setSerie("");
            c.setDocumentType(DocumentType.MULTI_CLIENT_PAYMENT.getType());
            c.setFolio(1L);
        });


        SerieDto[] serieList = getSerieRequests().serieList().assertReq(OK, DocumentType.MULTI_CLIENT_PAYMENT.getType()).getBody();

        assert serieList != null;
        Arrays.stream(serieList).filter(d -> d.getSerie().equals("")).findFirst().ifPresentOrElse(
                serie -> {
                    AssertionsEnhanced.assertEquals("", serie.getSerie(), "La serie A no se creó correctamente");
                    AssertionsEnhanced.assertEquals(DocumentType.MULTI_CLIENT_PAYMENT.getType(), serie.getDocumentType(), "El tipo de documento de la serie A es incorrecto");
                    AssertionsEnhanced.assertEquals(1L, serie.getFolio(), "El folio de la serie A es incorrecto");
                    getSerieRequests().delete().assertReq(OK, serie.getDocumentType(), serie.getSerie());
                },
                () -> AssertionsEnhanced.fail("La serie A no Existe")
        );

    }
}
