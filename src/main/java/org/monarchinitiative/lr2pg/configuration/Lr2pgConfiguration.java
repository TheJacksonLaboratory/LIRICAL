package org.monarchinitiative.lr2pg.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class Lr2pgConfiguration {

    public static final Logger logger = LoggerFactory.getLogger(Lr2pgConfiguration.class);

    @SuppressWarnings("FieldCanBeLocal")
    private final Environment env;

    public Lr2pgConfiguration(Environment env) {
        this.env = env;
    }
}
