/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.message;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.james.mime4j.ExampleMail;
import org.apache.james.mime4j.stream.BaseTestForBodyDescriptors;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeEntityConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.stream.MutableBodyDescriptor;

public class MaximalBodyDescriptorTest extends BaseTestForBodyDescriptors {

    MimeTokenStream parser;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MimeEntityConfig config = new MimeEntityConfig();
        config.setStrictParsing(true);
        parser = new MimeTokenStream(config, 
                new MaximalBodyDescriptorFactory(null));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMimeVersionDefault() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.RFC822_SIMPLE_BYTES);
        assertEquals(1, descriptor.getMimeMajorVersion());
        assertEquals(0, descriptor.getMimeMinorVersion());
    }
    
    public void testMimeVersion() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_ASCII_COMMENT_IN_MIME_VERSION_BYTES);
        assertEquals(2, descriptor.getMimeMajorVersion());
        assertEquals(4, descriptor.getMimeMinorVersion());
    }
    
    public void testContentId() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_8859_BYTES);
        assertEquals(1, descriptor.getMimeMajorVersion());
        assertEquals(0, descriptor.getMimeMinorVersion());
        assertEquals(ExampleMail.CONTENT_ID, descriptor.getContentId());
    }

    public void testContentDescription() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_8859_BYTES);
        assertEquals(1, descriptor.getMimeMajorVersion());
        assertEquals(0, descriptor.getMimeMinorVersion());
        assertEquals(ExampleMail.CONTENT_DESCRIPTION, descriptor.getContentDescription());
    }
    
    public void testMimeVersionHeaderBreak() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_ASCII_MIME_VERSION_SPANS_TWO_LINES_BYTES);
        assertEquals(4, descriptor.getMimeMajorVersion());
        assertEquals(1, descriptor.getMimeMinorVersion());
    }
    
    public void testContentDispositionType() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_BASE64_LATIN1_BYTES);
        assertEquals("inline", descriptor.getContentDispositionType());
    }
    
    public void testContentDispositionTypeCaseConversion() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_BASE64_LATIN1_BYTES);
        assertEquals("Should be converted to lower case", "inline", descriptor.getContentDispositionType());
        assertNotNull(descriptor.getContentDispositionParameters());
        assertEquals(0, descriptor.getContentDispositionParameters().size());
    }
    
    public void testContentDispositionParameters() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_WITH_CONTENT_DISPOSITION_PARAMETERS_BYTES);
        assertEquals("inline", descriptor.getContentDispositionType());
        assertNotNull(descriptor.getContentDispositionParameters());
        assertEquals(3, descriptor.getContentDispositionParameters().size());
        assertEquals("value", descriptor.getContentDispositionParameters().get("param"));
        assertEquals("1", descriptor.getContentDispositionParameters().get("one"));
        assertEquals("bar", descriptor.getContentDispositionParameters().get("foo"));
    }
    
    public void testContentDispositionStandardParameters() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_BINARY_ATTACHMENTS_BYTES, 1);
        assertEquals("attachment", descriptor.getContentDispositionType());
        assertNotNull(descriptor.getContentDispositionParameters());
        assertEquals(5, descriptor.getContentDispositionParameters().size());
        assertEquals("blob.png", descriptor.getContentDispositionFilename());
        
        SimpleDateFormat dateparser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateparser.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        assertEquals(dateparser.parse("2008-06-21 15:32:18"), descriptor.getContentDispositionModificationDate());
        assertEquals(dateparser.parse("2008-06-20 10:15:09"), descriptor.getContentDispositionCreationDate());
        assertEquals(dateparser.parse("2008-06-22 12:08:56"), descriptor.getContentDispositionReadDate());
        assertEquals(10234, descriptor.getContentDispositionSize());
    }
    
    public void testLanguageParameters() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_BINARY_ATTACHMENTS_BYTES, 3);
        assertNotNull(descriptor.getContentLanguage());
        assertEquals(3, descriptor.getContentLanguage().size());
        assertEquals("en", descriptor.getContentLanguage().get(0));
        assertEquals("en-US", descriptor.getContentLanguage().get(1));
        assertEquals("en-CA", descriptor.getContentLanguage().get(2));
    }
    
    public void testContentLocationRelativeUrl() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_CONTENT_LOCATION_BYTES, 0);
        assertEquals("relative/url", descriptor.getContentLocation());
    }
    
    public void testContentLocationAbsoluteUrl() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_CONTENT_LOCATION_BYTES, 1);
        assertEquals("http://www.example.org/absolute/rhubard.txt", descriptor.getContentLocation());
    }
    
    public void testContentLocationWithComment() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_CONTENT_LOCATION_BYTES, 3);
        assertEquals("http://www.example.org/absolute/comments/rhubard.txt", descriptor.getContentLocation());
    }
    
    public void testContentLocationFoldedUrl() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.MULTIPART_WITH_CONTENT_LOCATION_BYTES, 4);
        assertEquals("http://www.example.org/this/is/a/very/long/url/split/over/two/lines/", descriptor.getContentLocation());
    }
    
    public void testContentMD5Url() throws Exception {
        MaximalBodyDescriptor descriptor = describe(ExampleMail.ONE_PART_MIME_WITH_CONTENT_DISPOSITION_PARAMETERS_BYTES);
        assertEquals(ExampleMail.MD5_CONTENT, descriptor.getContentMD5Raw());
    }
    
    private MaximalBodyDescriptor describe(byte[] mail, int zeroBasedPart) throws Exception {
        ByteArrayInputStream bias = new ByteArrayInputStream(mail);
        parser.parse(bias);
        EntityState state = parser.next();
        while (state != EntityState.T_END_OF_STREAM && zeroBasedPart>=0) {
            state = parser.next();
            if (state == EntityState.T_BODY) {
                --zeroBasedPart;
            }
        }
        assertEquals(EntityState.T_BODY, state);
        BodyDescriptor descriptor = parser.getBodyDescriptor();
        assertNotNull(descriptor);
        assertTrue("Parser is maximal so body descriptor should be maximal", descriptor instanceof MaximalBodyDescriptor);
        return (MaximalBodyDescriptor) descriptor;
    }
    
    private MaximalBodyDescriptor describe(byte[] mail) throws Exception {
        ByteArrayInputStream bias = new ByteArrayInputStream(mail);
        parser.parse(bias);
        EntityState state = parser.next();
        while (state != EntityState.T_BODY && state != EntityState.T_END_OF_STREAM) 
        {
            state = parser.next();
        }
        assertEquals(EntityState.T_BODY, state);
        BodyDescriptor descriptor = parser.getBodyDescriptor();
        assertNotNull(descriptor);
        assertTrue("Parser is maximal so body descriptor should be maximal", descriptor instanceof MaximalBodyDescriptor);
        return (MaximalBodyDescriptor) descriptor;
    }

    @Override
    protected MutableBodyDescriptor newBodyDescriptor() {
        return new MaximalBodyDescriptor();
    }

    @Override
    protected MutableBodyDescriptor newBodyDescriptor(BodyDescriptor parent) {
        return new MaximalBodyDescriptor(parent);
    }
}
