package lt.rildev.movieappbe.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.rildev.movieappbe.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ForumDataProvider implements CinemaDataProvider {

    private static final String PROVIDER = "forum";
    private static final String API_BASE_URL = "https://restapi.forumcinemas.lt/api";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${url.api.cinemas.forum.cinemas:https://restapi.forumcinemas.lt/api/cinema}")
    private String cinemasUrl;

    @Value("${url.api.cinemas.forum.movies:https://restapi.forumcinemas.lt/api/movie}")
    private String moviesUrl;

    @Value("${url.api.cinemas.forum.genres:https://restapi.forumcinemas.lt/api/genres}")
    private String genresUrl;

    public ForumDataProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<Cinema> getCinemas() {
        List<Cinema> cinemas = new ArrayList<>();
        for (JsonNode cinema : get(cinemasUrl)) {
            cinemas.add(mapCinema(cinema));
        }
        return cinemas;
    }

    @Override
    public List<Movie> getMovies(String cinemaId, Instant from, Instant to) {
        Instant start = from == null ? Instant.now() : from;
        Instant end = to == null ? start.plusSeconds(30 * 24 * 60 * 60L) : to;
        List<Cinema> cinemas = cinemaId == null
                ? getCinemas()
                : getCinemas().stream().filter(cinema -> cinemaId.equals(cinema.id())).toList();

        List<Movie> movies = new ArrayList<>();
        for (JsonNode movieNode : get(moviesUrl)) {
            Movie movie = mapMovie(movieNode);
            List<Screening> screenings = cinemas.stream()
                    .flatMap(cinema -> getCinemaScreenings(cinema, start, end).stream())
                    .filter(screening -> movie.id().equals(screening.movieId()))
                    .toList();

            if (cinemaId == null || !screenings.isEmpty()) {
                movies.add(movie.toBuilder().screenings(screenings).build());
            }
        }
        return movies;
    }

    @Override
    public Optional<Movie> getMovie(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            return Optional.empty();
        }

        JsonNode movie = get(moviesUrl + "/" + sourceId(movieId));
        return movie.isMissingNode() || movie.isNull()
                ? Optional.empty()
                : Optional.of(mapMovie(movie));
    }

    @Override
    public List<Screening> getScreenings(String cinemaId, Instant from, Instant to) {
        Instant start = from == null ? Instant.now() : from;
        Instant end = to == null ? start.plusSeconds(30 * 24 * 60 * 60L) : to;
        Cinema cinema = getCinemas().stream()
                .filter(item -> cinemaId.equals(item.id()))
                .findFirst()
                .orElse(Cinema.builder().id(cinemaId).build());
        return getCinemaScreenings(cinema, start, end);
    }

    @Override
    public List<Screening> getMovieScreenings(
            String cinemaId,
            String movieId,
            Instant from,
            Instant to
    ) {
        Instant start = from == null ? Instant.now() : from;
        Instant end = to == null ? start.plusSeconds(30 * 24 * 60 * 60L) : to;
        String url = API_BASE_URL + "/cinema/" + cinemaId + "/movie/" + sourceId(movieId)
                + "/screening?dateTimeFrom=" + start + "&dateTimeTo=" + end;
        Cinema cinema = getCinemas().stream()
                .filter(item -> cinemaId.equals(item.id()))
                .findFirst()
                .orElse(Cinema.builder().id(cinemaId).build());
        return mapScreenings(get(url), cinema);
    }

    @Override
    public List<Genre> getGenres() {
        List<Genre> genres = new ArrayList<>();
        for (JsonNode genre : get(genresUrl)) {
            genres.add(Genre.builder()
                    .name(text(genre, "name"))
                    .slug(text(genre, "id"))
                    .build());
        }
        return genres;
    }

    private Movie mapMovie(JsonNode movie) {
        String sourceId = text(movie, "id");
        List<Genre> genres = new ArrayList<>();
        for (JsonNode genre : movie.path("genres")) {
            genres.add(Genre.builder()
                    .name(text(genre, "name"))
                    .slug(text(genre, "id"))
                    .build());
        }

        JsonNode rating = movie.path("ratings").isArray() && movie.path("ratings").size() > 0
                ? movie.path("ratings").get(0)
                : movie.path("ratings");

        return Movie.builder()
                .id(PROVIDER + ":" + sourceId)
                .provider(PROVIDER)
                .title(text(movie, "title"))
                .originalTitle(text(movie, "originalTitle"))
                .durationMinutes(integer(movie, "duration"))
                .description(text(movie, "description"))
                .shortDescription(text(movie, "shortDescription"))
                .director(text(movie, "director"))
                .cast(text(movie, "filmCast"))
                .country(text(movie, "country"))
                .language(firstText(movie, "originalLanguage", "originalTitleLanguage"))
                .imdbId(text(movie, "imdbId"))
                .releaseDate(parseDate(firstText(movie, "releaseDate", "premiereDate")))
                .releaseYear(year(firstText(movie, "yearOfProduction", "releaseDate", "premiereDate")))
                .genres(genres)
                .ageRating(AgeRating.builder()
                        .code(firstText(rating, "symbol", "value"))
                        .description(firstText(rating, "description", "value"))
                        .build())
                .externalPosterUrl(firstArrayText(movie, "posters"))
                .externalTrailerUrl(firstArrayText(movie, "trailers"))
                .premiere(movie.path("premiereDate").isTextual())
                .screenings(List.of())
                .build();
    }

    private List<Screening> getCinemaScreenings(Cinema cinema, Instant from, Instant to) {
        String url = API_BASE_URL + "/cinema/" + cinema.id() + "/screening"
                + "?dateTimeFrom=" + from + "&dateTimeTo=" + to;
        return mapScreenings(get(url), cinema);
    }

    private List<Screening> mapScreenings(JsonNode screenings, Cinema cinema) {
        List<Screening> result = new ArrayList<>();
        for (JsonNode screening : screenings) {
            String movieId = text(screening, "movieId");
            int availabilityStatus = screening.path("availabilityStatus").asInt(-1);
            result.add(Screening.builder()
                    .id(PROVIDER + ":" + text(screening, "id"))
                    .movieId(PROVIDER + ":" + movieId)
                    .cinema(cinema)
                    .startsAt(parseDate(text(screening, "screeningTimeFrom")))
                    .endsAt(parseDate(text(screening, "screeningTimeTo")))
                    .screen(text(screening, "screenId"))
                    .language(text(screening, "language"))
                    .subtitles(firstText(screening, "subtitles", "subtitles2"))
                    .format(firstText(screening, "format", "printType"))
                    .sound(text(screening, "soundType"))
                    .availability(String.valueOf(availabilityStatus))
                    .build());
        }
        return result;
    }

    private Cinema mapCinema(JsonNode cinema) {
        return Cinema.builder()
                .id(text(cinema, "id"))
                .name(text(cinema, "name"))
                .address(text(cinema, "street"))
                .city(lt.rildev.movieappbe.model.City.fromForumCinemaName(text(cinema, "name")))
                .build();
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
            throw new IllegalStateException("Could not read Forum Cinemas API response", exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not fetch Forum Cinemas API response", exception);
        }
    }

    private Instant parseDate(String value) {
        return value == null ? null : OffsetDateTime.parse(value).toInstant();
    }

    private Integer year(String value) {
        return value == null || value.length() < 4 ? null : Integer.valueOf(value.substring(0, 4));
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private String firstArrayText(JsonNode node, String field) {
        JsonNode values = node.path(field);
        return values.isArray() && !values.isEmpty() ? values.get(0).asText() : null;
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

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String sourceId(String id) {
        return id.startsWith(PROVIDER + ":") ? id.substring(PROVIDER.length() + 1) : id;
    }
}
