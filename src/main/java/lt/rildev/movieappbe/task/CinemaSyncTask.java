package lt.rildev.movieappbe.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CinemaSyncTask {

    private static final Logger log = LoggerFactory.getLogger(CinemaSyncTask.class);

    private final PasakaSyncTask pasakaSyncTask;
    private final VueSyncTask vueSyncTask;
    private final ForumSyncTask forumSyncTask;
    private final MovieMergeTask movieMergeTask;
    private final boolean pasakaEnabled;
    private final boolean vueEnabled;
    private final boolean forumEnabled;
    private final boolean mergeEnabled;

    public CinemaSyncTask(
            PasakaSyncTask pasakaSyncTask,
            VueSyncTask vueSyncTask,
            ForumSyncTask forumSyncTask,
            MovieMergeTask movieMergeTask,
            @Value("${cinema.sync.pasaka.enabled:false}") boolean pasakaEnabled,
            @Value("${cinema.sync.vue.enabled:false}") boolean vueEnabled,
            @Value("${cinema.sync.forum.enabled:false}") boolean forumEnabled,
            @Value("${cinema.merge.enabled:false}") boolean mergeEnabled
    ) {
        this.pasakaSyncTask = pasakaSyncTask;
        this.vueSyncTask = vueSyncTask;
        this.forumSyncTask = forumSyncTask;
        this.movieMergeTask = movieMergeTask;
        this.pasakaEnabled = pasakaEnabled;
        this.vueEnabled = vueEnabled;
        this.forumEnabled = forumEnabled;
        this.mergeEnabled = mergeEnabled;
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.fixed-delay-ms:3600000}"
    )
    public void synchronize() {
        if (pasakaEnabled) {
            pasakaSyncTask.synchronizePasaka();
        }
        if (vueEnabled) {
            vueSyncTask.synchronizeVue();
        }
        if (forumEnabled) {
            forumSyncTask.synchronizeForum();
        }
        if (mergeEnabled) {
            movieMergeTask.mergeMovies();
        }

        log.debug(
                "Cinema synchronization completed: Pasaka={}, VUE={}, Forum={}, Merge={}",
                pasakaEnabled,
                vueEnabled,
                forumEnabled,
                mergeEnabled
        );
    }
}
