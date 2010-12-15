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
package org.d2.example;

import org.d2.D2Metadata;
import org.d2.annotations.D2Entity;
import org.d2.annotations.D2Id;
import org.d2.annotations.D2Indexed;


@D2Entity( alias="Person", bucketName="people")
public class Person
{
    protected D2Metadata md;
    
    @D2Id public String id;
    @D2Indexed public String firstName;
    @D2Indexed public String lastName;
    
    public Person(String firstName, String lastName)
    {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

