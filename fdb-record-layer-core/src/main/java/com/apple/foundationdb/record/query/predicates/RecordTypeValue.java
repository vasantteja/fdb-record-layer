/*
 * RecordTypeValue.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2020 Apple Inc. and the FoundationDB project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apple.foundationdb.record.query.predicates;

import com.apple.foundationdb.annotation.API;
import com.apple.foundationdb.annotation.SpotBugsSuppressWarnings;
import com.apple.foundationdb.record.EvaluationContext;
import com.apple.foundationdb.record.ObjectPlanHash;
import com.apple.foundationdb.record.PlanHashable;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecord;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStoreBase;
import com.apple.foundationdb.record.query.plan.temp.AliasMap;
import com.apple.foundationdb.record.query.plan.temp.CorrelationIdentifier;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * A value which is unique for each record type produced by its quantifier.
 */
@API(API.Status.EXPERIMENTAL)
public class RecordTypeValue implements Value {
    private static final ObjectPlanHash BASE_HASH = new ObjectPlanHash("RecordType-Value");

    @Nonnull
    private final CorrelationIdentifier identifier;

    public RecordTypeValue(@Nonnull final CorrelationIdentifier identifier) {
        this.identifier = identifier;
    }

    @Nonnull
    @Override
    public Set<CorrelationIdentifier> getCorrelatedTo() {
        return Collections.singleton(identifier);
    }

    @Nonnull
    @Override
    public RecordTypeValue rebase(@Nonnull final AliasMap translationMap) {
        if (translationMap.containsSource(identifier)) {
            return new RecordTypeValue(translationMap.getTargetOrThrow(identifier));
        }
        return this;
    }

    @Nullable
    @Override
    public <M extends Message> Object eval(@Nonnull final FDBRecordStoreBase<M> store, @Nonnull final EvaluationContext context, @Nullable final FDBRecord<M> record, @Nullable final M message) {
        if (message == null) {
            return null;
        }
        final Object binding = context.getBinding(identifier);
        if (!(binding instanceof Message)) {
            return null;
        }

        return store.getRecordMetaData().getRecordType(((Message)binding).getDescriptorForType().getName()).getRecordTypeKey();
    }

    @Override
    public boolean semanticEquals(@Nullable final Object other, @Nonnull final AliasMap equivalenceMap) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final RecordTypeValue that = (RecordTypeValue)other;
        return equivalenceMap.containsMapping(identifier, that.identifier);
    }

    @Override
    public int semanticHashCode() {
        return PlanHashable.objectPlanHash(PlanHashKind.FOR_CONTINUATION, BASE_HASH);
    }

    @Override
    public int planHash(@Nonnull final PlanHashKind hashKind) {
        return PlanHashable.objectsPlanHash(hashKind, BASE_HASH);
    }

    @Override
    public int hashCode() {
        return semanticHashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @SpotBugsSuppressWarnings("EQ_UNUSUAL")
    @Override
    public boolean equals(final Object other) {
        return semanticEquals(other, AliasMap.identitiesFor(ImmutableSet.of(identifier)));
    }
}
