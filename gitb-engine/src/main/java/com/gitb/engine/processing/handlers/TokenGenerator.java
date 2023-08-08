package com.gitb.engine.processing.handlers;

import com.gitb.core.*;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.NumberType;
import com.gitb.types.StringType;
import com.github.curiousoddman.rgxgen.RgxGen;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@ProcessingHandler(name="TokenGenerator")
public class TokenGenerator extends AbstractProcessingHandler {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
    private static final String OPERATION__TIMESTAMP = "timestamp";
    private static final String OPERATION__UUID = "uuid";
    private static final String OPERATION__STRING = "string";
    private static final String OPERATION__RANDOM = "random";
    private static final String INPUT__FORMAT = "format";
    private static final String INPUT__TIME = "time";
    private static final String INPUT__DATE = "date";
    private static final String INPUT__INPUT_FORMAT = "inputFormat";
    private static final String INPUT__DIFF = "diff";
    private static final String INPUT__ZONE = "zone";
    private static final String INPUT__PREFIX = "prefix";
    private static final String INPUT__POSTFIX = "postfix";
    private static final String INPUT__MINIMUM = "minimum";
    private static final String INPUT__MAXIMUM = "maximum";
    private static final String INPUT__INTEGER = "integer";
    private static final String OUTPUT__VALUE = "value";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("TokenGenerator");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());

        TypedParameter uuidPrefix = createParameter(INPUT__PREFIX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "A text to prepend to the generated UUID.");
        TypedParameter uuidPostfix = createParameter(INPUT__POSTFIX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "A text to append to the generated UUID.");
        TypedParameter outputText = createParameter(OUTPUT__VALUE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output value.");
        TypedParameter timestampFormat = createParameter(INPUT__FORMAT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The optional format string to apply (see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html). Default is the epoch milliseconds.");
        TypedParameter milliseconds = createParameter(INPUT__TIME, "number", UsageEnumeration.O, ConfigurationType.SIMPLE, "The optional time (in epoch milliseconds) to use as the value (default is the current time).");
        TypedParameter inputDate = createParameter(INPUT__DATE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The optional date/time as a formatted date string to use as the value (default is the current time).");
        TypedParameter inputDateFormat = createParameter(INPUT__INPUT_FORMAT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "When the "+INPUT__DATE+" input is provided this is the optional format string to use to parse its value (see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html). If not provided an ISO 8601 date/time format is assumed (yyyy-MM-dd'T'HH:mm:ss");
        TypedParameter diff = createParameter(INPUT__DIFF, "number", UsageEnumeration.O, ConfigurationType.SIMPLE, "The number of milliseconds to apply as a diff to the base time.");
        TypedParameter zone = createParameter(INPUT__ZONE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The timezone to consider (default is UTC).");
        TypedParameter regexpFormat = createParameter(INPUT__FORMAT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "A regular expression defining the syntax and static parts of the returned string.");
        TypedParameter minimum = createParameter(INPUT__MINIMUM, "number", UsageEnumeration.O, ConfigurationType.SIMPLE, "The minimum bound (inclusive) for the generated random value.");
        TypedParameter maximum = createParameter(INPUT__MAXIMUM, "number", UsageEnumeration.O, ConfigurationType.SIMPLE, "The maximum bound (exclusive) for the generated random value.");
        TypedParameter integer = createParameter(INPUT__INTEGER, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the random number to be generated shall be an integer (by default it is a double).");

        module.getOperation().add(createProcessingOperation(OPERATION__UUID, List.of(uuidPrefix, uuidPostfix), List.of(outputText)));
        module.getOperation().add(createProcessingOperation(OPERATION__TIMESTAMP, List.of(timestampFormat, milliseconds, inputDate, inputDateFormat, diff, zone), List.of(outputText)));
        module.getOperation().add(createProcessingOperation(OPERATION__STRING, List.of(regexpFormat), List.of(outputText)));
        module.getOperation().add(createProcessingOperation(OPERATION__RANDOM, List.of(minimum, maximum, integer), List.of(outputText)));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        String value;
        if (OPERATION__UUID.equalsIgnoreCase(operation)) {
            var prefix = getInputForName(input, INPUT__PREFIX, StringType.class);
            var postfix = getInputForName(input, INPUT__POSTFIX, StringType.class);
            var prefixString = (prefix == null) ? "" : (String) prefix.getValue();
            var postfixString = (postfix == null) ? "" : (String) postfix.getValue();
            value = String.format("%s%s%s", prefixString, UUID.randomUUID(), postfixString);
        } else if (OPERATION__RANDOM.equalsIgnoreCase(operation)) {
            var minimumBound = getInputForName(input, INPUT__MINIMUM, NumberType.class);
            var maximumBound = getInputForName(input, INPUT__MAXIMUM, NumberType.class);
            var integer = getInputForName(input, INPUT__INTEGER, BooleanType.class);
            var random = new Random();
            Number result;
            if (integer != null && (Boolean) integer.getValue()) {
                // Generate as an integer
                int minimumToUse = 0;
                if (minimumBound != null) {
                    minimumToUse = minimumBound.intValue();
                }
                int maximumToUse = Integer.MAX_VALUE;
                if (maximumBound != null) {
                    maximumToUse = maximumBound.intValue();
                }
                if (minimumToUse >= maximumToUse) {
                    throw new IllegalArgumentException("The minimum bound must be less than the maximum bound.");
                }
                result = random.nextInt(maximumToUse - minimumToUse) + minimumToUse;
            } else {
                // Generate as a double.
                double minimumToUse = 0.0;
                if (minimumBound != null) {
                    minimumToUse = minimumBound.doubleValue();
                }
                if (maximumBound != null) {
                    if (minimumToUse >= maximumBound.doubleValue()) {
                        throw new IllegalArgumentException("The minimum bound must be less than the maximum bound.");
                    }
                    result = random.nextDouble(maximumBound.doubleValue() - minimumToUse) + minimumToUse;
                } else {
                    result = random.nextDouble() + minimumToUse;
                }
            }
            value = String.valueOf(result);
        } else if (OPERATION__TIMESTAMP.equalsIgnoreCase(operation)) {
            StringType format = getInputForName(input, INPUT__FORMAT, StringType.class);
            NumberType time = getInputForName(input, INPUT__TIME, NumberType.class);
            NumberType diff = getInputForName(input, INPUT__DIFF, NumberType.class);
            StringType zone = getInputForName(input, INPUT__ZONE, StringType.class);
            StringType inputDate = getInputForName(input, INPUT__DATE, StringType.class);
            StringType inputDateFormat = getInputForName(input, INPUT__INPUT_FORMAT, StringType.class);
            long epochMilliseconds;
            if (time == null) {
                if (inputDate == null) {
                    // UTC time in milliseconds by default.
                    epochMilliseconds = Instant.now().toEpochMilli();
                } else {
                    epochMilliseconds = parseDateStringAsMilliseconds((String) inputDate.getValue(), inputDateFormat == null?null: (String) inputDateFormat.getValue());
                }
            } else {
                epochMilliseconds = time.longValue();
            }
            if (diff != null) {
                epochMilliseconds += diff.longValue();
            }
            if (format == null) {
                value = String.valueOf(epochMilliseconds);
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern((String)format.getValue());
                Instant instant = Instant.ofEpochMilli(epochMilliseconds);
                ZoneId zoneId;
                if (zone == null) {
                    zoneId = DEFAULT_ZONE;
                } else {
                    zoneId = ZoneId.of((String)zone.getValue());
                }
                value = formatter.format(instant.atZone(zoneId));
            }
        } else if (OPERATION__STRING.equalsIgnoreCase(operation)) {
            StringType format = getInputForName(input, INPUT__FORMAT, StringType.class);
            if (format == null) {
                throw new IllegalArgumentException("Format to use for string generation is required");
            }
            try {
                var generator = new RgxGen((String)format.getValue());
                value = generator.generate();
            } catch (Exception e) {
                throw new IllegalArgumentException("Generation of string failed for expression ["+format.getValue()+"]", e);
            }
        } else {
            throw new IllegalArgumentException("Unknown operation ["+operation+"]");
        }
        ProcessingData data = new ProcessingData();
        data.getData().put(OUTPUT__VALUE, new StringType(value));
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private long parseDateStringAsMilliseconds(String dateString, String formatString) {
        DateTimeFormatter formatter;
        if (formatString == null) {
            formatter = new DateTimeFormatterBuilder().appendPattern("[dd][/][MM][/][yyyy]['T'[HH][:mm][:ss][.SSS][Z]]")
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(ChronoField.YEAR_OF_ERA, Instant.now().atZone(DEFAULT_ZONE).getYear())
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
                    .toFormatter();
        } else {
            formatter = new DateTimeFormatterBuilder().appendPattern(formatString)
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
                    .toFormatter();
        }
        if (formatter.getZone() == null) {
            formatter = formatter.withZone(DEFAULT_ZONE);
        }
        ZonedDateTime zonedResult;
        try {
            zonedResult = ZonedDateTime.parse(dateString, formatter);
        } catch (Exception e) {
            if (formatString == null) {
                throw new IllegalArgumentException("Unable to parse provided date string using default pattern (ISO 8061)");
            } else {
                throw new IllegalArgumentException("Unable to parse provided date string using provided pattern");
            }
        }
        return zonedResult.toInstant().toEpochMilli();
    }

}
