package com.marekhudyma.dbLockingPostgresql.utils;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
public abstract class AbstractIntegrationTest {

    private static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:10.4")
            .withUsername("marek")
            .withPassword("password")
            .withDatabaseName("experimentDB");
    private static boolean started = false;
    @Autowired
    protected ConfigurableApplicationContext configurableApplicationContext;

    @BeforeAll
    static void abstractBeforeAll() {
        // make sure that containers will be started only once before all tests.
        if (!started) {
            log.info("DockerContainers start");
            postgreSQLContainer.start();

            started = true;
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(new ImmutableList.Builder<String>()
                    .add("db_url=" + postgreSQLContainer.getJdbcUrl())
                    .build()).applyTo(configurableApplicationContext);
            ConfigurationPropertySources.attach(configurableApplicationContext.getEnvironment());
        }
    }
}
