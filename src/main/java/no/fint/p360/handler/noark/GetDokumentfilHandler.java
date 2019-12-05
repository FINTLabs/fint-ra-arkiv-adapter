package no.fint.p360.handler.noark;

import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.administrasjon.arkiv.ArkivActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.administrasjon.arkiv.DokumentfilResource;
import no.fint.p360.data.exception.FileNotFound;
import no.fint.p360.handler.Handler;
import no.fint.p360.repository.InternalFileRepository;
import no.fint.p360.service.CachedFileService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Service
public class GetDokumentfilHandler implements Handler {
    @Autowired
    private CachedFileService cachedFileService;

    @Autowired
    private InternalFileRepository internalFileRepository;

    @Override
    public void accept(Event<FintLinks> response) {
        try {
            if (!StringUtils.startsWithIgnoreCase(response.getQuery(), "systemid/")) {
                response.setResponseStatus(ResponseStatus.REJECTED);
                response.setStatusCode("INVALID_QUERY");
                response.setMessage("Invalid query: " + response.getQuery());
                return;
            }
            String systemId = StringUtils.removeStartIgnoreCase(response.getQuery(), "systemid/");
            if (StringUtils.startsWith(systemId, "I_")) {
                DokumentfilResource dokumentfilResource = internalFileRepository.getFile(systemId);
                response.addData(dokumentfilResource);
                response.setResponseStatus(ResponseStatus.ACCEPTED);
            } else {
                DokumentfilResource dokumentfilResource = cachedFileService.getFile(systemId);
                response.addData(dokumentfilResource);
                response.setResponseStatus(ResponseStatus.ACCEPTED);
            }
        } catch (FileNotFound | IOException e) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("NOT_FOUND");
            response.setMessage(e.getMessage());
        }

    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(ArkivActions.GET_DOKUMENTFIL.name());
    }

    @Override
    public boolean health() {
        return cachedFileService.health();
    }
}