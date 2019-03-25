package com.wrox.chat;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonEncoder implements Encoder.Text<ChatMessage>, Decoder.Text<ChatMessage> {

	private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        MAPPER.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

	@Override
    public void init(EndpointConfig config) {}

    @Override
    public String encode(ChatMessage chatMessage) throws EncodeException {

    	try {
    		System.out.println("encode");
    		System.out.println("charMessage => " + chatMessage.toString());

    		//POJO 객체일경우
    		String jsonString = JsonEncoder.MAPPER.writeValueAsString(chatMessage);
    		return jsonString;

        } catch(JsonProcessingException e) {
            throw new EncodeException(chatMessage, e.getMessage(), e);
        }

    	/*
    	//String JSON 일경우
        try (StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(payload);
            return stringWriter.toString();
        } catch (IOException ex) {
            throw new EncodeException(payload, "JsonEncoder could not encode JsonObject", ex);
        }
        */
    }

    @Override
	public ChatMessage decode(String jsonString) throws DecodeException {
    	try {
        	System.out.println("decode");

        	ChatMessage charMessage = JsonEncoder.MAPPER.readValue(jsonString, ChatMessage.class);
            return charMessage;
        } catch(IOException e) {
            throw new DecodeException((ByteBuffer)null, e.getMessage(), e);
        }
	}

    //@Override
    //public void init(EndpointConfig endpointConfig) { }

    @Override
    public void destroy() { }

	@Override
	public boolean willDecode(String s) {
		// TODO Auto-generated method stub
		return false;
	}
}
