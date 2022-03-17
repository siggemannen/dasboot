package com.siggemannen.dasboot;

import java.sql.JDBCType;

/**
 * Describes metadata of a bulk insert column
 */
public class ColumnMeta
{
    private final String name;
    private final int id;
    private final int type;
    private final int precision;
    private final int scale;

    /**
     * Creates new column meta data
     * @param name column name
     * @param id this is just a counter
     * @param type JDBC database of the column. See {@link JDBCType}
     * @param precision how many significant digits the field have
     * @param scale how many decimal numbers this number has
     */
    public ColumnMeta(String name, int id, int type, int precision, int scale)
    {
        this.name = name;
        this.id = id;
        this.type = type;
        this.precision = precision;
        this.scale = scale;
    }

    public String getName()
    {
        return name;
    }

    public int getId()
    {
        return id;
    }

    public int getType()
    {
        return type;
    }


    public int getPrecision()
    {
        return precision;
    }

    public int getScale()
    {
        return scale;
    }
}
