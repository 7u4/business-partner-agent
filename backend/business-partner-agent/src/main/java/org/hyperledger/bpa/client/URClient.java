/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.micronaut.cache.annotation.Cacheable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hyperledger.acy_py.generated.model.DIDEndpointWithType;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.ledger.EndpointResponse;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.impl.util.AriesStringUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Universal Resolver Client
 *
 */
@Slf4j
@Singleton
public class URClient {

    @Inject
    AriesClient ac;

    private final Gson gson = GsonConfig.defaultConfig();

    private final OkHttpClient okClient = new OkHttpClient();

    @Cacheable(cacheNames = { "ur-cache" })
    public Optional<DIDDocument> getDidDocument(@NonNull String did) {
        try {
            if (did.startsWith("did:sov:")) {
                Optional<DIDDocument> didDocument = ac.resolverResolveDid(did);
                if (didDocument.isPresent() && !didDocument.get().hasProfileEndpoint()) {
                    ac.ledgerDidEndpoint(did, DIDEndpointWithType.EndpointTypeEnum.PROFILE)
                            .map(EndpointResponse::getEndpoint)
                            .ifPresent(ep -> {
                                String profile = DIDEndpointWithType.EndpointTypeEnum.PROFILE
                                        .getValue().toLowerCase(Locale.US);
                                didDocument.get().getService().add(DIDDocument.Service
                                        .builder()
                                        .id(didDocument.get().getId() + "#" + profile)
                                        .type(profile)
                                        .serviceEndpoint(ep)
                                        .build());
                            });
                }
                return didDocument;
            } else if (did.startsWith("did:web:")) {
                String host = AriesStringUtil.getLastSegment(did);
                return call("https://" + host + "/.well-known/did.json", DIDDocument.class);
            }
        } catch (IOException e) {
            log.error("aca-py not reachable");
        }
        return Optional.empty();
    }

    public Optional<VerifiablePresentation<VerifiableIndyCredential>> getPublicProfile(String url) {
        return call(url, new TypeToken<VerifiablePresentation<VerifiableIndyCredential>>(){}.getType());
    }

    public <T> Optional<T> call(String url, Type type) {
        Optional<T> result = Optional.empty();
        try {
            URL url2 = new URL(url);
            Request request = new Request.Builder()
                    .url(url2.toString())
                    .build();
            try (Response response = okClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = Objects.requireNonNull(response.body()).string();
                    T md = gson.fromJson(body, type);
                    result = Optional.of(md);
                } else {
                    log.warn("Could not resolve public profile: {}, {}", response.code(), response.message());
                }
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed endpoint URL: " + url;
            log.error(msg, e);
            throw new PartnerException(msg);
        } catch (IOException e) {
            String msg = "Call to partner web endpoint failed - msg: " + e.getMessage();
            log.error(msg, e);
            throw new PartnerException(msg);
        }
        return result;
    }
}
