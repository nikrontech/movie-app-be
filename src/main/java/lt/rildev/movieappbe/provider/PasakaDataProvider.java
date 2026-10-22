package lt.rildev.movieappbe.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.rildev.movieappbe.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
public class PasakaDataProvider implements CinemaDataProvider {

    private static final String PROVIDER = "pasaka";
    private static final ZoneId PASAKA_ZONE = ZoneId.of("Europe/Vilnius");
    private static final DateTimeFormatter PASAKA_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Movie> movieDetailsCache = new ConcurrentHashMap<>();

    @Value("${url.api.cinemas.pasaka.films:https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&project_identifier=pasaka}")
    private String filmsUrl;

    @Value("${url.api.cinemas.pasaka.filters:https://api.pasaka.lt/filters?project_identifier=pasaka}")
    private String filtersUrl;

    public PasakaDataProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
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
                    text(theater, "name"),
                    text(theater, "address"),
                    lt.rildev.movieappbe.model.City.VILNIUS
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
            result.add(Genre.builder()
                    .name(text(genre, "name"))
                    .slug(text(genre, "slug"))
                    .build());
        }

        return result;
    }

    private Movie mapMovie(JsonNode movie) {
        List<Genre> genres = new ArrayList<>();
        for (JsonNode genre : movie.path("genres")) {
            genres.add(Genre.builder()
                    .name(text(genre, "name"))
                    .slug(text(genre, "slug"))
                    .build());
        }

        JsonNode rating = movie.path("mpaa_rating");
        JsonNode image = movie.path("image");
        String slug = text(movie, "slug");

        return Movie.builder()
                .id(qualifiedId(slug))
                .provider(PROVIDER)
                .title(text(movie, "name"))
                .originalTitle(text(movie, "original_name"))
                .slug(slug)
                .detailUrl(slug == null ? null : "https://pasaka.lt/filmas/" + slug)
                .durationMinutes(integer(movie, "runtime"))
                .description(text(movie, "description"))
                .shortDescription(firstText(movie, "synopsis", "short_description"))
                .releaseYear(integer(movie, "release_year"))
                .genres(genres)
                .ageRating(AgeRating.builder()
                        .code(text(rating, "name"))
                        .description(text(rating, "description"))
                        .build())
                .externalPosterUrl(firstText(image, "thumb", "poster", "original"))
                .externalThumbnailUrl(firstText(image, "poster", "thumb", "original"))
                .externalTrailerUrl(text(movie, "trailer_link"))
                .screenings(mapScreenings(movie.path("events"), qualifiedId(slug)))
                .premiere(booleanValue(movie, "premiere"))
                .build();
    }

    private List<Screening> mapScreenings(JsonNode events, String movieId) {
        if (!events.isArray()) {
            return Collections.emptyList();
        }

        List<Screening> screenings = new ArrayList<>();
        for (JsonNode event : events) {
            JsonNode theater = event.path("theater");
            screenings.add(Screening.builder()
                    .id(text(event, "markus_link"))
                    .movieId(movieId)
                    .cinema(Cinema.builder()
                            .id(text(theater, "id"))
                            .name(text(theater, "name"))
                            .displayName(text(theater, "name"))
                            .address(text(theater, "address"))
                            .city(lt.rildev.movieappbe.model.City.VILNIUS)
                            .build())
                    .startsAt(parseDate(text(event.path("starts_at"), "full")))
                    .endsAt(parseDate(text(event.path("ends_at"), "full")))
                    .bookingUrl(text(event, "markus_link"))
                    .availability(event.path("is_sold_out").asBoolean(false) ? "SOLD_OUT" : "AVAILABLE")
                    .soldOut(booleanValue(event, "is_sold_out"))
                    .cancelled(booleanValue(event, "is_cancelled"))
                    .build());
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
        return movie.toBuilder()
                .screenings(screenings)
                .build();
    }

    private JsonNode dataFrom(JsonNode response) {
        return response.path("data");
    }

    private JsonNode get(String url) {
        try {
            String body = restClient.get()
                    .uri(url)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read Pasaka API response", exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not fetch Pasaka API response", exception);
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
