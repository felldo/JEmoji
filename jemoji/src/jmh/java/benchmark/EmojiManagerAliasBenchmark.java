package benchmark;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
/*
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EmojiManagerAliasBenchmark {

    @Param({":+1:", "nope"})
    private String alias;

    @Benchmark
    public Optional<List<Emoji>> getByAlias() {
        return EmojiManager.getByAlias(alias);
    }
}*/