/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.exadel.frs.core.trainservice.filter;

import com.exadel.frs.commonservice.enums.ModelType;
import com.exadel.frs.commonservice.enums.ValidationResult;
import com.exadel.frs.commonservice.exception.BadFormatModelKeyException;
import com.exadel.frs.commonservice.exception.IncorrectModelTypeException;
import com.exadel.frs.commonservice.exception.ModelNotFoundException;
import com.exadel.frs.commonservice.handler.ResponseExceptionHandler;
import com.exadel.frs.core.trainservice.service.ModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.exadel.frs.commonservice.enums.ModelType.*;
import static com.exadel.frs.commonservice.enums.ValidationResult.OK;
import static com.exadel.frs.core.trainservice.system.global.Constants.API_V1;
import static com.exadel.frs.core.trainservice.system.global.Constants.X_FRS_API_KEY_HEADER;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static java.util.function.Function.identity;

/**
 * Filter created to validate if this application has access to requested model
 */

@Component
@RequiredArgsConstructor
@Profile("!integration-test")
@Order(1)
public class SecurityValidationFilter implements Filter {

    public static final String VERIFICATION = "Verification";
    private final ModelService modelService;
    private final ResponseExceptionHandler handler;
    private final ObjectMapper objectMapper;

    @Override
    public void init(final FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain
    ) throws IOException, ServletException {
        val httpRequest = (HttpServletRequest) servletRequest;
        val httpResponse = (HttpServletResponse) servletResponse;

        String requestURI = httpRequest.getRequestURI();
        if (!requestURI.matches("^/(swagger|webjars|v2|api/v1/migrate|api/v1/consistence/status|api/v1/static).*$")) {
            val headersMap =
                    list(httpRequest.getHeaderNames()).stream()
                            .collect(Collectors.<String, String, List<String>>toMap(
                                    identity(),
                                    header -> list(httpRequest.getHeaders(header))
                            ));

            var apiKey = headersMap.getOrDefault(X_FRS_API_KEY_HEADER, emptyList());

            if (!apiKey.isEmpty()) {
                val key = apiKey.get(0);
                try {
                    if (key.length() < 36) {
                        throw new IllegalArgumentException("UUID length is incorrect");
                    }

                    UUID.fromString(key);
                } catch (Exception e) {
                    val objectResponseEntity = handler.handleDefinedExceptions(new BadFormatModelKeyException());
                    buildException(httpResponse, objectResponseEntity);

                    return;
                }
                ModelType modelType = getModelTypeByUrl(requestURI);
                ValidationResult validationResult = modelService.validateModelKey(key, modelType);
                if (validationResult != OK) {
                    String capitalize = ModelType.VERIFY.equals(modelType) ? VERIFICATION : StringUtils.capitalize(modelType.name().toLowerCase());
                    val objectResponseEntity = handler.handleDefinedExceptions(new ModelNotFoundException(key, capitalize));
                    buildException(httpResponse, objectResponseEntity);

                    return;
                }
            } else {
                val objectResponseEntity = handler.handleMissingRequestHeader(X_FRS_API_KEY_HEADER);
                buildException(httpResponse, objectResponseEntity);

                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    @SneakyThrows
    private void buildException(final HttpServletResponse response, final ResponseEntity<?> responseEntity) {
        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().append(objectMapper.writeValueAsString(responseEntity.getBody()));
        response.getWriter().flush();
    }

    private ModelType getModelTypeByUrl(String url) {
        if (url.contains(API_V1 + "/detection")) {
            return DETECTION;
        } else if (url.contains(API_V1 + "/verification")) {
            return VERIFY;
        } else if (url.contains(API_V1 + "/recognition")) {
            return RECOGNITION;
        }

        throw new IncorrectModelTypeException(url.substring(API_V1.length()));
    }
}
