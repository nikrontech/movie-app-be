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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VueDataProvider implements CinemaDataProvider {

    private static final String PROVIDER = "vue";
    private static final String API_BASE_URL = "https://vuekinas.lt";
    private static final ZoneId VILNIUS_ZONE = ZoneId.of("Europe/Vilnius");
    private static final DateTimeFormatter VUE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)\\s*min\\.", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${url.api.cinemas.vue.cinemas:https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetCinemas}")
    private String cinemasUrl;

    @Value("${url.api.cinemas.vue.filters:https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetFilters?cinemaId=1001}")
    private String filtersUrl;

    @Value("${url.api.cinemas.vue.films:https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Alphabetic?cinemaId=1001}")
    private String filmsUrl;

    public VueDataProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<Cinema> getCinemas() {
        JsonNode cinemas = get(cinemasUrl).path("whatsOnCinemas");
        List<Cinema> result = new ArrayList<>();
        for (JsonNode cinema : cinemas) {
            result.add(Cinema.builder()
                    .id(text(cinema, "CinemaId"))
                    .name(text(cinema, "CinemaName"))
                    .displayName("Vue")
                    .city(lt.rildev.movieappbe.model.City.VILNIUS)
                    .build());
        }
        return result;
    }

    @Override
    public List<Movie> getMovies(String cinemaId, Instant from, Instant to) {
        JsonNode movies = get(filmsUrl).path("WhatsOnAlphabeticFilms");
        Predicate<Screening> matchingScreening = screening -> matches(screening, cinemaId, from, to);
        List<Movie> result = new ArrayList<>();

        for (JsonNode movie : movies) {
            Movie mappedMovie = mapMovie(movie);
            List<Screening> screenings = mappedMovie.screenings().stream()
                    .filter(matchingScreening)
                    .toList();
            if (cinemaId == null || !screenings.isEmpty()) {
                result.add(mappedMovie.toBuilder().screenings(screenings).build());
            }
        }
        return result;
    }

    @Override
    public Optional<Movie> getMovie(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            return Optional.empty();
        }

        return getMovies(null, null, null).stream()
                .filter(movie -> movieId.equals(movie.id())
                        || movieId.equals(movie.slug())
                        || movieId.equals(filmId(movie.id())))
                .findFirst();
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
        List<Genre> result = new ArrayList<>();
        collectGenres(get(filtersUrl), result);
        return result.stream().distinct().toList();
    }

    private Movie mapMovie(JsonNode movie) {
        String filmId = text(movie, "FilmId");
        String filmUrl = text(movie, "FilmUrl");
        String slug = slug(filmUrl);
        List<Genre> genres = mapGenres(movie.path("FilmParams"));
        String age = text(movie, "CertificateAge");

        if (age == null) {
            age = parameterContaining(movie.path("FilmParams"), "cenz");
        }

        return Movie.builder()
                .id(PROVIDER + ":" + filmId)
                .provider(PROVIDER)
                .title(text(movie, "Title"))
                .slug(slug)
                .detailUrl(filmUrl == null ? null : API_BASE_URL + filmUrl)
                .durationMinutes(parseMinutes(movie.path("FilmParams")))
                .description(text(movie, "Synopsis"))
                .shortDescription(text(movie, "ShortSynopsis"))
                .releaseDate(parseDate(text(movie, "ReleaseDate")))
                .releaseYear(year(text(movie, "ReleaseDate")))
                .genres(genres)
                .ageRating(AgeRating.builder().code(age).build())
                .externalPosterUrl(text(movie, "Poster"))
                .externalTrailerUrl(text(movie, "TrailerUrl"))
                .premiere(hasPremiereLabel(movie.path("PromoLabels")))
                .screenings(mapScreenings(movie, PROVIDER + ":" + filmId))
                .build();
    }

    private List<Screening> mapScreenings(JsonNode movie, String movieId) {
        List<Screening> result = new ArrayList<>();
        JsonNode cinemas = movie.path("WhatsOnAlphabeticCinemas");

        for (JsonNode cinemaGroup : cinemas) {
            JsonNode cinema = firstArrayValue(cinemaGroup.path("WhatsOnAlphabeticCinemas"));
            if (cinema.isMissingNode()) {
                cinema = cinemaGroup;
            }

            for (JsonNode schedule : cinema.path("WhatsOnAlphabeticShedules")) {
                String time = text(schedule, "Time");
                result.add(Screening.builder()
                        .id(bookingId(text(schedule, "BookingLink")))
                        .movieId(movieId)
                        .cinema(Cinema.builder()
                                .id(text(cinema, "CinemaId"))
                                .name(text(cinema, "CinemaName"))
                                .displayName("Vue")
                                .city(lt.rildev.movieappbe.model.City.VILNIUS)
                                .build())
                        .startsAt(parseDate(time))
                        .endsAt(endsAt(time, movie))
                        .screen(text(schedule, "ScreenNumber"))
                        .bookingUrl(absoluteUrl(text(schedule, "BookingLink")))
                        .format(text(schedule, "VersionTitle"))
                        .availability("AVAILABLE")
                        .build());
            }
        }
        return result;
    }

    private List<Genre> mapGenres(JsonNode parameters) {
        List<Genre> genres = new ArrayList<>();
        for (JsonNode parameter : parameters) {
            String link = text(parameter, "Link");
            String title = text(parameter, "Title");
            if (link != null && link.startsWith("/rusis/") && !title.toLowerCase(Locale.ROOT).contains("cenz")) {
                genres.add(Genre.builder().name(title).slug(slug(link)).build());
            }
        }
        return genres;
    }

    private void collectGenres(JsonNode node, List<Genre> genres) {
        if (node.isObject()) {
            String title = text(node, "Title");
            String slug = text(node, "Slug");
            if (title != null && slug != null && node.toString().toLowerCase(Locale.ROOT).contains("genre")) {
                genres.add(Genre.builder().name(title).slug(slug).build());
            }
            node.elements().forEachRemaining(child -> collectGenres(child, genres));
        } else if (node.isArray()) {
            node.forEach(child -> collectGenres(child, genres));
        }
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

    private JsonNode get(String url) {
        try {
            String body = restClient.get()
                    .uri(url)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read VUE API response", exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not fetch VUE API response", exception);
        }
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, VUE_DATE_TIME).atZone(VILNIUS_ZONE).toInstant();
        } catch (RuntimeException ignored) {
            return Instant.parse(value);
        }
    }

    private Instant endsAt(String start, JsonNode movie) {
        Instant startsAt = parseDate(start);
        Integer duration = parseMinutes(movie.path("FilmParams"));
        return startsAt == null || duration == null ? null : startsAt.plusSeconds(duration * 60L);
    }

    private Integer parseMinutes(JsonNode parameters) {
        for (JsonNode parameter : parameters) {
            Matcher matcher = MINUTES_PATTERN.matcher(text(parameter, "Title") == null ? "" : text(parameter, "Title"));
            if (matcher.find()) {
                return Integer.valueOf(matcher.group(1));
            }
        }
        return null;
    }

    private Integer year(String value) {
        if (value == null || value.length() < 4) {
            return null;
        }
        return Integer.valueOf(value.substring(0, 4));
    }

    private boolean hasPremiereLabel(JsonNode labels) {
        return labels.path("names").toString().toLowerCase(Locale.ROOT).contains("premjera");
    }

    private String parameterContaining(JsonNode parameters, String value) {
        for (JsonNode parameter : parameters) {
            String title = text(parameter, "Title");
            if (title != null && title.toLowerCase(Locale.ROOT).contains(value)) {
                return title;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String slug(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String normalized = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String absoluteUrl(String url) {
        return url == null ? null : (url.startsWith("http") ? url : API_BASE_URL + url);
    }

    private String bookingId(String bookingLink) {
        if (bookingLink == null) {
            return null;
        }
        String normalized = bookingLink.endsWith("/")
                ? bookingLink.substring(0, bookingLink.length() - 1)
                : bookingLink;
        return PROVIDER + ":" + normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String filmId(String id) {
        return id != null && id.startsWith(PROVIDER + ":") ? id.substring(PROVIDER.length() + 1) : id;
    }

    private JsonNode firstArrayValue(JsonNode node) {
        return node.isArray() && !node.isEmpty() ? node.get(0) : node;
    }
}
