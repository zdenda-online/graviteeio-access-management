/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.common.vertx.web.handler;

import io.gravitee.am.gateway.handler.common.vertx.sstore.RepositorySessionStore;
import io.gravitee.am.model.Session;
import io.gravitee.am.repository.management.api.SessionRepository;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.ext.web.handler.SessionHandlerTestBase;
import io.vertx.reactivex.core.Vertx;
import org.junit.Test;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositorySessionHandlerTest extends SessionHandlerTestBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        store = RepositorySessionStore.create(Vertx.newInstance(vertx), getRepository());
    }

    @Test
    public void testRetryTimeout() throws Exception {
        assertTrue(doTestSessionRetryTimeout() < 3000);
    }

    private SessionRepository getRepository() {
        return new SessionRepository() {
            @Override
            public Completable clear() {
                return Completable.complete();
            }

            @Override
            public Maybe<Session> findById(String s) {
                return Maybe.empty();
            }

            @Override
            public Single<Session> create(Session item) {
                return Single.just(item);
            }

            @Override
            public Single<Session> update(Session item) {
                return Single.just(item);
            }

            @Override
            public Completable delete(String s) {
                return Completable.complete();
            }
        };
    }
 }
