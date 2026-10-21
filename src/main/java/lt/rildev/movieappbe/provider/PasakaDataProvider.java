package lt.rildev.movieappbe.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.rildev.movieappbe.model.AgeRating;
import lt.rildev.movieappbe.model.Cinema;
import lt.rildev.movieappbe.model.Genre;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.model.Screening;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
public class PasakaDataProvider implements CinemaDataProvider {

    private static final String PROVIDER = "pasaka";
    private static final ZoneId PASAKA_ZONE = ZoneId.of("Europe/Vilnius");
    private static final DateTimeFormatter PASAKA_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Movie> movieDetailsCache = new ConcurrentHashMap<>();

    @Value("${url.api.cinemas.pasaka.films:https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&project_identifier=pasaka}")
    private String filmsUrl;

    @Value("${url.api.cinemas.pasaka.filters:https://api.pasaka.lt/filters?project_identifier=pasaka}")
    private String filtersUrl;

    public PasakaDataProvider(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<Cinema> getCinemas() {
        JsonNode theaters = dataFrom(get(filtersUrl)).path("theaters");
        List<Cinema> cinemas = new ArrayList<>();

        for (JsonNode theater : theaters) {
            cinemas.add(new Cinema(
                    text(theater, "id"),
                    text(theater, "name"),
                    text(theater, "address")
            ));
        }

        return cinemas;
    }

    @Override
    public List<Movie> getMovies(String cinemaId, Instant from, Instant to) {
        JsonNode movies = dataFrom(get(filmsUrl));
        Predicate<Screening> matchingScreening = screening -> matches(
                screening,
                cinemaId,
                from,
                to
        );

        List<Movie> result = new ArrayList<>();
        for (JsonNode movie : movies) {
            Movie mappedMovie = mapMovie(movie);
            Movie detailedMovie = getMovie(text(movie, "slug")).orElse(mappedMovie);
            List<Screening> screenings = mappedMovie.screenings().stream()
                    .filter(matchingScreening)
                    .toList();

            if (cinemaId == null || !screenings.isEmpty()) {
                result.add(withScreenings(detailedMovie, screenings));
            }
        }

        return result;
    }

    @Override
    public Optional<Movie> getMovie(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            return Optional.empty();
        }

        Movie cachedMovie = movieDetailsCache.get(movieId);
        if (cachedMovie != null) {
            return Optional.of(cachedMovie);
        }

        JsonNode movie = dataFrom(get(movieUrl(movieId)));
        if (movie.isMissingNode() || movie.isNull()) {
            return Optional.empty();
        }

        Movie mappedMovie = mapMovie(movie);
        movieDetailsCache.put(movieId, mappedMovie);
        return Optional.of(mappedMovie);
    }

    @Override
    public List<Screening> getScreenings(String cinemaId, Instant from, Instant to) {
        return getMovies(cinemaId, from, to).stream()
                .flatMap(movie -> movie.screenings().stream())
                .toList();
    }

    @Override
    public List<Screening> getMovieScreenings(
            String cinemaId,
            String movieId,
            Instant from,
            Instant to
    ) {
        return getMovie(movieId)
                .map(movie -> movie.screenings().stream()
                        .filter(screening -> matches(screening, cinemaId, from, to))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public List<Genre> getGenres() {
        JsonNode genres = dataFrom(get(filtersUrl)).path("genres");
        List<Genre> result = new ArrayList<>();

        for (JsonNode genre : genres) {
            result.add(new Genre(
                    text(genre, "name"),
                    text(genre, "slug")
            ));
        }

        return result;
    }

    private Movie mapMovie(JsonNode movie) {
        List<Genre> genres = new ArrayList<>();
        for (JsonNode genre : movie.path("genres")) {
            genres.add(new Genre(text(genre, "name"), text(genre, "slug")));
        }

        JsonNode rating = movie.path("mpaa_rating");
        JsonNode image = movie.path("image");
        String slug = text(movie, "slug");

        return new Movie(
                qualifiedId(slug),
                PROVIDER,
                text(movie, "name"),
                text(movie, "original_name"),
                slug,
                slug == null ? null : "https://pasaka.lt/filmas/" + slug,
                integer(movie, "runtime"),
                text(movie, "description"),
                firstText(movie, "synopsis", "short_description"),
                null,
                integer(movie, "release_year"),
                genres,
                new AgeRating(text(rating, "name"), text(rating, "description")),
                firstText(image, "poster", "thumb", "original"),
                text(movie, "trailer_link"),
                null,
                null,
                booleanValue(movie, "premiere"),
                mapScreenings(movie.path("events"), qualifiedId(slug))
        );
    }

    private List<Screening> mapScreenings(JsonNode events, String movieId) {
        if (!events.isArray()) {
            return Collections.emptyList();
        }

        List<Screening> screenings = new ArrayList<>();
        for (JsonNode event : events) {
            JsonNode theater = event.path("theater");
            screenings.add(new Screening(
                    text(event, "markus_link"),
                    movieId,
                    new Cinema(
                            text(theater, "id"),
                            text(theater, "name"),
                            text(theater, "address")
                    ),
                    parseDate(text(event.path("starts_at"), "full")),
                    parseDate(text(event.path("ends_at"), "full")),
                    null,
                    text(event, "markus_link"),
                    null,
                    null,
                    null,
                    null,
                    event.path("is_sold_out").asBoolean(false) ? "SOLD_OUT" : "AVAILABLE",
                    booleanValue(event, "is_sold_out"),
                    booleanValue(event, "is_cancelled")
            ));
        }

        return screenings;
    }

    private boolean matches(Screening screening, String cinemaId, Instant from, Instant to) {
        if (cinemaId != null && !cinemaId.equals(screening.cinema().id())) {
            return false;
        }
        if (from != null && screening.startsAt() != null && screening.startsAt().isBefore(from)) {
            return false;
        }
        return to == null || screening.startsAt() == null || screening.startsAt().isBefore(to);
    }

    private Movie withScreenings(Movie movie, List<Screening> screenings) {
        return new Movie(
                movie.id(), movie.provider(), movie.title(), movie.originalTitle(), movie.slug(),
                movie.detailUrl(), movie.durationMinutes(), movie.description(), movie.shortDescription(),
                movie.releaseDate(), movie.releaseYear(), movie.genres(), movie.ageRating(), movie.posterUrl(),
                movie.externalTrailerUrl(), movie.trailerPath(), movie.downloadedPosterPath(),
                movie.premiere(), screenings
        );
    }

    private JsonNode dataFrom(JsonNode response) {
        return response.path("data");
    }

    private JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Pasaka API returned HTTP status " + response.statusCode()
                );
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read Pasaka API response", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pasaka API request was interrupted", exception);
        }
    }

    private String movieUrl(String movieId) {
        String baseUrl = filmsUrl.substring(0, filmsUrl.indexOf('?'));
        return baseUrl + "/" + movieId + filmsUrl.substring(filmsUrl.indexOf('?'));
    }

    private String qualifiedId(String slug) {
        return slug == null ? null : PROVIDER + ":" + slug;
    }

    private Instant parseDate(String value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.parse(value, PASAKA_DATE_TIME)
                .atZone(PASAKA_ZONE)
                .toInstant();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isBoolean() ? value.asBoolean() : null;
    }
}
