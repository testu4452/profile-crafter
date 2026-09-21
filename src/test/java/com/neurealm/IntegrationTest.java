package com.neurealm;

import com.neurealm.config.AsyncSyncConfiguration;
import com.neurealm.config.EmbeddedSQL;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { ProfileCrafterApp.class, AsyncSyncConfiguration.class, com.neurealm.config.JacksonHibernateConfiguration.class }
)
@EmbeddedSQL
public @interface IntegrationTest {}
