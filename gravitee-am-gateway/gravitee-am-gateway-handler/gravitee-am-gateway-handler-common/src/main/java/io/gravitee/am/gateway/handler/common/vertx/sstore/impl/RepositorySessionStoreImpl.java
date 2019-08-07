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
package io.gravitee.am.gateway.handler.common.vertx.sstore.impl;

import io.gravitee.am.gateway.handler.common.vertx.sstore.RepositorySessionStore;
import io.gravitee.am.repository.management.api.SessionRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositorySessionStoreImpl implements RepositorySessionStore, Handler<Long>  {

    /**
     * Default of how often, in ms, to check for expired sessions
     */
    private static final long DEFAULT_REAPER_INTERVAL = 1000;

    /**
     * Default name for map used to store sessions
     */
    private static final String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";

    private LocalMap<String, Session> localMap;
    private long reaperInterval;
    private long timerID = -1;
    private boolean closed;
    protected Vertx vertx;
    private PRNG random;
    private SessionRepository sessionRepository;

    public RepositorySessionStoreImpl() {}

    public RepositorySessionStoreImpl(Vertx vertx, SessionRepository sessionRepository) {
        init(vertx, new JsonObject());
        this.sessionRepository = sessionRepository;
    }

    @Override
    public SessionStore init(Vertx vertx, JsonObject options) {
        // initialize a secure random
        this.random = new PRNG(vertx);
        this.vertx = vertx;
        this.reaperInterval = DEFAULT_REAPER_INTERVAL;
        localMap = vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME);
        setTimer();
        return this;
    }

    @Override
    public long retryTimeout() {
        return 0;
    }

    @Override
    public Session createSession(long timeout) {
        return new RepositorySession(random, timeout, DEFAULT_SESSIONID_LENGTH);
    }

    @Override
    public Session createSession(long timeout, int length) {
        return new RepositorySession(random, timeout, length);
    }

    @Override
    public void get(String cookieValue, Handler<AsyncResult<Session>> resultHandler) {
        // check local cache first
        Session localSession = localMap.get(cookieValue);
        if (localSession != null) {
            resultHandler.handle(Future.succeededFuture(localSession));
            return;
        }

        // fallback to repository store
        sessionRepository.findById(cookieValue)
                .subscribe(
                        session -> {
                            // reconstruct the session
                            RepositorySession repositorySession = new RepositorySession(random);
                            repositorySession.readFromBuffer(0, Buffer.buffer(session.getValue()));
                            // need to validate for expired
                            long now = System.currentTimeMillis();
                            // if expired, the operation succeeded, but returns null
                            if (now - repositorySession.lastAccessed() > repositorySession.timeout()) {
                                resultHandler.handle(Future.succeededFuture());
                            } else {
                                // update local cache and return the already recreated session
                                localMap.put(cookieValue, repositorySession);
                                resultHandler.handle(Future.succeededFuture(repositorySession));
                            }
                        },
                        e -> resultHandler.handle(Future.failedFuture(e)),
                        () -> resultHandler.handle(Future.succeededFuture()));
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Void>> resultHandler) {
        localMap.remove(id);
        sessionRepository.delete(id)
                .subscribe(
                        () -> resultHandler.handle(Future.succeededFuture()),
                        error -> resultHandler.handle(Future.failedFuture(error)));

    }

    @Override
    public void put(Session session, Handler<AsyncResult<Void>> resultHandler) {
        final RepositorySession oldSession = (RepositorySession) localMap.get(session.id());
        final RepositorySession newSession = (RepositorySession) session;

        if (oldSession != null) {
            // there was already some stored data in this case we need to validate versions
            if (oldSession.version() != newSession.version()) {
                resultHandler.handle(Future.failedFuture("Version mismatch"));
                return;
            }
        }
        newSession.incrementVersion();
        localMap.put(session.id(), newSession);

        // update the sessions store
        sessionRepository.findById(session.id())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMapSingle(optionalSession -> {
                    if (optionalSession.isPresent()) {
                        io.gravitee.am.model.Session storedSession = optionalSession.get();
                        storedSession.setId(newSession.id());
                        storedSession.setValue(getSessionData(newSession));
                        storedSession.setExpireAt(new Date(System.currentTimeMillis() + newSession.timeout()));
                        storedSession.setUpdatedAt(new Date());
                        return sessionRepository.update(storedSession);
                    } else {
                        io.gravitee.am.model.Session sessionToCreate = new io.gravitee.am.model.Session();
                        sessionToCreate.setId(newSession.id());
                        sessionToCreate.setValue(getSessionData(newSession));
                        sessionToCreate.setExpireAt(new Date(System.currentTimeMillis() + newSession.timeout()));
                        sessionToCreate.setCreatedAt(new Date());
                        sessionToCreate.setUpdatedAt(sessionToCreate.getCreatedAt());
                        return sessionRepository.create(sessionToCreate);
                    }
                })
                .subscribe(
                        __ ->  resultHandler.handle(Future.succeededFuture()),
                        error -> resultHandler.handle(Future.failedFuture(error.getMessage())));
    }

    @Override
    public void clear(Handler<AsyncResult<Void>> resultHandler) {
        localMap.clear();
        sessionRepository.clear()
                .subscribe(
                        () -> resultHandler.handle(Future.succeededFuture()),
                        error -> resultHandler.handle(Future.failedFuture(error)));

    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(localMap.size()));
    }

    @Override
    public void close() {
        localMap.close();
        if (timerID != -1) {
            vertx.cancelTimer(timerID);
        }
        // stop seeding the PRNG
        random.close();
        closed = true;
    }

    private byte[] getSessionData(RepositorySession repositorySession) {
        Buffer buffer = Buffer.buffer();
        repositorySession.writeToBuffer(buffer);
        return buffer.getBytes();
    }

    @Override
    public synchronized void handle(Long tid) {
        long now = System.currentTimeMillis();
        Set<String> toRemove = new HashSet<>();
        for (Session session: localMap.values()) {
            if (now - session.lastAccessed() > session.timeout()) {
                toRemove.add(session.id());
            }
        }
        for (String id: toRemove) {
            localMap.remove(id);
        }
        if (!closed) {
            setTimer();
        }
    }

    private void setTimer() {
        if (reaperInterval != 0) {
            timerID = vertx.setTimer(reaperInterval, this);
        }
    }
}
