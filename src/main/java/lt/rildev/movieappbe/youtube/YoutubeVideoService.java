package lt.rildev.movieappbe.youtube;

import java.nio.file.Path;

public interface YoutubeVideoService {

    Path download(String videoUrl);
}
