package de.rwth.imi.flare.server.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.mapping.expansion.ExpansionTreeNode;
import de.rwth.imi.flare.mapping.expansion.QueryExpander;
import de.rwth.imi.flare.mapping.lookup.NaiveLookupMapping;
import de.rwth.imi.flare.mapping.lookup.SourceMappingEntry;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.springframework.lang.Nullable;

@Configuration
public class FlareAlgorithmConfiguration {

    @Bean
    public Map<TerminologyCode, SourceMappingEntry> loadMappingFile(@Value("${app.mappingsFile}") String mappingsFile)
            throws IOException {

        var lookupTable = new HashMap<TerminologyCode, SourceMappingEntry>();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<SourceMappingEntry> sourceMappingEntries = objectMapper.readValue(new File(mappingsFile), new TypeReference<>() {
        });
        sourceMappingEntries.forEach(sourceMappingEntry -> lookupTable.put(sourceMappingEntry.getKey(), sourceMappingEntry));
        return lookupTable;
    }


    @Bean
    public ExpansionTreeNode loadExpansionTree(@Value("${app.conceptTreeFile}") String conceptTreeFile)
            throws IOException {

        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(new File(conceptTreeFile), new TypeReference<>() {
        });
    }

    @Bean
    public QueryExpander expander(ExpansionTreeNode loadExpansionTree) throws IOException {
        return new QueryExpander(loadExpansionTree);
    }

    @Bean
    public FhirResourceMapper mapper(Map<TerminologyCode, SourceMappingEntry> loadMFile, QueryExpander expander) throws IOException {
        return new NaiveLookupMapping(loadMFile, expander);
    }

    @Bean
    public Authenticator createAuthenticator(
            @Value("${flare.fhir.user}") String userName,
            @Value("${flare.fhir.password}") String password) {
        if (!userName.equals("") && !password.equals("")) {
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            userName,
                            password.toCharArray());
                }
            };
            return auth;
        }
        return null;
    }

    @Bean
    public Executor executor(@Nullable Authenticator auth,
                             @Value("${flare.fhir.server}") String fhirBaseUri, @Value("${flare.fhir.pagecount}") String fhirSearchPageCount,
                             @Value("${flare.exec.corePoolSize}") int corePoolSize, @Value("${flare.exec.maxPoolSize}") int maxPoolSize,
                             @Value("${flare.exec.keepAliveTimeSeconds}") int keepAliveTimeSeconds,
                             @Value("${flare.cache.cleanCycleMinutes}") int cleanCycleMinutes,
                             @Value("${flare.cache.cacheEntryLifetimeHours}") int cacheEntryLifetimeHours,
                             @Value("${flare.cache.cacheSizeThousandsOfEntries}") int cacheSizeThousandsOfEntries,
                             @Value("${flare.cache.cacheEntryExpirationUpdatedAtAccess}") boolean cacheEntryExpirationUpdatedAtAccess,
                             @Value("${flare.cache.cacheCompleteDeleteOnClean}") boolean cacheCompleteDeleteOnClean) {
        return new FlareExecutor(new FhirRequestorConfig() {
            @Override
            public Optional<Authenticator> getAuthentication() {
                return Optional.ofNullable(auth);
            }

            @Override
            public URI getBaseURI() {
                URI uri = null;
                try {
                    uri = new URI(fhirBaseUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return uri;
            }

            @Override
            public String getPageCount() {
                return fhirSearchPageCount;
            }

            @Override
            public FlareThreadPoolConfig getThreadPoolConfig() {
                return new FlareThreadPoolConfig(corePoolSize, maxPoolSize, keepAliveTimeSeconds);
            }
        }, new CacheConfig() {
            @Override
            public int getCleanCycleMS() {
                return cleanCycleMinutes * 60 * 1000;
            }

            @Override
            public int getEntryLifetimeMS() {
                return cacheEntryLifetimeHours * 60 * 60 * 1000;
            }

            @Override
            public int getMaxCacheEntries() {
                return cacheSizeThousandsOfEntries * 1000;
            }

            @Override
            public boolean getUpdateExpiryAtAccess() {
                return cacheEntryExpirationUpdatedAtAccess;
            }

            @Override
            public boolean getDeleteAllEntriesOnCleanup() {
                return cacheCompleteDeleteOnClean;
            }
        });
    }


}
