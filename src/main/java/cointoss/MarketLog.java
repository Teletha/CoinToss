/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import cointoss.market.bitflyer.BitFlyerService;
import cointoss.util.Chrono;
import cointoss.util.Num;
import cointoss.util.Span;
import filer.Filer;
import kiss.Disposable;
import kiss.I;
import kiss.Signal;

/**
 * @version 2018/05/26 10:41:05
 */
public class MarketLog implements Disposable {

    /** The log writer. */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, run -> {
        Thread thread = new Thread(run);
        thread.setName("Market Log Writer");
        thread.setDaemon(true);
        return thread;
    });

    /** The writer thread. */
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(run -> {
        final Thread thread = new Thread(run);
        thread.setName("Log Writer");
        thread.setDaemon(true);
        return thread;
    });

    /** The file data format */
    private static final DateTimeFormatter fileName = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** The file name pattern. */
    private static final Pattern Name = Pattern.compile("\\D.(\\d.)\\.log");

    /** The market provider. */
    private final MarketService service;

    /** The root directory of logs. */
    private final Path root;

    /** The first day. */
    private ZonedDateTime cacheFirst;

    /** The last day. */
    private ZonedDateTime cacheLast;

    /** The latest execution id. */
    private long latestId = 23164000;

    /** The latest cached id. */
    private long cacheId;

    /** The latest realtime id. */
    private long realtimeId;

    /** The current log writer. */
    private Cache logger;

    /**
     * Create log manager.
     * 
     * @param provider
     */
    public MarketLog(MarketService service) {
        this(service, Paths.get(".log").resolve(service.exchangeName).resolve(service.marketName));
    }

    /**
     * Create log manager with the specified log store directory.
     * 
     * @param service A market service.
     * @param root A log store directory.
     */
    MarketLog(MarketService service, Path root) {
        this.service = Objects.requireNonNull(service);
        this.root = Objects.requireNonNull(root);
        this.logger = new Cache(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, Chrono.UTC));

        try {
            ZonedDateTime start = null;
            ZonedDateTime end = null;

            for (Path file : Filer.walk(root, "execution*.log").toList()) {
                String name = file.getFileName().toString();
                ZonedDateTime date = LocalDate.parse(name.substring(9, 17), fileName).atTime(0, 0, 0, 0).atZone(Chrono.UTC);

                if (start == null || end == null) {
                    start = date;
                    end = date;
                } else {
                    if (start.isAfter(date)) {
                        start = date;
                    }

                    if (end.isBefore(date)) {
                        end = date;
                    }
                }
            }
            this.cacheFirst = start;
            this.cacheLast = end;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void vandalize() {
        logger.dispose();
    }

    /**
     * Read date from the specified date.
     * 
     * @param start
     * @return
     */
    public final synchronized Signal<Execution> from(ZonedDateTime start) {
        return new Signal<Execution>((observer, disposer) -> {
            // read from cache
            if (cacheFirst != null) {
                ZonedDateTime current = start.isBefore(cacheFirst) ? cacheFirst : start.isAfter(cacheLast) ? cacheLast : start;
                current = current.withHour(0).withMinute(0).withSecond(0).withNano(0);

                while (disposer.isDisposed() == false && !current.isAfter(cacheLast)) {
                    disposer.add(read(current).effect(e -> latestId = cacheId = e.id)
                            .take(e -> e.exec_date.isAfter(start))
                            .to(observer::accept));
                    current = current.plusDays(1);
                }
            }

            AtomicBoolean completeREST = new AtomicBoolean();

            // read from realtime API
            if (disposer.isNotDisposed()) {
                disposer.add(service.executions().effect(e -> {
                    if (e.id == 0) {
                        e.id = ++realtimeId;
                    }
                    realtimeId = e.id;
                }).skipUntil(e -> completeREST.get()).effect(this::cache).to(observer::accept));
            }

            // read from REST API
            if (disposer.isNotDisposed()) {
                disposer.add(rest(latestId).effect(this::cache).effectOnComplete(() -> completeREST.set(true)).to(observer::accept));
            }

            return disposer;
        });
    }

    /**
     * Store cache data.
     * 
     * @param exe
     */
    private void cache(Execution exe) {
        if (cacheId < exe.id) {
            cacheId = exe.id;

            if (logger.end.isBefore(exe.exec_date)) {
                logger.dispose();
                logger = new Cache(exe.exec_date);
            }
            logger.write(exe);

            // writer.submit(() -> {
            // try {
            // ZonedDateTime date = exe.exec_date;
            //
            // if (cache == null || cacheLast.isBefore(date)) {
            // I.quiet(cache);
            //
            // File file = locateLog(date).toFile();
            // file.createNewFile();
            // cache = new BufferedWriter(new FileWriter(file, true));
            // cacheLast = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            // }
            // cache.write(exe.toString() + "\r\n");
            // cache.flush();
            // } catch (IOException e) {
            // e.printStackTrace();
            // throw I.quiet(e);
            // }
            // });
        }
    }

    /**
     * Read data from REST API.
     */
    private Signal<Execution> rest(long id) {
        return new Signal<>((observer, disposer) -> {
            long offset = 0;
            latestId = id;

            while (disposer.isNotDisposed()) {
                try {
                    List<Execution> executions = service.executions(latestId + service.executionMaxAcquirableSize() * offset)
                            .retry()
                            .toList();

                    // skip if there is no new execution
                    if (executions.get(0).id == latestId) {
                        offset++;
                        continue;
                    }
                    offset = 0;

                    for (int i = executions.size() - 1; 0 <= i; i--) {
                        Execution exe = executions.get(i);

                        if (latestId < exe.id) {
                            observer.accept(exe);
                            latestId = exe.id;
                        }
                    }
                } catch (Exception e) {
                    // ignore to retry
                }

                if (realtimeId != 0 && realtimeId <= latestId) {
                    break;
                }

                try {
                    Thread.sleep(110);
                } catch (final InterruptedException e) {
                    observer.error(e);
                }
            }
            observer.complete();

            return disposer;
        });
    }

    /**
     * Read log at the specified date.
     * 
     * @param year
     * @param month
     * @param day
     * @return
     */
    public final Signal<Execution> at(int year, int month, int day) {
        return at(LocalDate.of(year, month, day));
    }

    /**
     * Read log at the specified date.
     * 
     * @param date
     * @return
     */
    public final Signal<Execution> at(LocalDate date) {
        return at(date.atTime(0, 0).atZone(Chrono.UTC));
    }

    /**
     * Read log at the specified date.
     * 
     * @param date
     * @return
     */
    public final Signal<Execution> at(ZonedDateTime date) {
        if (date.isBefore(service.start())) {
            return Signal.EMPTY;
        }

        // check cache
        Path file = locateLog(date);

        // no cache
        if (file == null) {
            // try the previous day
            return at(date.minusDays(1));
        }
        return read(date);
    }

    /**
     * Locate execution log.
     * 
     * @param date A date time.
     * @return A file location.
     */
    Path locateLog(ZonedDateTime date) {
        return root.resolve("execution" + Chrono.DateCompact.format(date) + ".log");
    }

    /**
     * Locate compressed execution log.
     * 
     * @param date A date time.
     * @return A file location.
     */
    Path locateCompactLog(ZonedDateTime date) {
        return root.resolve("compact" + Chrono.DateCompact.format(date) + ".log");
    }

    /**
     * Parse date from file name.
     * 
     * @param file
     * @return
     */
    private ZonedDateTime parse(Path file) {
        String name = file.getFileName().toString();
        Matcher matcher = Name.matcher(name);

        if (matcher.matches()) {
            return ZonedDateTime.of(LocalDateTime.parse(matcher.group(1)), Chrono.UTC);
        } else {
            throw new Error("Illegal file name [" + name + "]");
        }
    }

    /**
     * Read log from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromToday() {
        return from(ZonedDateTime.now());
    }

    /**
     * Read log from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromYestaday() {
        return fromLast(1);
    }

    /**
     * Read log from the specified date.
     * 
     * @param start
     * @return
     */
    public final Signal<Execution> fromLast(int days) {
        return fromLast(days, ChronoUnit.DAYS);
    }

    /**
     * Read log from the specified date.
     * 
     * @param time A duration.
     * @param unit A duration unit.
     * @return
     */
    public final Signal<Execution> fromLast(int time, ChronoUnit unit) {
        return from(ZonedDateTime.now(Chrono.UTC).minus(time, unit));
    }

    /**
     * Read log from the specified start to end.
     * 
     * @param start
     * @param end
     * @return
     */
    public final Signal<Execution> rangeAll() {
        return range(cacheFirst, cacheLast);
    }

    /**
     * Read log from the specified start to end.
     * 
     * @param start
     * @param end
     * @return
     */
    public final Signal<Execution> range(Span span) {
        return range(span.start, span.end);
    }

    /**
     * Read log from the specified start to end.
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
     * Read log from the specified start to end.
     * 
     * @param days
     * @return
     */
    public final Signal<Execution> rangeRandom(int days) {
        return range(Span.random(cacheFirst, cacheLast.minusDays(1), days));
    }

    /**
     * Read {@link Execution} log of the specified {@link ZonedDateTime}.
     * 
     * @param date
     * @return
     */
    public final Signal<Execution> read(ZonedDateTime date) {
        return new Signal<>((observer, disposer) -> {
            try {
                Path log = locateLog(date);
                Path compact = locateCompactLog(date);
                boolean hasLog = Files.isExecutable(log);
                boolean hasCompact = Files.exists(compact);

                if (!hasLog && !hasCompact) {
                    return disposer;
                }

                if (!hasCompact) {
                    ZonedDateTime nextDay = date.plusDays(1);

                    if (Files.exists(locateCompactLog(nextDay)) || Files.exists(locateLog(nextDay))) {
                        // compact today log
                        // writeCompactLog(date, readLog(date, false));
                    }
                }

                CsvParserSettings settings = new CsvParserSettings();
                settings.getFormat().setDelimiter(' ');

                CsvParser parser = new CsvParser(settings);
                parser.beginParsing(hasCompact ? new ZstdInputStream(newInputStream(compact)) : newInputStream(log), ISO_8859_1);

                String[] row;
                Execution previous = null;

                while (disposer.isNotDisposed() && (row = parser.parseNext()) != null) {
                    Execution e = hasCompact && previous != null ? service.decode(row, previous) : new Execution(row);
                    observer.accept(e);

                    previous = e;
                }

                parser.stopParsing();
                observer.complete();
                return disposer;
            } catch (Exception e) {
                throw I.quiet(e);
            }
        });
    }

    /**
     * Read normal execution log actually.
     * 
     * @param date A target date.
     */
    Signal<Execution> readLog(ZonedDateTime date, boolean compact) {
        return new Signal<>((observer, disposer) -> {
            try {
                CsvParserSettings settings = new CsvParserSettings();
                settings.getFormat().setDelimiter(' ');

                CsvParser parser = new CsvParser(settings);
                parser.beginParsing(compact ? new ZstdInputStream(newInputStream(locateCompactLog(date)))
                        : newInputStream(locateLog(date)), ISO_8859_1);

                String[] row;
                Execution previous = null;

                while (disposer.isNotDisposed() && (row = parser.parseNext()) != null) {
                    observer.accept(previous = compact && previous != null ? service.decode(row, previous) : new Execution(row));
                }

                parser.stopParsing();
                observer.complete();

                return disposer;
            } catch (IOException e) {
                throw I.quiet(e);
            }
        });
    }

    /**
     * Write compact execution log actually.
     * 
     * @param date A target date.
     * @param executions A execution log.
     */
    void writeCompactLog(ZonedDateTime date, Signal<Execution> executions) {
        try {
            Path file = locateCompactLog(date);

            CsvWriterSettings setting = new CsvWriterSettings();
            setting.getFormat().setDelimiter(' ');

            CsvWriter writer = new CsvWriter(new ZstdOutputStream(Files.newOutputStream(file), 1), ISO_8859_1, setting);

            Execution[] previous = new Execution[1];
            executions.to(e -> {
                writer.writeRow(service.encode(e, previous[0]));
                previous[0] = e;
            });
            writer.close();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2018/05/27 10:31:20
     */
    private class Cache implements Disposable {

        /** The target datetime. */
        private final ZonedDateTime start;

        /** The end datetime */
        private final ZonedDateTime end;

        /** The log file. */
        private final Path log;

        /** The compact log file. */
        private final Path compact;

        /** The latest id. */
        private long latest;

        /** The wrting execution queue. */
        private ConcurrentLinkedQueue<Execution> writing = new ConcurrentLinkedQueue();

        /** The actual log writer. */
        private final ScheduledFuture writer;

        /**
         * @param date
         */
        private Cache(ZonedDateTime date) {
            this.start = date.truncatedTo(ChronoUnit.DAYS);
            this.end = this.start.plusDays(1).minusNanos(1);
            this.log = locateLog(date);
            this.compact = locateCompactLog(date);

            // schedule writing task
            this.writer = scheduler.scheduleWithFixedDelay(this::write, 30, 30, TimeUnit.SECONDS);
        }

        /**
         * Read cached date.
         * 
         * @return
         */
        private Signal<Execution> read() {
            return new Signal<>((observer, disposer) -> {
                return disposer;
            });
        }

        /**
         * Write {@link Execution} log.
         * 
         * @param execution
         */
        private void write(Execution execution) {
            writing.add(execution);
        }

        /**
         * Write all buffered execution log to file.
         */
        private void write() {
            if (writing.isEmpty()) {
                return;
            }

            // switch buffer
            ConcurrentLinkedQueue<Execution> remaining = writing;
            writing = new ConcurrentLinkedQueue();

            // build text
            StringBuilder builder = new StringBuilder();

            for (Execution e : remaining) {
                builder.append(e).append("\r\n");
            }

            // write to file
            try (FileChannel channel = FileChannel.open(log, CREATE, APPEND)) {
                channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void vandalize() {
            write();
            writer.cancel(false);
        }
    }

    // public static void main(String[] args) {
    // MarketLog log = new MarketLog(BitFlyerService.FX_BTC_JPY);
    // long start = System.currentTimeMillis();
    // Filer.walk(log.root, "*.log").to(file -> {
    // long s = System.currentTimeMillis();
    // log.read(file).to(e -> {
    // });
    // long e = System.currentTimeMillis();
    // System.out.println("Done " + file + " " + (e - s));
    // });
    // long end = System.currentTimeMillis();
    // System.out.println(end - start);
    // }
    //
    /**
     * @param args
     */
    public static void main2(String[] args) {
        System.out.println(Num.of(10));
        MarketLog log = new MarketLog(BitFlyerService.FX_BTC_JPY);

        Filer.walk(log.root, "execution*.log").to(file -> {

        });
    }
}
