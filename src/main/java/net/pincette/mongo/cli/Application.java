package net.pincette.mongo.cli;

import static java.lang.System.exit;
import static java.util.Arrays.stream;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static net.pincette.json.JsonUtil.string;
import static net.pincette.mongo.Match.predicate;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetWithRethrow;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import net.pincette.json.JsonStructureIterator;
import net.pincette.json.JsonUtil;
import net.pincette.mongo.Validator;
import net.pincette.util.ArgsBuilder;
import net.pincette.util.StreamUtil;

/**
 * A command line tool for pincette-mongo. The supported commands are "match" and "validate".
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class Application {
  private static final String MATCH = "match";
  private static final String QUERY_FILE = "query";
  private static final String QUERY_FILE_OPT_LONG = "--query-file";
  private static final String QUERY_FILE_OPT_SHORT = "-q";
  private static final String SPEC_FILE = "spec";
  private static final String SPEC_FILE_OPT_LONG = "--specification-file";
  private static final String SPEC_FILE_OPT_SHORT = "-s";
  private static final String TRACE = "trace";
  private static final String TRACE_OPT_LONG = "--trace";
  private static final String TRACE_OPT_SHORT = "-t";
  private static final String VALIDATE = "validate";

  private static boolean isMatch(final Map<String, String> options) {
    return options.containsKey(MATCH) && options.containsKey(QUERY_FILE);
  }

  private static boolean isValidate(final Map<String, String> options) {
    return options.containsKey(VALIDATE) && options.containsKey(SPEC_FILE);
  }

  @SuppressWarnings("squid:S106") // Not logging.
  public static void main(final String[] args) {
    stream(args)
        .reduce(new ArgsBuilder(), Application::options, (b1, b2) -> b1)
        .build()
        .filter(options -> isMatch(options) || isValidate(options))
        .map(
            options ->
                options.containsKey(MATCH)
                    ? match(options, System.in, System.out)
                    : validate(options, System.in, System.out))
        .orElse(Application::usage)
        .run();
  }

  private static Runnable match(
      final Map<String, String> options, final InputStream in, final OutputStream out) {
    return () -> {
      trace(options);

      final Predicate<JsonObject> predicate = predicate(read(options.get(QUERY_FILE)));

      write(objectStream(in).filter(predicate), out);
    };
  }

  private static Stream<JsonObject> objectStream(final InputStream in) {
    return StreamUtil.stream(new JsonStructureIterator(in))
        .filter(JsonUtil::isObject)
        .map(JsonValue::asJsonObject);
  }

  private static ArgsBuilder options(final ArgsBuilder builder, final String arg) {
    switch (arg) {
      case QUERY_FILE_OPT_LONG:
      case QUERY_FILE_OPT_SHORT:
        return builder.addPending(QUERY_FILE);
      case SPEC_FILE_OPT_LONG:
      case SPEC_FILE_OPT_SHORT:
        return builder.addPending(SPEC_FILE);
      case TRACE_OPT_LONG:
      case TRACE_OPT_SHORT:
        return builder.add(TRACE);
      default:
        return builder.add(arg);
    }
  }

  private static JsonObject read(final String filename) {
    return tryToGetWithRethrow(
            () -> createReader(tryToGetRethrow(() -> new FileInputStream(filename)).orElse(null)),
            JsonReader::readObject)
        .orElse(null);
  }

  @SuppressWarnings("squid:S106") // This is part of logging.
  private static void trace(final Map<String, String> options) {
    if (options.containsKey(TRACE)) {
      final Logger logger = getLogger("net.pincette.mongo.expressions");
      final Handler handler =
          new StreamHandler(
              System.err,
              new Formatter() {
                public String format(final LogRecord record) {
                  return record.getMessage();
                }
              });

      logger.setLevel(FINEST);
      handler.setLevel(FINEST);
      logger.addHandler(handler);
    }
  }

  @SuppressWarnings("squid:S106") // Not logging.
  private static void usage() {
    System.err.println(
        "Usage: net.pincette.mongo,cli.Application\n"
            + "  match\n"
            + "    (-q | --query-file) query_file |\n"
            + "  validate\n"
            + "    (-s | --specification-file) specification_file\n"
            + "  [-t | --trace]");
    exit(1);
  }

  private static Runnable validate(
      final Map<String, String> options, final InputStream in, final OutputStream out) {
    return () -> {
      trace(options);

      final Function<JsonObject, JsonArray> validator =
          new Validator().validator(options.get(SPEC_FILE));

      write(objectStream(in).map(json -> validate(json, validator)), out);
    };
  }

  private static JsonObject validate(
      final JsonObject json, final Function<JsonObject, JsonArray> validator) {
    final JsonArray result = validator.apply(json);

    return result.isEmpty() ? json : createObjectBuilder().add("errors", result).build();
  }

  private static void write(final Stream<JsonObject> objects, final OutputStream out) {
    final PrintWriter writer = new PrintWriter(out);

    objects.forEach(
        json -> {
          writer.println(string(json));
          writer.flush();
        });
  }
}
