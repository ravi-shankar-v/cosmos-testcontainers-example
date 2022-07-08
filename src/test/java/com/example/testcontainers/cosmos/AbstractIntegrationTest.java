package com.example.testcontainers.cosmos;

import com.azure.cosmos.CosmosClientBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
@Testcontainers
@ActiveProfiles({"integration-test"})
class AbstractIntegrationTest {

    protected static final int ServerPort = 8090;
    @Container
    private static final CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
            DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
    );
    @TempDir
    private static Path tempFolder;

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            Path keyStoreFile = tempFolder.resolve("azure-cosmos-emulator.keystore");
            KeyStore keyStore = emulator.buildNewKeyStore();
            keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());

            System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", emulator.getEmulatorKey());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

            TestPropertyValues values = TestPropertyValues.of("azure.cosmosdb.uri=" + emulator.getEmulatorEndpoint(),
                    "azure.cosmosdb.key=" + emulator.getEmulatorKey(),
                    "server.port=" + ServerPort);

            values.applyTo(context);
        }
    }

    @TestConfiguration
    static class DemoConfiguration {

        @Value("${azure.cosmosdb.uri}")
        private String testContainerURI;

        @Value("${azure.cosmosdb.key}")
        private String testContainerKey;

        @Bean
        public CosmosClientBuilder cosmosClient() {
            return new CosmosClientBuilder()
                    .endpoint(testContainerURI)
                    .key(testContainerKey)
                    .gatewayMode()
                    .endpointDiscoveryEnabled(false);
        }
    }

}