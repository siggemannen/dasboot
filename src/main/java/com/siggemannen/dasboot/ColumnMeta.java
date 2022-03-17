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
     * 
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

    /**
     * Creates a copy of meta data with new name
     * 
     * @param copy existing column meta
     * @param name new column name
     * @param id new id
     */
    public ColumnMeta(ColumnMeta copy, String name, int id)
    {
        this.name = name;
        this.id = id;
        this.type = copy.type;
        this.precision = copy.precision;
        this.scale = copy.scale;
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + precision;
        result = prime * result + scale;
        result = prime * result + type;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ColumnMeta other = (ColumnMeta) obj;
        if (id != other.id) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        if (precision != other.precision) return false;
        if (scale != other.scale) return false;
        if (type != other.type) return false;
        return true;
    }
}
