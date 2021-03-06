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

package org.garnishtest.steps.restclient;

import org.garnishtest.modules.generic.httpclient.HttpRequestBuilder;
import org.garnishtest.modules.generic.httpclient.SimpleHttpClient;
import org.garnishtest.modules.generic.httpclient.model.HttpMethod;
import org.garnishtest.modules.generic.httpclient.model.body.impl.MultiPartBodyPublisher;
import org.garnishtest.modules.generic.springutils.ClasspathUtils;
import org.garnishtest.modules.generic.uri.UriQuery;
import org.garnishtest.modules.generic.variables_resolver.impl.escape.impl.ValueEscapers;
import org.garnishtest.modules.it.test_utils_json.JsonUtils;
import org.garnishtest.steps.restclient.manager.ApiClientResponseManager;
import org.garnishtest.steps.vars.resource_files_vars.ResourceFilesVariables;
import org.garnishtest.steps.vars.scenario_attrs.ScenarioAttribute;
import org.garnishtest.steps.vars.scenario_user_vars.ScenarioUserVariables;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.MimeTypeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class RestClientCallSteps {

    @NonNull private static final ScenarioAttribute<MultiPartBodyPublisher>
        MULTIPART_REQUEST_SCENARIO_ATTR = ScenarioAttribute.create();

    @NonNull private static final ScenarioAttribute<Map<String, String>> PREPARED_HEADERS = ScenarioAttribute.create();
    @NonNull private static final ScenarioAttribute<String> PREPARED_REQUEST_BODY = ScenarioAttribute.create();

    @NonNull private final ApiClientResponseManager responseManager;

    @NonNull private final SimpleHttpClient httpClient;

    @Autowired
    public RestClientCallSteps(@NonNull @Qualifier("garnishStepsRestClient_responseManager") final ApiClientResponseManager responseManager,
                               @NonNull @Qualifier("garnishStepsRestClient_httpClient") final SimpleHttpClient httpClient) {
        this.responseManager = responseManager;
        this.httpClient = httpClient;
    }

    @Given("^the request headers$")
    public void the_request_headers(@NonNull final Map<String, String> requestHeaders) {
        PREPARED_HEADERS.setValue(requestHeaders);
    }

    @Given("^the request form body$")
    public void the_request_form_body(@NonNull final Map<String, String> requestFormParameters) {
        final UriQuery query = UriQuery.fromSingleValuedParameters(requestFormParameters);

        PREPARED_REQUEST_BODY.setValue(
                query.toStringWithoutInitialQuestionMark()
        );
    }

    @When("^I call '(PUT|POST|PATCH)' on '(.+)' with previously prepared request$")
    public void when_call_method_on_url_with_previously_prepared_request(
            @NonNull final HttpMethod method,
            @NonNull final String url
    ) {
        final String requestBody = PREPARED_REQUEST_BODY.getValue();
        if (requestBody == null) {
            throw new IllegalArgumentException("missing request body; please use one of the steps that set it");
        }



        HttpRequestBuilder requestBuilder = this.httpClient.request(method, url);

        final Map<String, String> requestHeaders = PREPARED_HEADERS.getValue();
        if (requestHeaders != null) {
            for (final Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        requestBuilder = requestBuilder.body(requestBody);

        final HttpResponse<String> response = requestBuilder.execute();

        this.responseManager.setResponse(response);
    }

    @When("^I call '(POST|PUT|PATCH)' on '(.+)' with JSON from '(.+)' and previously provided headers$")
    public void callMethodOnUrlWithJsonBodyAndHeaders(
            @NonNull final HttpMethod method,
            @NonNull final String url,
            @NonNull final String jsonReqBodyFile
    ) {
        String jsonBody = ClasspathUtils.loadFromClasspath(ResourceFilesVariables.getResourceFilesPrefix() + jsonReqBodyFile);

        jsonBody = ScenarioUserVariables.resolveInText(jsonBody, ValueEscapers.json());
        jsonBody = JsonUtils.makeValidJson(jsonBody);

        HttpRequestBuilder requestBuilder = this.httpClient.request(method, url);

        final Map<String, String> requestHeaders = PREPARED_HEADERS.getValue();
        if (requestHeaders != null) {
            for (final Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        requestBuilder = requestBuilder.body(jsonBody, MimeTypeUtils.APPLICATION_JSON);

        final HttpResponse<String> response = requestBuilder.execute();

        this.responseManager.setResponse(response);
    }

    @When("^I call '(PUT|POST|PATCH)' on '(.+)' with JSON from '(.+)'$")
    public void callMethodOnUrlWithJsonBody(
            @NonNull final HttpMethod method,
            @NonNull final String url,
            @NonNull final String jsonReqBodyFile
    ) {
        String jsonBody = ClasspathUtils.loadFromClasspath(ResourceFilesVariables.getResourceFilesPrefix() + jsonReqBodyFile);

        jsonBody = ScenarioUserVariables.resolveInText(jsonBody, ValueEscapers.json());
        jsonBody = JsonUtils.makeValidJson(jsonBody);

        final HttpResponse<String> response =
            this.httpClient.request(method, url).body(jsonBody, MimeTypeUtils.APPLICATION_JSON)
                                                     .execute();

        this.responseManager.setResponse(response);
    }

    @When("^I call '(PUT|POST|PATCH)' on '(.+)' with JSON '(.+)'$")
    public void callMethodOnUrlWithJsonBodyFromString(
            @NonNull final HttpMethod method,
            @NonNull final String url,
            @NonNull final String jsonReqBody
    ) {

        // todo: move these 2 into support code (into the client of another class)
        String jsonBody = jsonReqBody;
        jsonBody = ScenarioUserVariables.resolveInText(jsonBody, ValueEscapers.json());
        jsonBody = JsonUtils.makeValidJson(jsonBody);


        final HttpResponse<String> response =
            this.httpClient.request(method, url).body(jsonBody, MimeTypeUtils.APPLICATION_JSON)
                                                     .execute();

        this.responseManager.setResponse(response);
    }

    @When("^I call '(PUT|POST|PATCH)' on '(.+)' with body '(.+)'$")
    public void callMethodOnUrlWithBodyFromString(
            @NonNull final HttpMethod method,
            @NonNull final String url,
            @NonNull final String reqBody
    ) {

        // todo: move into support code (into the client of another class)
        String body = reqBody;
        body = ScenarioUserVariables.resolveInText(body, ValueEscapers.json());

        final HttpResponse<String> response = this.httpClient.request(method, url)
                                                     .body(body)
                                                     .execute();

        this.responseManager.setResponse(response);
    }

    @Given("^a multipart request field '([^']+)' with value '([^']*)' and content type '([^']+)'$")
    public void a_multipart_request_field_name_value_contentType(@NonNull final String fieldName,
                                                                 @NonNull final String fieldValue,
                                                                 @NonNull final String contentType) throws UnsupportedEncodingException {
        MultiPartBodyPublisher multipart = MULTIPART_REQUEST_SCENARIO_ATTR.getValue();
        if (multipart == null) {
            multipart = new MultiPartBodyPublisher();
            MULTIPART_REQUEST_SCENARIO_ATTR.setValue(multipart);
        }


        final Charset charset = MimeTypeUtils.parseMimeType(contentType).getCharset();
        byte[] fieldValueBytes =
            fieldValue.getBytes(charset != null ? charset : StandardCharsets.US_ASCII);

        multipart.addPart(fieldName, new String(fieldValueBytes, StandardCharsets.UTF_8));
    }

    @Given("^a multipart request field '([^']+)' with value from file '([^']+)' and content type '([^']+)'$")
    public void a_multipart_request_field_name_fileValue_contentType(@NonNull final String fieldName,
                                                                     @NonNull final String fieldValueFilePath,
                                                                     @NonNull final String contentType) throws UnsupportedEncodingException {
        a_multipart_request_field_name_value_contentType(
                fieldName,
                ClasspathUtils.loadFromClasspath(ResourceFilesVariables.getResourceFilesPrefix() + fieldValueFilePath),
                contentType
        );
    }

    @Given("^a multipart request field '([^']+)' with value from JSON file '([^']+)'$")
    public void a_multipart_request_field_name_jsonFileValue_contentType(@NonNull final String fieldName,
                                                                         @NonNull final String fieldValueJsonFilePath) throws UnsupportedEncodingException {
        String jsonBody = ClasspathUtils.loadFromClasspath(ResourceFilesVariables.getResourceFilesPrefix() + fieldValueJsonFilePath);

        jsonBody = ScenarioUserVariables.resolveInText(jsonBody, ValueEscapers.json());
        jsonBody = JsonUtils.makeValidJson(jsonBody);

        a_multipart_request_field_name_value_contentType(fieldName, jsonBody,
            MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    @When("^I call multipart '(GET|PUT|POST|DELETE|HEAD|OPTIONS)' on '(.+)'$")
    public void callMultipartMethodOnUrlWithJsonBody(
            @NonNull final HttpMethod method,
            @NonNull final String url
    ) {
        final MultiPartBodyPublisher multipartBody =
            MULTIPART_REQUEST_SCENARIO_ATTR.getRequiredValue();
        final HttpResponse<String> response = this.httpClient.request(method, url)
                                                     .body(multipartBody)
                                                     .execute();

        this.responseManager.setResponse(response);
    }

    @When("^I call '(GET|DELETE|HEAD|OPTIONS|TRACE|PUT|POST|PATCH)' on '(.+)' without body$")
    public void callMethodOnUrlWithoutBody(
            @NonNull final HttpMethod method,
            @NonNull final String url
    ) throws URISyntaxException {
        final HttpResponse<String> response = performJsonRequestWithoutBody(method, url);

        this.responseManager.setResponse(response);
    }

    // todo: move this method to a separate support class
    public HttpResponse<String> performJsonRequestWithoutBody(@NonNull final HttpMethod method,
        @NonNull final String url) {
        return this.httpClient.request(method, url)
                              .setHeader("Content-Type", "application/json")
                              .execute();
    }

}
