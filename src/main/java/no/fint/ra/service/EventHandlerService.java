package no.fint.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.event.EventResponseService;
import no.fint.adapter.event.EventStatusService;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import no.fint.model.kultur.kulturminnevern.KulturminnevernActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.kultur.kulturminnevern.TilskuddFartoyResource;
import no.fint.ra.data.P360CaseFactory;
import no.fint.ra.data.exception.CreateTilskuddFartoyException;
import no.fint.ra.data.exception.GetTilskuddFartoyException;
import no.fint.ra.data.exception.GetTilskuddFartoyNotFoundException;
import no.fint.ra.data.service.P360CaseServiceP360;
import no.fint.ra.data.service.P360DocumentService;
import no.fint.ra.data.service.P360FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;


@Slf4j
@Service
public class EventHandlerService {

    @Autowired
    private EventResponseService eventResponseService;

    @Autowired
    private EventStatusService eventStatusService;

    @Autowired
    private P360DocumentService p360DocumentService;

    @Autowired
    private P360FileService p360FileService;

    @Autowired
    private P360CaseServiceP360 p360CaseServiceP360;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private P360CaseFactory caseFactory;

    public void handleEvent(Event event) {
        if (event.isHealthCheck()) {
            postHealthCheckResponse(event);
        } else {
            if (eventStatusService.verifyEvent(event).getStatus() == Status.ADAPTER_ACCEPTED) {
                log.info("{}", event.getData());
                Event<FintLinks> responseEvent = new Event<>(event);
                try {

                    createEventResponse(event, responseEvent);

                } catch (Exception e) {
                    log.error("Error handling event {}", event, e);
                    responseEvent.setResponseStatus(ResponseStatus.ERROR);
                    responseEvent.setMessage(e.getMessage());
                } finally {
                    //responseEvent.setStatus(Status.ADAPTER_RESPONSE);
                    //eventResponseService.postResponse(responseEvent);
                }
            }
        }
    }


    private void createEventResponse(Event event, Event<FintLinks> response) {

        if (KulturminnevernActions.getActions().contains(event.getAction())) {
            switch (KulturminnevernActions.valueOf(event.getAction())) {
                case UPDATE_TILSKUDDFARTOY:
                    onCreateTilskuddFartoy(response);
                    break;

                case GET_TILSKUDDFARTOY:
                    onGetTilskuddFartoy(event, response);
                    break;
            }
        }
        /*
        if (ArkivActions.getActions().contains(event.getAction())) {
            String query = event.getQuery();
            JournalpostResource journalPost = p360DocumentService.getJournalPost(query.replaceFirst("mappeid/", ""));
            response.setData(Collections.singletonList(journalPost));
            response.setStatus(Status.TEMP_UPSTREAM_QUEUE);
            response.setResponseStatus(ResponseStatus.ACCEPTED);
            eventResponseService.postResponse(response);

        }
         */
    }

    private void onGetTilskuddFartoy(Event event, Event<FintLinks> response) {
        String query = event.getQuery();

        TilskuddFartoyResource tilskuddFartoyResource = null;
        try {
            if (query.startsWith("mappeid")) {
                response.setData(
                        Collections.singletonList(
                                p360CaseServiceP360.getTilskuddFartoyCaseByCaseNumber(query.replaceFirst("mappeid/", ""))
                        )
                );
            }
            if (query.startsWith("systemid")) {
                response.setData(
                        Collections.singletonList(
                                p360CaseServiceP360.getTilskuddFartoyCaseBySystemId(query.replaceFirst("systemid/", ""))
                        )
                );
            }
            if (query.startsWith("?")) {
                List<TilskuddFartoyResource> tilskuddFartoyResources = p360CaseServiceP360.searchTilskuddFartoyCaseByTitle(query);
                tilskuddFartoyResources.forEach(response::addData);
            }
            response.setStatus(Status.TEMP_UPSTREAM_QUEUE);
            response.setResponseStatus(ResponseStatus.ACCEPTED);
            //response.setData(Collections.singletonList(tilskuddFartoyResource));
        } catch (GetTilskuddFartoyNotFoundException e) {
            response.setMessage(e.getMessage());
            response.setData(Collections.emptyList());
        } catch (GetTilskuddFartoyException e) {
            response.setResponseStatus(ResponseStatus.ERROR);
            response.setMessage(String.format("Error from application: %s", e.getMessage()));
        } catch (Exception e) {
            response.setResponseStatus(ResponseStatus.ERROR);
            response.setMessage(String.format("Error from adapter: %s", e.getMessage()));
        }

        eventResponseService.postResponse(response);

    }

    private void onCreateTilskuddFartoy(Event<FintLinks> response) {

        if (response.getData().size() == 1) {

            TilskuddFartoyResource tilskuddFartoyResource = objectMapper.convertValue(response.getData().get(0), TilskuddFartoyResource.class);

            try {
                TilskuddFartoyResource tilskuddFartoy = p360CaseServiceP360.createTilskuddFartoyCase(tilskuddFartoyResource);
                response.setResponseStatus(ResponseStatus.ACCEPTED);
                response.setData(Collections.singletonList(tilskuddFartoy));
            } catch (CreateTilskuddFartoyException e) {
                response.setResponseStatus(ResponseStatus.ERROR);
                response.setMessage(e.getMessage());

            }
        }
        eventResponseService.postResponse(response);

    }


    public void postHealthCheckResponse(Event event) {
        Event<Health> healthCheckEvent = new Event<>(event);
        healthCheckEvent.setStatus(Status.TEMP_UPSTREAM_QUEUE);

        if (healthCheck()) {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_HEALTHY.name()));
        } else {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_UNHEALTHY));
            healthCheckEvent.setMessage("The adapter is unable to communicate with the application.");
        }

        eventResponseService.postResponse(healthCheckEvent);
    }


    private boolean healthCheck() {

        return p360CaseServiceP360.ping() && p360DocumentService.ping() && p360FileService.ping();

    }


    @PostConstruct
    void init() {

    }
}
