package no.fint.p360.data.kulturminne;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.p360.caze.CaseResult;
import no.fint.arkiv.p360.caze.CreateCaseParameter;
import no.fint.arkiv.p360.caze.ObjectFactory;
import no.fint.arkiv.p360.document.CreateDocumentParameter;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.administrasjon.arkiv.JournalpostResource;
import no.fint.model.resource.kultur.kulturminnevern.TilskuddFredaHusPrivatEieResource;
import no.fint.p360.data.exception.GetDocumentException;
import no.fint.p360.data.exception.IllegalCaseNumberFormat;
import no.fint.p360.data.noark.common.NoarkFactory;
import no.fint.p360.data.noark.journalpost.JournalpostFactory;
import no.fint.p360.data.utilities.P360Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TilskuddFripFactory {

    @Autowired
    private NoarkFactory noarkFactory;

    @Autowired
    private JournalpostFactory journalpostFactory;

    @Autowired
    private CaseDefaultsService caseDefaultsService;

    private ObjectFactory objectFactory;


    @PostConstruct
    private void init() {
        objectFactory = new ObjectFactory();
    }

    public TilskuddFredaHusPrivatEieResource toFintResource(CaseResult caseResult) throws GetDocumentException, IllegalCaseNumberFormat {

        TilskuddFredaHusPrivatEieResource tilskuddFredaHusPrivatEie = new TilskuddFredaHusPrivatEieResource();
        tilskuddFredaHusPrivatEie.setSoknadsnummer(new Identifikator());
        tilskuddFredaHusPrivatEie.setMappeId(new Identifikator());
        tilskuddFredaHusPrivatEie.setSystemId(new Identifikator());

        noarkFactory.getSaksmappe(caseResult, tilskuddFredaHusPrivatEie);

        return tilskuddFredaHusPrivatEie;
    }


    public List<TilskuddFredaHusPrivatEieResource> toFintResourceList(List<CaseResult> caseResults) throws GetDocumentException, IllegalCaseNumberFormat {
        List<TilskuddFredaHusPrivatEieResource> result = new ArrayList<>(caseResults.size());
        for (CaseResult caseResult : caseResults) {
            result.add(toFintResource(caseResult));
        }
        return result;
    }

    public CreateCaseParameter convertToCreateCase(TilskuddFredaHusPrivatEieResource tilskuddFredaHusPrivatEie) {
        CreateCaseParameter createCaseParameter = objectFactory.createCreateCaseParameter();

        caseDefaultsService.applyDefaultsToCreateCase("tilskudd-freda-hus-privat-eie", createCaseParameter);

        createCaseParameter.setExternalId(P360Utils.getExternalIdParameter(tilskuddFredaHusPrivatEie.getSoknadsnummer()));

        noarkFactory.applyCaseParameters(tilskuddFredaHusPrivatEie, createCaseParameter);

        return createCaseParameter;
    }


    public CreateDocumentParameter convertToCreateDocument(JournalpostResource journalpostResource, String caseNumber) {
        CreateDocumentParameter createDocumentParameter = journalpostFactory.toP360(journalpostResource, caseNumber);
        return createDocumentParameter;
    }
}