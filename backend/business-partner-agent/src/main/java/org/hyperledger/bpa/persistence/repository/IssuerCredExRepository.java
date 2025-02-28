/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.StateChangeDecorator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface IssuerCredExRepository extends CrudRepository<BPACredentialExchange, UUID> {

    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Iterable<BPACredentialExchange> findAll();

    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findById(@NonNull UUID id);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findByCredentialExchangeId(@NonNull String credentialExchangeId);

    int countIdByCredDefId(@NonNull UUID credDefId);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    List<BPACredentialExchange> listOrderByUpdatedAtDesc();

    Number updateCredential(@Id UUID id, Credential indyCredential);

    Number updateCredential(@Id UUID id, BPACredentialExchange.ExchangePayload ldCredential);

    Number updateAfterEventWithRevocationInfo(@Id UUID id,
            CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            @Nullable String revRegId,
            @Nullable String credRevId,
            @Nullable String errorMsg);

    Number updateAfterEventNoRevocationInfo(@Id UUID id,
            CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            @Nullable String errorMsg);

    Number updateRevocationInfo(@Id UUID id, String revRegId, @Nullable String credRevId);

    Number updateReferent(@Id UUID id, String referent);

    Number updateByCredentialExchangeId(String credentialExchangeId, String referent);
}
