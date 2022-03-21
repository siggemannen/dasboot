package com.siggemannen.dasboot;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.microsoft.sqlserver.jdbc.ISQLServerBulkData;

/**
 * Bulk implementation of {@link ISQLServerBulkData}
 */
class BulkData implements ISQLServerBulkData
{
    private final Set<Integer> COLLECT;
    private final List<ColumnMeta> meta;
    private int rowPointer = -1;
    private int size = 0;
    private final Object[] data;
    private int roof = -1;

    BulkData(List<ColumnMeta> meta, int batchSize)
    {
        this.meta = meta;
        roof = batchSize;
        this.data = new Object[batchSize];
        COLLECT = IntStream.range(0, meta.size()).mapToObj(f -> f + 1).collect(Collectors.toSet());
    }

    public int add(Object[] data)
    {
        this.data[size] = data;
        return ++size;
    }

    @Override
    public Set<Integer> getColumnOrdinals()
    {
        return COLLECT;
    }

    @Override
    public String getColumnName(int column)
    {
        return meta.get(column - 1).getName();
    }

    @Override
    public int getColumnType(int column)
    {
        return meta.get(column - 1).getType();
    }

    @Override
    public int getPrecision(int column)
    {
        return meta.get(column - 1).getPrecision();
    }

    @Override
    public int getScale(int column)
    {
        return meta.get(column - 1).getScale();
    }

    @Override
    public Object[] getRowData() throws SQLException
    {
        return (Object[]) data[rowPointer];
    }

    @Override
    public boolean next() throws SQLException
    {
        rowPointer++;
        return rowPointer < Math.min(roof, data.length);
    }

    public void flush()
    {
        roof = size;
    }

    public List<ColumnMeta> getMeta()
    {
        return meta;
    }

}
