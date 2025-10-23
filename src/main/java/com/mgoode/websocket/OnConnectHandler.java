package com.mgoode.websocket;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

public class OnConnectHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
	private final DynamoDbClient ddb = DynamoDbClient.create();
	
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
		Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
		String connectionId = (String) requestContext.get("connectionId");
		
		ddb.putItem(PutItemRequest.builder()
			.tableName("WebSocketConnections")
			.item(Map.of("connectionId", AttributeValue.builder().s(connectionId).build()))
			.build());
		
		return Map.of("statusCode", 200);
	}
}
