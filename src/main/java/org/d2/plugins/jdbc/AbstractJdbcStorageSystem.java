/*******************************************************************************
 * Copyright 2010 Nathan Kopp
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.d2.plugins.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.d2.Bucket;
import org.d2.pluggable.StorageSystem;
import org.nkts.util.StringVisitor;
import org.nkts.util.Util;


public abstract class AbstractJdbcStorageSystem implements StorageSystem
{
    protected Connection conn;
    protected String tableName;
    protected Bucket bucket;
    
    protected abstract String getCreateTableStatement();

    public AbstractJdbcStorageSystem(Bucket bucket, Connection conn)
    {
        this.bucket = bucket;
        this.conn = conn;
        
        tableName = "d2_"+bucket.getName();
        
        createTableIfNecessary(conn);
    }

    private void createTableIfNecessary(Connection conn)
    {
        boolean tableExists = true;
        try
        {
            String sql = "select id from "+tableName+"where id='0'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            rs.close();
            pstmt.close();
        }
        catch(Exception e)
        {
            tableExists = false;
        }
        
        if(!tableExists)
        {
            try
            {
                String ddl = getCreateTableStatement();
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(ddl);
            }
            catch(Exception e)
            {
                throw Util.wrap(e);
            }
        }
    }
    
    @Override
    public void eachId(StringVisitor visitor)
    {
        List<String> list = new ArrayList<String>();
        try
        {
            String sql = "select id from "+tableName;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                String id = rs.getString(1);
                visitor.visit(id);
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public void eachDocument(final StringVisitor docVisitor)
    {
        eachId(new StringVisitor(){
            @Override
            public void visit(String id)
            {
                String doc = loadDocument(id);
                docVisitor.visit(doc);
            }
        });
    }
    
    public void deleteDocument(String id)
    {
        try
        {
            String sql = "delete from "+tableName+" where id=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public String loadDocument(String id)
    {
        try
        {
            String sql = "select data from "+tableName+" where id=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next())
            {
                String val = rs.getString(1);
                rs.close();
                return val;
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public void saveDocument(String id, String str, Date now)
    {
        try
        {
            String sql = "select id from "+tableName+" where id=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            boolean found = rs.next();
            rs.close();
            
            if(found)
            {
                sql = "update "+tableName+" set data=?,dttm=? where id=?";
            }
            else
            {
                sql = "insert into "+tableName+" (data, dttm, id) values (?,?,?)";
            }
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, str);
            pstmt.setTimestamp(3, new Timestamp(now.getTime()));
            int count = pstmt.executeUpdate();
            if(count!=1) throw new RuntimeException("Count was "+count+" but should have been 1 for insert or update statement with id="+id);
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public boolean isIdValid(String id)
    {
        return true;
    }

    public Bucket getBucket()
    {
        return bucket;
    }

    public void close()
    {
        try
        {
            conn.close();
        }
        catch (SQLException e)
        {
            throw Util.wrap(e);
        }
    }

}
