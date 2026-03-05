package tz.co.iseke.config;


import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.GraphQLScalarType;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@RequiredArgsConstructor
public class GraphQLConfig {

    private final ResourceLoader resourceLoader;

    @Bean
    public GraphQL graphQL(GraphQLSchema graphQLSchema) {
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Bean
    public GraphQLSchema graphQLSchema() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:graphql/schema.graphqls");

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry;

        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            typeDefinitionRegistry = schemaParser.parse(reader);
        }

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .scalar(dateScalar())
                .scalar(dateTimeScalar())
                .scalar(decimalScalar())
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    private GraphQLScalarType dateScalar() {
        return GraphQLScalarType.newScalar()
                .name("Date")
                .description("Java LocalDate as scalar")
                .coercing(new Coercing<LocalDate, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDate) {
                            return ((LocalDate) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE);
                        }
                        throw new CoercingSerializeException("Expected a LocalDate object.");
                    }

                    @Override
                    public LocalDate parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDate.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE);
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Not a valid date", e);
                        }
                    }

                    @Override
                    public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof String) {
                            return LocalDate.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE);
                        }
                        throw new CoercingParseLiteralException("Expected a String");
                    }
                })
                .build();
    }

    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("Java LocalDateTime as scalar")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Not a valid datetime", e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof String) {
                            return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingParseLiteralException("Expected a String");
                    }
                })
                .build();
    }

    private GraphQLScalarType decimalScalar() {
        return GraphQLScalarType.newScalar()
                .name("Decimal")
                .description("Java BigDecimal as scalar")
                .coercing(new Coercing<BigDecimal, BigDecimal>() {
                    @Override
                    public BigDecimal serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof BigDecimal) {
                            return (BigDecimal) dataFetcherResult;
                        } else if (dataFetcherResult instanceof String) {
                            return new BigDecimal((String) dataFetcherResult);
                        } else if (dataFetcherResult instanceof Integer) {
                            return new BigDecimal((Integer) dataFetcherResult);
                        } else if (dataFetcherResult instanceof Long) {
                            return new BigDecimal((Long) dataFetcherResult);
                        } else if (dataFetcherResult instanceof Double) {
                            return BigDecimal.valueOf((Double) dataFetcherResult);
                        }
                        throw new CoercingSerializeException("Expected a BigDecimal object.");
                    }

                    @Override
                    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof BigDecimal) {
                                return (BigDecimal) input;
                            } else if (input instanceof String) {
                                return new BigDecimal((String) input);
                            } else if (input instanceof Integer) {
                                return new BigDecimal((Integer) input);
                            } else if (input instanceof Long) {
                                return new BigDecimal((Long) input);
                            } else if (input instanceof Double) {
                                return BigDecimal.valueOf((Double) input);
                            }
                            throw new CoercingParseValueException("Expected a valid decimal value");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Not a valid decimal", e);
                        }
                    }

                    @Override
                    public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                        try {
                            if (input instanceof Number) {
                                return BigDecimal.valueOf(((Number) input).doubleValue());
                            }
                            throw new CoercingParseLiteralException("Expected a Number");
                        } catch (Exception e) {
                            throw new CoercingParseLiteralException("Not a valid decimal", e);
                        }
                    }
                })
                .build();
    }
}