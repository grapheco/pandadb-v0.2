/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.values.virtual;

import static java.lang.String.format;

import org.neo4j.cypher.internal.runtime.QueryContext;
import org.neo4j.values.AnyValueWriter;

public class NodeReferencePromised extends NodeReference
{
    private final long id;
    private final QueryContext query;

    NodeReferencePromised(long id, QueryContext query)
    {
        super(id);
        this.id = id;
        this.query = query;
    }

    @Override
    public <E extends Exception> void writeTo( AnyValueWriter<E> writer ) throws E
    {
        NodeValue n = this.query.nodeById(this.id);
        writer.writeNode( id, n.labels(), n.properties() );
    }

    @Override
    public String getTypeName()
    {
        return "NodeReference";
    }

    @Override
    public String toString()
    {
        return format( "(%d)", id );
    }

    @Override
    public long id()
    {
        return id;
    }
}