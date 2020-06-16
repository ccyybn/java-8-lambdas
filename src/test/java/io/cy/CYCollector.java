package io.cy;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Slf4j
public class CYCollector implements Collector<String, StringBuilder, String> {
    static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();

    @Override
    public Supplier<StringBuilder> supplier() {
//        log.debug("supplier");
        return StringBuilder::new;
    }

    @Override
    public BiConsumer<StringBuilder, String> accumulator() {
        return (a, b) -> {
            log.debug("accumulator");
            a.append(b).append(",");
        };
    }

    @Override
    public BinaryOperator<StringBuilder> combiner() {
        return (a, b) -> {
            log.debug("combiner");
            return  a.append(b).append("-");
        };
    }

    @Override
    public Function<StringBuilder, String> finisher() {
        return StringBuilder::toString;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CH_NOID;
    }
}
