/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * GraphQL handler to be exposed as a WebFlux.fn endpoint via
 * {@link org.springframework.web.reactive.function.server.RouterFunctions}.
 */
public class WebFluxGraphQLHandler implements HandlerFunction<ServerResponse> {

	private final GraphQL graphQL;

	public WebFluxGraphQLHandler(GraphQL.Builder graphQLBuilder) {
		this.graphQL = graphQLBuilder.build();
	}

	public Mono<ServerResponse> handle(ServerRequest request) {
		return request.bodyToMono(RequestInput.class)
				.flatMap(requestInput -> {
					requestInput.validate();
					ExecutionInput executionInput = ExecutionInput.newExecutionInput()
							.query(requestInput.getQuery())
							.operationName(requestInput.getOperationName())
							.variables(requestInput.getVariables())
							.build();
					// Invoke GraphQLInterceptor's preHandle here
					return customizeExecutionInput(executionInput, request.headers().asHttpHeaders());
				})
				.flatMap(input -> {
					// Invoke GraphQLInterceptor's postHandle here
					return execute(input);
				})
				.flatMap(result -> ServerResponse.ok().bodyValue(result.toSpecification()));
	}

	protected Mono<ExecutionInput> customizeExecutionInput(ExecutionInput input, HttpHeaders headers) {
		return Mono.just(input);
	}

	protected Mono<ExecutionResult> execute(ExecutionInput input) {
		return Mono.fromFuture(graphQL.executeAsync(input));
	}

}
