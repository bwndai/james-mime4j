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

package org.apache.james.mime4j.parser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeEntityConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.stream.MutableBodyDescriptorFactory;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.stream.RecursionMode;

/**
 * <p>
 * Parses MIME (or RFC822) message streams of bytes or characters and reports 
 * parsing events to a <code>ContentHandler</code> instance.
 * </p>
 * <p>
 * Typical usage:<br/>
 * <pre>
 *      ContentHandler handler = new MyHandler();
 *      MimeStreamParser parser = new MimeStreamParser();
 *      parser.setContentHandler(handler);
 *      parser.parse(new FileInputStream("mime.msg"));
 * </pre>
 */
public class MimeStreamParser {

    private ContentHandler handler = null;
    private boolean contentDecoding;
    private final MimeEntityConfig mimeEntityConfig;
    
    private final MimeTokenStream mimeTokenStream;

    public MimeStreamParser(MimeTokenStream tokenStream) {
        super();
        this.mimeTokenStream = tokenStream;
        this.mimeEntityConfig = tokenStream.getConfig();
        this.contentDecoding = false;
    }

    public MimeStreamParser(
            final MimeEntityConfig config, 
            boolean clone,
            final DecodeMonitor monitor,
            final MutableBodyDescriptorFactory bodyDescFactory) {
        this(new MimeTokenStream(clone ? config.clone() : config, monitor, bodyDescFactory));
    }

    public MimeStreamParser(final MimeEntityConfig config, boolean clone) {
        this(new MimeTokenStream(clone ? config.clone() : config, null, null));
    }

    public MimeStreamParser(
            final MimeEntityConfig config,
            final DecodeMonitor monitor,
            final MutableBodyDescriptorFactory bodyDescFactory) {
        this(config != null ? config : new MimeEntityConfig(), config != null, 
                monitor, bodyDescFactory);
    }

    public MimeStreamParser(final MimeEntityConfig config) {
        this(config, null, null);
    }

    public MimeStreamParser() {
        this(new MimeEntityConfig(), false, null, null);
    }
    
    /**
     * Determines whether this parser automatically decodes body content
     * based on the on the MIME fields with the standard defaults.
     */ 
    public boolean isContentDecoding() {
        return contentDecoding;
    }

    /**
     * Defines whether parser should automatically decode body content
     * based on the on the MIME fields with the standard defaults.
     */ 
    public void setContentDecoding(boolean b) {
        this.contentDecoding = b;
    }

    /**
     * Parses a stream of bytes containing a MIME message. If the mime config of this
     * object contains a not null defaultContentType
     * ({@link MimeEntityConfig#getDefaultContentType()}) a headless parsing is performed.
     * 
     * @param is the stream to parse.
     * @throws MimeException if the message can not be processed
     * @throws IOException on I/O errors.
     */
    public void parse(InputStream inputStream) throws MimeException, IOException {
        if (mimeEntityConfig.getHeadlessParsing() != null) {
            Field contentType = mimeTokenStream.parseHeadless(inputStream, mimeEntityConfig.getHeadlessParsing());
            handler.startMessage();
            handler.startHeader();
            handler.field(contentType);
            handler.endHeader();
        } else {
            mimeTokenStream.parse(inputStream);
        }
        OUTER: for (;;) {
            EntityState state = mimeTokenStream.getState();
            switch (state) {
                case T_BODY:
                    BodyDescriptor desc = mimeTokenStream.getBodyDescriptor();
                    InputStream bodyContent;
                    if (contentDecoding) {
                        bodyContent = mimeTokenStream.getDecodedInputStream(); 
                    } else {
                        bodyContent = mimeTokenStream.getInputStream(); 
                    }
                    handler.body(desc, bodyContent);
                    break;
                case T_END_BODYPART:
                    handler.endBodyPart();
                    break;
                case T_END_HEADER:
                    handler.endHeader();
                    break;
                case T_END_MESSAGE:
                    handler.endMessage();
                    break;
                case T_END_MULTIPART:
                    handler.endMultipart();
                    break;
                case T_END_OF_STREAM:
                    break OUTER;
                case T_EPILOGUE:
                    handler.epilogue(mimeTokenStream.getInputStream());
                    break;
                case T_FIELD:
                    handler.field(mimeTokenStream.getField());
                    break;
                case T_PREAMBLE:
                    handler.preamble(mimeTokenStream.getInputStream());
                    break;
                case T_RAW_ENTITY:
                    handler.raw(mimeTokenStream.getInputStream());
                    break;
                case T_START_BODYPART:
                    handler.startBodyPart();
                    break;
                case T_START_HEADER:
                    handler.startHeader();
                    break;
                case T_START_MESSAGE:
                    handler.startMessage();
                    break;
                case T_START_MULTIPART:
                    handler.startMultipart(mimeTokenStream.getBodyDescriptor());
                    break;
                default:
                    throw new IllegalStateException("Invalid state: " + state);
            }
            state = mimeTokenStream.next();
        }
    }
    
    /**
     * Determines if this parser is currently in raw mode.
     * 
     * @return <code>true</code> if in raw mode, <code>false</code>
     *         otherwise.
     * @see #setRaw(boolean)
     */
    public boolean isRaw() {
        return mimeTokenStream.isRaw();
    }
    
    /**
     * Enables raw mode. In raw mode all future entities (messages 
     * or body parts) in the stream will be reported to the
     * {@link ContentHandler#raw(InputStream)} handler method only.
     * The stream will contain the entire unparsed entity contents 
     * including header fields and whatever is in the body.
     */
    public void setRaw() {
        mimeTokenStream.setRecursionMode(RecursionMode.M_RAW);
    }
    
    /**
     * Enables flat mode. In flat mode rfc822 parts are not recursively 
     * parsed and multipart content is handled as a single "simple" stream.
     */
    public void setFlat() {
        mimeTokenStream.setRecursionMode(RecursionMode.M_FLAT);
    }
    
    /**
     * Enables recursive mode. In tihs mode rfc822 parts are recursively 
     * parsed.
     */
    public void setRecurse() {
        mimeTokenStream.setRecursionMode(RecursionMode.M_RECURSE);
    }

    /**
     * Finishes the parsing and stops reading lines.
     * NOTE: No more lines will be parsed but the parser
     * will still call 
     * {@link ContentHandler#endMultipart()},
     * {@link ContentHandler#endBodyPart()},
     * {@link ContentHandler#endMessage()}, etc to match previous calls
     * to 
     * {@link ContentHandler#startMultipart(BodyDescriptor)},
     * {@link ContentHandler#startBodyPart()},
     * {@link ContentHandler#startMessage()}, etc.
     */
    public void stop() {
        mimeTokenStream.stop();
    }
    
    /**
     * Sets the <code>ContentHandler</code> to use when reporting 
     * parsing events.
     * 
     * @param h the <code>ContentHandler</code>.
     */
    public void setContentHandler(ContentHandler h) {
        this.handler = h;
    }

}
