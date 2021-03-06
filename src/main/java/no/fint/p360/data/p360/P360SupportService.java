package no.fint.p360.data.p360;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.p360.support.*;
import no.fint.p360.data.exception.CodeTableNotFound;
import no.fint.p360.data.utilities.FintUtils;
import no.fint.p360.data.utilities.P360Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class P360SupportService extends P360AbstractService {

    private static final QName SERVICE_NAME = new QName("http://software-innovation.com/SI.Data", "SupportService");

    private ISupportService supportService;
    private ObjectFactory objectFactory;

    @Value("${fint.p360.wsdl-location:./src/main/resources/wsdl}/SupportService.wsdl")
    private String wsdlLocation;

    public P360SupportService() {
        super("http://software-innovation.com/SI.Data", "SupportService");
    }

    @PostConstruct
    private void init() throws MalformedURLException {
        URL wsdlLocationUrl = P360Utils.getURL(wsdlLocation);
        log.info("WSDL location: {}", wsdlLocationUrl);
        supportService = new SupportService(wsdlLocationUrl, SERVICE_NAME).getBasicHttpBindingISupportService();
        super.setup(supportService, "SupportService");
        objectFactory = new ObjectFactory();
    }

    public GetCodeTableRowsResult getCodeTable(String table) throws CodeTableNotFound {
        GetCodeTableRowsQuery codeTableRowsQuery = objectFactory.createGetCodeTableRowsQuery();
        codeTableRowsQuery.setCodeTableName(objectFactory.createGetCodeTableRowsQueryCodeTableName(table));
        GetCodeTableRowsResult codeTableRows = supportService.getCodeTableRows(codeTableRowsQuery);
        if (codeTableRows.isSuccessful()) {
            return codeTableRows;
        }

        throw new CodeTableNotFound(String.format("Could not find %s", table));
    }

    public Stream<CodeTableRowResult> getCodeTableRowResultStream(String table) {
        GetCodeTableRowsResult codeTable = getCodeTable(table);
        return FintUtils.optionalValue(codeTable.getCodeTableRows())
                .map(ArrayOfCodeTableRowResult::getCodeTableRowResult)
                .map(List::stream)
                .orElseThrow(() -> new CodeTableNotFound(table));
    }

    public boolean ping() {

        try {
            supportService.ping();
        } catch (WebServiceException e) {
            return false;
        }

        return true;
    }

    public String getSIFVersion() {
        return supportService.getSIFVersion();
    }
}
