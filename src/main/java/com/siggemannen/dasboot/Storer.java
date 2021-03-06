package com.siggemannen.dasboot;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.sqlserver.jdbc.ISQLServerBulkData;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopyOptions;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

/**
 * Handles bulk storage of values
 */
public class Storer
{
    private static final int QUEUE_MAX_SIZE = 300;
    private static final int DEFAULT_BATCH_SIZE = 50000;
    private volatile BulkData bd;
    private final int batchSize;
    private int currentBatch = 0;
    private final Queue<Result> futures = new ConcurrentLinkedQueue<>();
    private final ExecutorService ES = Executors.newWorkStealingPool();
    private final AtomicInteger queue = new AtomicInteger(0);
    private final AtomicInteger circle = new AtomicInteger(0);
    private static final int MAX_ERRORS = 10;
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private int RR = -1;

    private final SQLServerBulkCopy[] bulk;

    public Storer(SQLServerDataSource dss, List<ColumnMeta> meta, String tableName) throws SQLServerException
    {
        this(dss, meta, tableName, Runtime.getRuntime().availableProcessors() * 2);
    }

    public Storer(SQLServerDataSource dss, List<ColumnMeta> meta, String destination, int threads)
            throws SQLServerException
    {
        this(dss, meta, destination, threads, DEFAULT_BATCH_SIZE);
    }

    public Storer(SQLServerDataSource dss, List<ColumnMeta> meta, String destination, int threads, int batchSize)
            throws SQLServerException
    {
        this.bd = new BulkData(meta, batchSize);
        this.batchSize = batchSize;
        this.bulk = new SQLServerBulkCopy[threads];
        
        for (int i = 0; i < threads; i++)
        {
            SQLServerBulkCopyOptions bo = new SQLServerBulkCopyOptions();
            bo.setTableLock(true); // to set bulk logged
            @SuppressWarnings("resource")
            SQLServerBulkCopy sbc = new SQLServerBulkCopy(dss.getConnection());
            for (int ix = 0; ix < meta.size(); ix++)
            {
                sbc.addColumnMapping(meta.get(ix).getName(), meta.get(ix).getName());
            }
            sbc.setDestinationTableName(destination);
            sbc.setBulkCopyOptions(bo);
            bulk[i] = sbc;
        }
    }

    public void add(Object[] ss) throws Exception
    {
        // Stored it in bulk
        int size = bd.add(ss);
        if (errorCount.get() >= MAX_ERRORS)
        {
            for (Result r : futures)
            {
                if (!r.isOk())
                {
                    throw r.getException();
                }
            }
        }
        // Do some slicin'
        if (queue.get() > QUEUE_MAX_SIZE)
        {
            try
            {
                Thread.sleep(100 * Math.max(1, circle.incrementAndGet()));
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (circle.get() >= 5)
                {
                    circle.set(0);
                }
            }
        }
        else if (circle.get() > 4)
        {
            circle.set(0);
        }
        if (size % batchSize == 0)
        {
            RR++;
            int local = RR;
            if (RR >= bulk.length)
            {
                RR = -1;
                local = 0;
            }

            BulkData temp = bd;
            queue.incrementAndGet();
            ES.submit(new X(temp, local)); //using own class to avoid lambdas binding upp BulkData's
            bd = new BulkData(bd.getMeta(), batchSize);
        }
    }

    /**
     * "Finish" storage by flushing data left in the buffers. This must be performed after regular storage
     * 
     * @return result of the whole bulk storage operation
     * @throws Exception
     */
    public Result flush() throws Exception
    {
        bd.flush();
        flushData(bd, bulk[0]);
        ES.shutdown();
        try
        {
            ES.awaitTermination(99999, TimeUnit.MINUTES);
        }
        catch (InterruptedException e)
        {
        }
        for (SQLServerBulkCopy sbc : bulk)
        {
            try
            {
                sbc.close();
            }
            catch (Exception ex)
            {
            }
        }
        for (Result r : futures)
        {
            if (!r.isOk())
            {
                return r;
            }
        }
        return null;
    }

    private class X implements Runnable
    {
        private ISQLServerBulkData d;
        private final SQLServerBulkCopy rr;

        X(ISQLServerBulkData d, int rr)
        {
            this.d = d;
            this.rr = bulk[rr];
        }

        @Override
        public void run()
        {
            Result r = flushData(d, rr);
            d = null;
            if (r.getException() != null)
            {
                futures.add(r);
            }
        }
    }

    private Result flushData(ISQLServerBulkData bd, SQLServerBulkCopy sbc) throws RuntimeException
    {
        try
        {
            synchronized (sbc)
            {
                sbc.writeToServer(bd);
            }

            currentBatch++;
            if (currentBatch % 100 == 0)
            {
                System.out
                        .println(
                                "Processed " + currentBatch + " batches, in queue:" + queue.get() + ", circle is:" + circle);
            }
        }
        catch (Exception e)
        {
            errorCount.incrementAndGet();
            return new Result(e);
        }
        finally
        {
            queue.decrementAndGet();
        }
        return new Result();
    }

}
