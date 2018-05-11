/*
 * Copyright 2016-2018, Garnish.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.garnishtest.modules.generic.httpclient.executor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.garnishtest.modules.generic.httpclient.HttpRequestBuilderImpl;
import org.garnishtest.modules.generic.httpclient.model.HttpMethod;
import org.garnishtest.modules.generic.httpclient.model.HttpRequest;
import org.garnishtest.modules.generic.httpclient.model.HttpResponse;
import org.garnishtest.modules.generic.httpclient.model.body.HttpRequestBody;
import org.garnishtest.modules.generic.httpclient.request_preparer.HttpRequestPreparer;
import org.garnishtest.modules.generic.uri.util.MultiMapBuilder;
import lombok.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpRequestExecutor {

    private static final CreateHttpResponseResponseHandler CREATE_HTTP_RESPONSE_RESPONSE_HANDLER = new CreateHttpResponseResponseHandler();
    @NonNull
    private final HttpClient client;
    @NonNull
    private final HttpRequestPreparer httpRequestPreparer;

    public HttpRequestExecutor(@NonNull final HttpClient client,
                               @NonNull final HttpRequestPreparer httpRequestPreparer) {
        this.client = client;
        this.httpRequestPreparer = httpRequestPreparer;
    }

    public HttpResponse execute(@NonNull final HttpRequestBuilderImpl requestBuilder) throws HttpRequestExecutorException {
        try {
            this.httpRequestPreparer.prepareRequest(requestBuilder);
        } catch (final Exception e) {
            throw new HttpRequestExecutorException("failed to prepare request [" + requestBuilder.build() + "]", e);
        }

        final HttpRequest request = requestBuilder.build();
        try {
            final HttpUriRequest uriRequest = createRequest(request);

            return this.client.execute(uriRequest, CREATE_HTTP_RESPONSE_RESPONSE_HANDLER);
        } catch (final Exception e) {
            throw new HttpRequestExecutorException("failed to execute request [" + request + "]", e);
        }
    }

    @NonNull
    private HttpUriRequest createRequest(@NonNull final HttpRequest request) throws UnsupportedEncodingException {
        final HttpUriRequest httpClientRequest = createHttpClientRequest(request);

        setHeaders(request, httpClientRequest);
        setBody(request, httpClientRequest);

        return httpClientRequest;
    }

    @NonNull
    private HttpUriRequest createHttpClientRequest(@NonNull final HttpRequest request) {
        final HttpMethod method = request.getMethod();
        final String url = request.getUrl().toString();

        switch (method) {
            case GET:
                return new HttpGet(url);
            case DELETE:
                return new HttpDelete(url);
            case HEAD:
                return new HttpHead(url);
            case OPTIONS:
                return new HttpOptions(url);
            case TRACE:
                return new HttpTrace(url);
            case PUT:
                return new HttpPut(url);
            case POST:
                return new HttpPost(url);
            case PATCH:
                return new HttpPatch(url);
            default:
                throw new IllegalArgumentException("unknown HTTP method [" + method + "]");
        }
    }

    private void setBody(@NonNull final HttpRequest request,
                         @NonNull final HttpUriRequest httpClientRequest) throws UnsupportedEncodingException {
        if (!request.getMethod().canHaveBody()) {
            return;
        }

        final HttpRequestBody body = request.getBody();
        if (body == null) {
            return;
        }

        final HttpEntityEnclosingRequestBase httpEntityEnclosingRequest = (HttpEntityEnclosingRequestBase) httpClientRequest;
        httpEntityEnclosingRequest.setEntity(
                request.getBody().createEntity()
        );
    }

    private void setHeaders(final HttpRequest request, final HttpUriRequest uriRequest) {
        for (final Map.Entry<String, ImmutableList<String>> entry : request.getHeaders().entrySet()) {
            final String name = entry.getKey();

            for (final String value : entry.getValue()) {
                uriRequest.addHeader(name, value);
            }
        }
    }


    private static class CreateHttpResponseResponseHandler implements ResponseHandler<HttpResponse> {
        @Override
        public HttpResponse handleResponse(final org.apache.http.HttpResponse response) throws IOException {
            final int statusCode = getStatusCode(response);
            final ImmutableMap<String, ImmutableList<String>> headers = getHeaders(response);
            final String body = getBody(response);

            return new HttpResponse(statusCode, headers, body);
        }

        private int getStatusCode(final org.apache.http.HttpResponse response) {
            return response.getStatusLine().getStatusCode();
        }

        private ImmutableMap<String, ImmutableList<String>> getHeaders(final org.apache.http.HttpResponse response) {
            final MultiMapBuilder<String, String> headersBuilder = new MultiMapBuilder<>();
            for (final Header header : response.getAllHeaders()) {
                headersBuilder.addValue(
                        header.getName(),
                        header.getValue()
                );
            }

            return headersBuilder.build();
        }

        @NonNull
        private String getBody(final org.apache.http.HttpResponse response) throws IOException {
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                return "";
            }

            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
    }
}
