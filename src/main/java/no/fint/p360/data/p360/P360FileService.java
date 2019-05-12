package no.fint.p360.data.p360;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.p360.file.*;
import no.fint.p360.AdapterProps;
import no.fint.p360.data.exception.FileNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;

@Slf4j
@Service
public class P360FileService extends P360AbstractService {


    private static final QName SERVICE_NAME = new QName("http://software-innovation.com/SI.Data", "FileService");

    private IFileService fileServicePort;
    private ObjectFactory objectFactory;

    @Autowired
    private AdapterProps props;

    public P360FileService() {
        super("http://software-innovation.com/SI.Data", "FileService");
    }

    @PostConstruct
    private void init() {

        fileServicePort = new FileService(FileService.WSDL_LOCATION, SERVICE_NAME).getBasicHttpBindingIFileService();
        objectFactory = new ObjectFactory();

    }

    public FileResult getFileByRecNo(String recNo) {
        log.info("Retrieving {} ...", recNo);
        GetFileWithMetadataQuery getFileWithMetadataQuery = objectFactory.createGetFileWithMetadataQuery();
        getFileWithMetadataQuery.setRecno(objectFactory.createGetFileWithMetadataQueryRecno(Integer.parseInt(recNo)));
        getFileWithMetadataQuery.setIncludeFileData(objectFactory.createGetFileWithMetadataQueryIncludeFileData(true));
        getFileWithMetadataQuery.setADContextUser(objectFactory.createFileParameterBaseADContextUser(props.getP360User()));
        GetFileWithMetadataResult fileWithMetadata = fileServicePort.getFileWithMetadata((getFileWithMetadataQuery));

        if (fileWithMetadata.isSuccessful()) {
            log.info("Retrieving {} successfully", recNo);
            return fileWithMetadata.getFile().getValue();
        }

        log.info("Retrieving {} failed: {}", recNo, fileWithMetadata.getErrorDetails().getValue());
        throw new FileNotFound(fileWithMetadata.getErrorMessage().getValue());
    }


    public boolean ping() {

        try {
            fileServicePort.ping();
        } catch (WebServiceException e) {
            return false;
        }

        return true;
    }
}