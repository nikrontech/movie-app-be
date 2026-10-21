# Pasaka API

The Pasaka website uses a Nuxt frontend and loads movie data from the Pasaka API.

## Base URL

```text
https://api.pasaka.lt
```

## Movie list

```http
GET https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&project_identifier=pasaka
```

## Movies currently showing

```http
GET https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&schedule=now&project_identifier=pasaka
```

## Individual movie

Replace `{slug}` with the movie slug:

```http
GET https://api.pasaka.lt/movies/{slug}?include=mpaaRating,genres,collections&project_identifier=pasaka
```

Example:

```http
GET https://api.pasaka.lt/movies/protas-ir-jausmai?include=mpaaRating,genres,collections&project_identifier=pasaka
```

## Filters

```http
GET https://api.pasaka.lt/filters?project_identifier=pasaka
```

Supported movie query filters include `languages`, `genres`, `theaters`, `schedule`,
`theaterHall`, `date`, `collection`, and `page`.

## Application configuration

```yaml
url:
  api:
    cinemas:
      pasaka:
        schedule: "https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&schedule=now&project_identifier=pasaka"
        film: "https://api.pasaka.lt/movies/{slug}?include=mpaaRating,genres,collections&project_identifier=pasaka"
```

Movie images are served from URLs under `https://api.pasaka.lt/storage/...`.
