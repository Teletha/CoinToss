/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import static cointoss.ticker.TickSpan.*;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import cointoss.ticker.Tick;
import cointoss.ticker.TickSpan;
import cointoss.util.Span;
import kiss.I;
import kiss.Signal;

/**
 * @version 2017/09/08 18:20:48
 */
public abstract class MarketLog {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<TickSpan, CacheWriter> writers = new ConcurrentHashMap();

    /**
     * Get the starting day of cache.
     * 
     * @return
     */
    public abstract ZonedDateTime getCacheStart();

    /**
     * Get the ending day of cache.
     * 
     * @return
     */
    public abstract ZonedDateTime getCacheEnd();

    /**
     * Locate cache directory.
     * 
     * @return
     */
    public abstract Path cacheRoot();

    /**
     * Read date from the specified date.
     * 
     * @param start
     * @return
     */
    public abstract Signal<Execution> from(ZonedDateTime start);

    /**
     * Read date from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromToday() {
        return from(ZonedDateTime.now());
    }

    /**
     * Read date from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromYestaday() {
        return fromLast(1);
    }

    /**
     * Read date from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromLast(int days) {
        return fromLast(days, ChronoUnit.DAYS);
    }

    /**
     * Read date from the specified date.
     * 
     * @param time A duration.
     * @param unit A duration unit.
     * @return
     */
    public final Signal<Execution> fromLast(int time, ChronoUnit unit) {
        return from(ZonedDateTime.now(Execution.UTC).minus(time, unit));
    }

    /**
     * Read date from the specified start to end.
     * 
     * @param start
     * @param end
     * @return
     */
    public final Signal<Execution> rangeAll() {
        return range(getCacheStart(), getCacheEnd());
    }

    /**
     * Read date from the specified start to end.
     * 
     * @param start
     * @param end
     * @return
     */
    public final Signal<Execution> range(Span span) {
        return range(span.start, span.end);
    }

    /**
     * Read date from the specified start to end.
     * 
     * @param start
     * @param end
     * @return
     */
    public final Signal<Execution> range(ZonedDateTime start, ZonedDateTime end) {
        if (start.isBefore(end)) {
            return from(start).takeWhile(e -> e.exec_date.isBefore(end));
        } else {
            return Signal.EMPTY;
        }
    }

    /**
     * Read date from the specified start to end.
     * 
     * @param days
     * @return
     */
    public final Signal<Execution> rangeRandom(int days) {
        return range(Span.random(getCacheStart(), getCacheEnd().minusDays(1), days));
    }

    private void smaple() {
        fromToday().map(Tick.by(Minute1));
    }

    public final Function<Execution, Tick> ticklize(TickSpan span) {
        AtomicReference<Tick> latest = new AtomicReference();

        return e -> {
            Tick tick = latest.get();

            if (tick == null || !e.exec_date.isBefore(tick.end)) {
                ZonedDateTime startTime = span.calculateStartTime(e.exec_date);
                ZonedDateTime endTime = span.calculateEndTime(e.exec_date);

                tick = new Tick(startTime, endTime, e.price);
                latest.set(tick);
            }
            tick.update(e);
            return tick;
        };
    }

    /**
     * Read tick data.
     * 
     * @param start
     * @param end
     * @param span
     * @param every
     * @return
     */
    public Signal<Tick> read(ZonedDateTime start, ZonedDateTime end, TickSpan span, boolean every) {
        Signal<Tick> signal = new Signal<>((observer, disposer) -> {
            ZonedDateTime day = start;
            ZonedDateTime[] current = new ZonedDateTime[] {start.withHour(0).withMinute(0).withSecond(0).withNano(0)};

            // read from cache
            while (day.isBefore(end)) {
                Path file = file(current[0], span);

                if (Files.exists(file)) {
                    try {
                        I.signal(Files.lines(file))
                                .map(Tick::new)
                                .effect(tick -> current[0] = tick.end)
                                .take(tick -> tick.start.isBefore(end))
                                .to(observer);
                    } catch (Exception e) {
                        break;
                    }
                    day = day.plusDays(1);
                } else {
                    break;
                }
            }

            // read from execution flow
            return disposer.add(range(current[0], end).map(Tick.by(span))
                    .effect(tick -> writers.computeIfAbsent(span, CacheWriter::new).write(tick))
                    .to(observer));
        });
        return every ? signal : signal.diff().delay(1);
    }

    /**
     * Locate cache file.
     * 
     * @param time
     * @param span
     * @return
     */
    private Path file(ZonedDateTime time, TickSpan span) {
        return cacheRoot().resolve(span.name()).resolve(formatter.format(time).concat(".log"));
    }

    /**
     * @version 2018/01/29 16:57:46
     */
    private class CacheWriter {

        private final TickSpan span;

        /** The writer thread. */
        private final ExecutorService writer = Executors.newSingleThreadExecutor(run -> {
            Thread thread = new Thread(run);
            thread.setName("Log Writer");
            thread.setDaemon(true);
            return thread;
        });

        private Tick latest;

        /**
         * @param span
         */
        private CacheWriter(TickSpan span) {
            this.span = span;
        }

        /**
         * Write tick to cache.
         * 
         * @param tick
         */
        private void write(Tick tick) {
            if (tick != latest) {
                // write latest tick
                writer.execute(() -> {
                    try {
                        Path path = file(tick.start, span);

                        if (Files.notExists(path)) {
                            Files.createDirectories(path.getParent());
                        }

                        RandomAccessFile store = new RandomAccessFile(path.toFile(), "rw");
                        FileChannel channel = store.getChannel();
                        channel.position(channel.size());
                        channel.write(ByteBuffer.wrap((tick + "\r\n").getBytes(StandardCharsets.UTF_8)));
                        channel.close();
                        store.close();
                    } catch (Exception e) {
                        throw I.quiet(e);
                    }
                });

                // next
                latest = tick;
            }
        }
    }
}
