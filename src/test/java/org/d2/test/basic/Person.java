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
package org.d2.test.basic;

import java.util.ArrayList;
import java.util.List;

import org.d2.D2Metadata;
import org.d2.annotations.D2Entity;
import org.d2.annotations.D2Id;
import org.d2.annotations.D2Indexed;
import org.d2.annotations.D2Versioned;


@D2Entity( alias="Person", bucketName="people")
@D2Versioned(version="1.0.0")
public class Person
{
    protected D2Metadata md;
    
    @D2Id private String id;
    @D2Indexed private String firstName;
    @D2Indexed private String lastName;
    
    private Person spouse;
    private List<Person> children;
    
    public Person(String firstName, String lastName)
    {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public void addChild(Person child)
    {
        if(children==null) children = new ArrayList<Person>();
        children.add(child);
    }

    public D2Metadata getMd()
    {
        return md;
    }

    public void setMd(D2Metadata md)
    {
        this.md = md;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public List<Person> getChildren()
    {
        return children;
    }

    public void setChildren(List<Person> children)
    {
        this.children = children;
    }

    public Person getSpouse()
    {
        return spouse;
    }

    public void setSpouse(Person spouse)
    {
        this.spouse = spouse;
    }
}

