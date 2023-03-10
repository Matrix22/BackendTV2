package backendtv.parser;

import backendtv.server.ServerApp;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * <p>Utility class to parse the output into a Json file.</p>
 *
 * @since 1.0.0
 * @author Mihai Negru
 */
public final class JsonParser {
    private JsonParser() {
        // Do not let anyone instantiate this class
    }

    /**
     * <p>Parses the basic error message in the output node.</p>
     *
     * @param output main node to parse the error message.
     */
    public static void parseBasicError(final ObjectNode output) {
        output.put("error", "Error");
        output.putArray("currentMoviesList");
        output.putNull("currentUser");
    }

    /**
     * <p>Parses a {@code Client} object to a Json File.</p>
     *
     * @param output main node to parse the client object.
     */
    public static void parseClient(final ObjectNode output) {
        final var client = ServerApp.connect().fetchActiveClient();

        if (client.getStatus()) {
            final var outputObject = output.putObject("currentUser");
            final var credentialsOutput = outputObject.putObject("credentials");

            credentialsOutput.put("name", client.getName());
            credentialsOutput.put("password", client.getPassword());
            credentialsOutput.put("accountType", client.getAccountType());
            credentialsOutput.put("country", client.getCountry());
            credentialsOutput.put("balance", Integer.toString(client.getBalance()));

            outputObject.put("tokensCount", client.getTokensCount());
            outputObject.put("numFreePremiumMovies", client.getNumFreePremiumMovies());

            var tempArray = outputObject.putArray("purchasedMovies");
            for (String movieName : client.getPurchasedMovies()) {
                parseMovie(tempArray, "name", movieName);
            }

            tempArray = outputObject.putArray("watchedMovies");
            for (String movieName : client.getWatchedMovies()) {
                parseMovie(tempArray, "name", movieName);
            }

            tempArray = outputObject.putArray("likedMovies");
            for (String movieName : client.getLikedMovies()) {
                parseMovie(tempArray, "name", movieName);
            }

            tempArray = outputObject.putArray("ratedMovies");
            for (String movieName : client.getRatedMovies()) {
                parseMovie(tempArray, "name", movieName);
            }

            tempArray = outputObject.putArray("notifications");
            for (String notification : client.getNotifications()) {
                parseNotification(tempArray, notification);
            }
        } else {
            output.putNull("currentUser");
        }
    }

    /**
     * <p>Parses all the available movies of one client.</p>
     *
     * <p>The movies parsed does not depend on the
     * current page loaded for the client.</p>
     *
     * @param output main array node to parse movies.
     */
    public static void parseAvailableMovies(final ArrayNode output) {
        final var client = ServerApp.connect().fetchActiveClient();

        for (String movieName : client.getAvailableMovies()) {
            parseMovie(output, "name", movieName);
        }
    }

    /**
     * <p>Parses a movie to a Json File.</p>
     *
     * <p>The movie is loaded from the server's database
     * and does not depend on the current active client.</p>
     *
     * @param output main array node to parse the movie.
     * @param movieKey movie key to find in the database.
     * @param movieValue movie value to find in the database.
     */
    public static void parseMovie(final ArrayNode output, final String movieKey,
                                  final String movieValue) {
        final var movie = ServerApp.connect()
                .fetchDatabase()
                .collection("movies")
                .findOne(movieKey, movieValue);

        parseMovie(output, movie);
    }

    /**
     * <p>Parses a movie to a Json File.</p>
     *
     * <p>The movies is parsed depending on the {@code Map}
     * object specified at input.</p>
     *
     * @param output main array node to parse the movie.
     * @param movie data {@code Map} of the movie.
     */
    public static void parseMovie(final ArrayNode output, final Map<String, String> movie) {
        if (movie != null) {
            final var outputObject = output.addObject();

            outputObject.put("name", movie.get("name"));
            outputObject.put("year", movie.get("year"));
            outputObject.put("duration", Integer.parseInt(movie.get("duration")));

            var tempArray = outputObject.putArray("genres");
            if (!movie.get("genres").equals("null")) {
                for (String genre : movie.get("genres").split(",")) {
                    tempArray.add(genre);
                }
            }

            tempArray = outputObject.putArray("actors");
            if (!movie.get("actors").equals("null")) {
                for (String actor : movie.get("actors").split(",")) {
                    tempArray.add(actor);
                }
            }

            tempArray = outputObject.putArray("countriesBanned");
            if (!movie.get("countriesBanned").equals("null")) {
                for (String actor : movie.get("countriesBanned").split(",")) {
                    tempArray.add(actor);
                }
            }

            outputObject.put("numLikes", Integer.parseInt(movie.get("numLikes")));

            double rating = 0;
            for (String rate : movie.get("rating").split(",")) {
                rating += Double.parseDouble(rate.split(":")[1]);
            }

            final int numRatings = Integer.parseInt(movie.get("numRatings"));

            if (numRatings != 0) {
                rating /= numRatings;
            } else {
                rating = 0;
            }

            outputObject.put("rating", rating);
            outputObject.put("numRatings", numRatings);
        }
    }

    /**
     * <p>Parses a notification from the client's notifications queue.</p>
     *
     * @param output the main node to parse the notification.
     * @param notification the {@code String} representation for the notifications,
     *                     contains the message then separated by the comma contains
     *                     the actual notification.
     */
    public static void parseNotification(final ArrayNode output, final String notification) {
        final var notificationInfo = notification.split(";");

        if (notificationInfo.length == 2) {
            final var outputObject = output.addObject();
            outputObject.put("movieName", notificationInfo[0]);
            outputObject.put("message", notificationInfo[1]);
        }
    }
}
