package com.ebikes.iam.support.infrastructure;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class MockKeycloakConfig {

  @Bean
  @Primary
  public Keycloak keycloakAdminClient() {
    UsersResource usersResource = Mockito.mock(UsersResource.class);
    Mockito.when(usersResource.searchByEmail(Mockito.anyString(), Mockito.eq(true)))
        .thenReturn(List.of(new UserRepresentation()));

    RealmResource realmResource = Mockito.mock(RealmResource.class);
    Mockito.when(realmResource.users()).thenReturn(usersResource);

    Keycloak keycloak = Mockito.mock(Keycloak.class);
    Mockito.when(keycloak.realm(Mockito.anyString())).thenReturn(realmResource);

    return keycloak;
  }
}
