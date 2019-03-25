package com.wrox.chat;

<<<<<<< HEAD
import javax.json.JsonObject;
=======
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
>>>>>>> 15a5cbd913b53025cb7f622f8a27842cd4bdbf46
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

<<<<<<< HEAD
public class ChatMessageCodec implements Encoder.Text<JsonObject> {

    @Override
    public void init(EndpointConfig config) {}

    @Override
    public String encode(JsonObject payload) throws EncodeException {
        return "This silly encode just overides your payload";
=======
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatMessageCodec implements Encoder.BinaryStream<ChatMessage>, Decoder.BinaryStream<ChatMessage> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        MAPPER.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    @Override
    public void encode(ChatMessage chatMessage, OutputStream outputStream) throws EncodeException, IOException {

    	try {
    		System.out.println("encode");

    		OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
    		BufferedWriter bw = new BufferedWriter(osw);

    		System.out.println("charMessage => " + chatMessage.toString());
    		/*
    		byte[] source = chatMessage.getContent().getBytes("UTF-8");
    		for(int i=0;i<source.length;i++) System.out.println("source " + i + " => " + source[i]);

    		System.out.println("source => " + source);
    		String remake = new String(source, "UTF-8");
    		System.out.println("remake => " + remake);
    		 */

            ChatMessageCodec.MAPPER.writeValue(bw, chatMessage);

        } catch(JsonGenerationException | JsonMappingException e) {
            throw new EncodeException(chatMessage, e.getMessage(), e);
        }
    }

    @Override
    public ChatMessage decode(InputStream inputStream) throws DecodeException, IOException {
        try {
        	System.out.println("decode");

        	/*
        	Reader reader = new InputStreamReader(inputStream, "utf-8");
        	String str = "";

        	while(true) {
        		int i = inputStream.read();
        		if( i == -1 ) break;
        		//System.out.println("i = " + (char)i);
        		str += (char)i;
        	}

        	System.out.println("str : " + str);
        	*/

        	ChatMessage charMessage = ChatMessageCodec.MAPPER.readValue(inputStream, ChatMessage.class);
            return charMessage;
        } catch(JsonParseException | JsonMappingException e) {
            throw new DecodeException((ByteBuffer)null, e.getMessage(), e);
        }
>>>>>>> 15a5cbd913b53025cb7f622f8a27842cd4bdbf46
    }

    @Override
    public void destroy() {}
}
