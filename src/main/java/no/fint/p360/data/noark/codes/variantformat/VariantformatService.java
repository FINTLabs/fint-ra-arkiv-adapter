package no.fint.p360.data.noark.codes.variantformat;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.administrasjon.arkiv.VariantformatResource;
import no.fint.p360.data.p360.P360SupportService;
import no.fint.p360.data.utilities.BegrepMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Slf4j
@Service
public class VariantformatService {

    @Autowired
    private P360SupportService supportService;

    @Value("${fint.p360.tables.version-format:Attribute value: File - ToVersionFormat}")
    private String versionFormatTable;

    public Stream<VariantformatResource> getVersionFormatTable() {
        return supportService.getCodeTableRowResultStream(versionFormatTable)
                .map(BegrepMapper.mapValue(VariantformatResource::new));
    }

}
