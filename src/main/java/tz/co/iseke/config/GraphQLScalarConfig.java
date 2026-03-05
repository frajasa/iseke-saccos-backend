package tz.co.iseke.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class GraphQLScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(dateScalar())
                .scalar(dateTimeScalar())
                .scalar(decimalScalar());
    }

    /**
     * Custom Date scalar (LocalDate)
     * Format: yyyy-MM-dd
     */
    private GraphQLScalarType dateScalar() {
        return GraphQLScalarType.newScalar()
                .name("Date")
                .description("Date scalar type for LocalDate (yyyy-MM-dd)")
                .coercing(new Coercing<LocalDate, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDate) {
                            return ((LocalDate) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE);
                        } else if (dataFetcherResult instanceof String) {
                            return (String) dataFetcherResult;
                        }
                        throw new CoercingSerializeException("Expected a LocalDate object.");
                    }

                    @Override
                    public LocalDate parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDate.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE);
                            } else if (input instanceof LocalDate) {
                                return (LocalDate) input;
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException("Invalid date format. Expected yyyy-MM-dd", e);
                        }
                    }

                    @Override
                    public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDate.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException("Invalid date format. Expected yyyy-MM-dd", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue");
                    }
                })
                .build();
    }

    /**
     * Custom DateTime scalar (LocalDateTime)
     * Format: yyyy-MM-dd'T'HH:mm:ss
     */
    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("DateTime scalar type for LocalDateTime (ISO-8601)")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } else if (dataFetcherResult instanceof String) {
                            return (String) dataFetcherResult;
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } else if (input instanceof LocalDateTime) {
                                return (LocalDateTime) input;
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException("Invalid datetime format. Expected ISO-8601 format", e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDateTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException("Invalid datetime format. Expected ISO-8601 format", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue");
                    }
                })
                .build();
    }

    /**
     * Custom Decimal scalar (BigDecimal)
     * Handles precise decimal numbers
     */
    private GraphQLScalarType decimalScalar() {
        return GraphQLScalarType.newScalar()
                .name("Decimal")
                .description("Decimal scalar type for BigDecimal")
                .coercing(new Coercing<BigDecimal, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof BigDecimal) {
                            return ((BigDecimal) dataFetcherResult).toPlainString();
                        } else if (dataFetcherResult instanceof String) {
                            return (String) dataFetcherResult;
                        } else if (dataFetcherResult instanceof Number) {
                            return BigDecimal.valueOf(((Number) dataFetcherResult).doubleValue()).toPlainString();
                        }
                        throw new CoercingSerializeException("Expected a BigDecimal or Number object.");
                    }

                    @Override
                    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return new BigDecimal((String) input);
                            } else if (input instanceof BigDecimal) {
                                return (BigDecimal) input;
                            } else if (input instanceof Number) {
                                return BigDecimal.valueOf(((Number) input).doubleValue());
                            }
                            throw new CoercingParseValueException("Expected a String or Number");
                        } catch (NumberFormatException e) {
                            throw new CoercingParseValueException("Invalid decimal format", e);
                        }
                    }

                    @Override
                    public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return new BigDecimal(((StringValue) input).getValue());
                            } catch (NumberFormatException e) {
                                throw new CoercingParseLiteralException("Invalid decimal format", e);
                            }
                        } else if (input instanceof graphql.language.IntValue) {
                            return BigDecimal.valueOf(((graphql.language.IntValue) input).getValue().longValue());
                        } else if (input instanceof graphql.language.FloatValue) {
                            return ((graphql.language.FloatValue) input).getValue();
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue, IntValue, or FloatValue");
                    }
                })
                .build();
    }
}