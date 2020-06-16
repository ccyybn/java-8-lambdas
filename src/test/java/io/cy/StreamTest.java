package io.cy;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.*;

@Slf4j
public class StreamTest {

    @Test
    public void testStreamOf() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        Assert.assertEquals(ImmutableList.of(1, 2, 3), list);
        list.add(4);
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4), list);

        Set<Integer> set = Stream.of(1, 2, 2, 3).collect(Collectors.toSet());
        Assert.assertEquals(ImmutableSet.of(1, 2, 3), set);
    }

    @Test
    public void testMaxMin() {
        Optional<Integer> min = ImmutableList.of(1, 1, 2, 3, 4).stream().min(Comparator.naturalOrder());
        Assert.assertTrue(min.isPresent());
        Assert.assertEquals(min.get(), Integer.valueOf(1));
    }

    @Test
    public void testReduce() {
        Integer reduce = ImmutableList.of(1, 2, 3, 4).stream().reduce(100, (result, item) -> result * item);
        Assert.assertEquals(reduce, Integer.valueOf(2400));
    }

    @Test
    public void testFlatMap() {
        ImmutableList<ImmutableList<Integer>> of = ImmutableList.of(ImmutableList.of(1, 2, 3), ImmutableList.of(1, 2, 3));
        List<Integer> collect = of.stream().flatMap(Collection::stream).collect(Collectors.toList());
        Assert.assertEquals(collect, ImmutableList.of(1, 2, 3, 1, 2, 3));

        List<Map<Integer, List<Integer>>> listMap = ImmutableList.of(ImmutableMap.of(1, ImmutableList.of(1, 2), 2, ImmutableList.of(1, 2)), ImmutableMap.of(1, ImmutableList.of(3, 4)));
        List<Integer> collect1 = listMap.stream().flatMap(a -> a.values().stream()).flatMap(Collection::stream).collect(Collectors.toList());
        Assert.assertEquals(collect1, ImmutableList.of(1, 2, 1, 2, 3, 4));

    }

    @Test
    public void testSort() {
        List<Integer> collect = ImmutableList.of(4, 5, 3, 1, 2).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(collect, ImmutableList.of(1, 2, 3, 4, 5));

        collect = ImmutableList.of(1, 2, 3, 4, 5).stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        Assert.assertEquals(collect, ImmutableList.of(5, 4, 3, 2, 1));
    }

    @Test
    public void testIntStream() {
        IntSummaryStatistics intSummaryStatistics = ImmutableList.of(1, 2, 3, 4).stream().mapToInt(a -> a).summaryStatistics();
        Assert.assertEquals(Double.valueOf(intSummaryStatistics.getAverage()), Double.valueOf(2.5D));
        Assert.assertEquals(Double.valueOf(intSummaryStatistics.getMax()), Double.valueOf(4));
        Assert.assertEquals(Double.valueOf(intSummaryStatistics.getMin()), Double.valueOf(1));
        Assert.assertEquals(Double.valueOf(intSummaryStatistics.getCount()), Double.valueOf(4));
        Assert.assertEquals(Double.valueOf(intSummaryStatistics.getSum()), Double.valueOf(10));
    }

    @Test
    public void testAnyAllMatch() {
        ImmutableList<String> containOf = ImmutableList.of("1", "2", "3", "4");
        ImmutableList<String> containEd = ImmutableList.of("4", "5", "6", "7");
        Assert.assertTrue(containEd.stream().anyMatch(containOf::contains));
        Assert.assertFalse(containEd.stream().allMatch(containOf::contains));
    }

    @Test
    public void testOverLoad() {
        Assert.assertEquals(overloadMethod((x, y) -> x), "BinaryOperator");
    }

    @Test
    public void testAutoCloseable() {
        try (AutoCloseable ignored = () -> log.debug("close")) {
            log.debug("testAutoCloseable ...");
        } catch (Exception e) {
            log.error("testAutoCloseable", e);
        }
    }

    @Test
    public void testPartitioningBy() {
        Map<Boolean, List<Integer>> collect = ImmutableList.of(1, 2, 3, 4, 5).stream().collect(Collectors.partitioningBy(a -> a > 2));
        Assert.assertEquals(ImmutableMap.of(false, ImmutableList.of(1, 2), true, ImmutableList.of(3, 4, 5)), collect);
    }


    @Test
    public void testGroupingBy() {
        List<Number> doubleList = ImmutableList.of(1.5, 2, 3, 4, 5);
        Double d = doubleList.parallelStream().collect(Collectors.averagingDouble(Number::doubleValue));
        Assert.assertEquals(Double.valueOf(3.1), d);

        Map<Boolean, Double> collect = ImmutableList.of(1, 2, 3, 4, 5).parallelStream().collect(Collectors.groupingBy(a -> a > 2, Collectors.averagingInt(a -> a)));
        Assert.assertEquals(ImmutableMap.of(false, 1.5D, true, 4D), collect);
    }

    @Test
    public void testMyCollector() {
        List<Integer> of = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            of.add(i);
        }
        String collect = of.parallelStream().map(String::valueOf).collect(new CYCollector());
        Assert.assertTrue(collect.contains(","));
        Assert.assertTrue(collect.contains("-"));
        log.debug(collect);
    }

    @Test
    public void testGroupingByCounting() {
        Stream<String> test = Stream.of("John", "Paul", "George", "John", "Paul", "John");
        Map<String, Long> collect = test.collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        Assert.assertEquals(collect.get("John"), Long.valueOf(3));
        Assert.assertEquals(collect.get("Paul"), Long.valueOf(2));
        Assert.assertEquals(collect.get("George"), Long.valueOf(1));
        Assert.assertEquals(collect.size(), 3);
    }

    @Test
    public void testStreamSupport() throws ExecutionException, InterruptedException {
        ForkJoinPool forkJoinPool = new ForkJoinPool(100);
        ImmutableList<Integer> of = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        forkJoinPool.submit(() -> {
            Stream<Integer> stream = StreamSupport.stream(Spliterators.spliterator(of.iterator(), 5, 0), true);
            stream.forEach(a -> log.debug(String.valueOf(a)));
        }).get();


    }


    private String overloadMethod(BinaryOperator lambda) {
        return "BinaryOperator";
    }

    private String overloadMethod(BiFunction lambda) {
        return "BiFunction";
    }

}
