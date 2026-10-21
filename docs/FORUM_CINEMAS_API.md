# Forum Cinemas API

Forum Cinemas uses a web-component frontend backed by an internal REST API.
The API configuration is published at:

```http
GET https://forumcinemas.lt/app/themes/forum/assets/config.json
```

The configuration currently identifies the REST API base URL as:

```text
https://restapi.forumcinemas.lt/api
```

## Cinemas

```http
GET https://restapi.forumcinemas.lt/api/cinema
```

This returns cinema records and UUIDs. The response currently includes Vingis,
Kaunas, Klaipėda, and Šiauliai locations.

Regions are available from:

```http
GET https://restapi.forumcinemas.lt/api/region
```

## Movies

Get the movie catalog:

```http
GET https://restapi.forumcinemas.lt/api/movie
```

Get one movie by its UUID:

```http
GET https://restapi.forumcinemas.lt/api/movie/{movieId}
```

Movie records contain fields such as title, duration, description, age rating,
genres, posters, trailers, premiere date, country, director, and cast.

## Showtimes

Get showtimes for a cinema and date range:

```http
GET https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/screening?dateTimeFrom={start}&dateTimeTo={end}
```

Example:

```http
GET https://restapi.forumcinemas.lt/api/cinema/f933f355-2e6b-466f-9b97-e267ecd8e266/screening?dateTimeFrom=2026-10-20T00:00:00%2B03:00&dateTimeTo=2026-10-21T00:00:00%2B03:00
```

The screening response includes the movie ID, cinema ID, screen ID, movie-print
ID, start/end times, language/format information, availability, and booking
identifiers.

Get screenings for one movie at one cinema:

```http
GET https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/movie/{movieId}/screening?dateTimeFrom={start}&dateTimeTo={end}
```

Get one screening:

```http
GET https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/screening/{screeningId}
```

## Supporting endpoints

```http
GET https://restapi.forumcinemas.lt/api/genres
GET https://restapi.forumcinemas.lt/api/movieprint
GET https://restapi.forumcinemas.lt/api/movieprint/{moviePrintId}
GET https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/event
```

`/movieprint` can be queried with `cinemaId`, `dateTimeFrom`, and `dateTimeTo`.

## Application configuration

```yaml
url:
  api:
    cinemas:
      forum:
        cinemas: "https://restapi.forumcinemas.lt/api/cinema"
        regions: "https://restapi.forumcinemas.lt/api/region"
        films: "https://restapi.forumcinemas.lt/api/movie"
        genres: "https://restapi.forumcinemas.lt/api/genres"
        schedule: "https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/screening"
```

The endpoints are internal application APIs rather than a documented public
API, so response fields and paths may change without notice.
