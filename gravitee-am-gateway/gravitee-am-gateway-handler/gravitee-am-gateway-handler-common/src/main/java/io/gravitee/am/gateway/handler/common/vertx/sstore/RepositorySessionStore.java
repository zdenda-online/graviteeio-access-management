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
package io.gravitee.am.gateway.handler.common.vertx.sstore;

import io.gravitee.am.gateway.handler.common.vertx.sstore.impl.RepositorySessionStoreImpl;
import io.gravitee.am.repository.management.api.SessionRepository;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.reactivex.core.Vertx;

/**
 * A session store based on an underlying repository
 *
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RepositorySessionStore extends SessionStore {

    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @return the session store
     */
    static RepositorySessionStore create(Vertx vertx, SessionRepository sessionRepository) {
        return new RepositorySessionStoreImpl(vertx.getDelegate(), sessionRepository);
    }
}
