package cz.vse.miraql.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Slf4j
@Component
@Getter
public class MiraqlConfig {

    private final String ontologyApiEndpoint;

    @Autowired
    public MiraqlConfig(@Value("${ontology-api.integration.endpoint}") @Nonnull String ontologyApiEndpoint) {
        this.ontologyApiEndpoint = ontologyApiEndpoint;
        log.info("Setting ontology api endpoint: {}", this.ontologyApiEndpoint);
    }

}
