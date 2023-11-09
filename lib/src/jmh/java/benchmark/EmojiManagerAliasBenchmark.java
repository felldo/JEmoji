package benchmark;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Optional;

@State(Scope.Benchmark)
public class EmojiManagerAliasBenchmark {

    @Param({":+1:", "nope"})
    private String alias;

    @Benchmark
    //@BenchmarkMode(Mode.AverageTime)
    //@Warmup(iterations = 1)
    public Optional<Emoji> getByAlias() {
        return EmojiManager.getByAlias(alias);
    }
}
