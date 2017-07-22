/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.testing.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.config.CharCodingConfig;
import org.apache.hc.core5.http.config.H1Config;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.nio.support.AsyncServerExchangeHandlerRegistry;
import org.apache.hc.core5.http.nio.AsyncServerExchangeHandler;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.support.BasicServerExchangeHandler;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.http2.impl.Http2Processors;
import org.apache.hc.core5.reactor.IOEventHandlerFactory;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;

public class Http2TestServer extends AsyncServer {

    private final SSLContext sslContext;
    private final AsyncServerExchangeHandlerRegistry handlerRegistry;

    public Http2TestServer(final IOReactorConfig ioReactorConfig, final SSLContext sslContext) throws IOException {
        super(ioReactorConfig);
        this.sslContext = sslContext;
        this.handlerRegistry = new AsyncServerExchangeHandlerRegistry("localhost");
    }

    public Http2TestServer() throws IOException {
        this(IOReactorConfig.DEFAULT, null);
    }

    public void register(final String uriPattern, final Supplier<AsyncServerExchangeHandler> supplier) {
        handlerRegistry.register(null, uriPattern, supplier);
    }

    public <T> void register(
            final String uriPattern,
            final AsyncServerRequestHandler<T> requestHandler) {
        register(uriPattern, new Supplier<AsyncServerExchangeHandler>() {

            @Override
            public AsyncServerExchangeHandler get() {
                return new BasicServerExchangeHandler<>(requestHandler);
            }

        });
    }

    public void start(final IOEventHandlerFactory handlerFactory) throws IOException {
        execute(handlerFactory);
    }

    public InetSocketAddress start(final HttpProcessor httpProcessor, final H2Config h2Config) throws Exception {
        start(new InternalServerHttp2EventHandlerFactory(
                httpProcessor,
                handlerRegistry,
                HttpVersionPolicy.FORCE_HTTP_2,
                h2Config,
                H1Config.DEFAULT,
                CharCodingConfig.DEFAULT,
                sslContext));
        final Future<ListenerEndpoint> future = listen(new InetSocketAddress(0));
        final ListenerEndpoint listener = future.get();
        return (InetSocketAddress) listener.getAddress();
    }

    public InetSocketAddress start(final HttpProcessor httpProcessor, final H1Config h1Config) throws Exception {
        start(new InternalServerHttp2EventHandlerFactory(
                httpProcessor,
                handlerRegistry,
                HttpVersionPolicy.FORCE_HTTP_1,
                H2Config.DEFAULT,
                h1Config,
                CharCodingConfig.DEFAULT,
                sslContext));
        final Future<ListenerEndpoint> future = listen(new InetSocketAddress(0));
        final ListenerEndpoint listener = future.get();
        return (InetSocketAddress) listener.getAddress();
    }

    public InetSocketAddress start(final H2Config h2Config) throws Exception {
        return start(Http2Processors.server(), h2Config);
    }

    public InetSocketAddress start(final H1Config h1Config) throws Exception {
        return start(HttpProcessors.server(), h1Config);
    }

    public InetSocketAddress start() throws Exception {
        return start(H2Config.DEFAULT);
    }

}
