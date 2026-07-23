package com.yuka.ailearningserver.flashcard;

import com.yuka.ailearningserver.flashcard.review.Fsrs6Scheduler;
import com.yuka.ailearningserver.flashcard.review.ReviewParameters;
import com.yuka.ailearningserver.flashcard.review.ReviewScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the flashcard module's spaced-repetition engine.
 *
 * <p>The scheduler in {@code flashcard.review} is a deliberately pure,
 * framework-free island (Step 1) — it carries no Spring annotations so it can be
 * unit-tested and reasoned about in isolation. This is the single place that
 * lifts it into the container, injecting the published FSRS-6 defaults. Swapping
 * the algorithm (a re-tuned variant, per-user optimized weights) is a change of
 * this one bean, invisible to every {@link ReviewScheduler} consumer.
 */
@Configuration
public class FlashcardConfig {

    @Bean
    public ReviewScheduler reviewScheduler() {
        return new Fsrs6Scheduler(ReviewParameters.defaults());
    }
}
