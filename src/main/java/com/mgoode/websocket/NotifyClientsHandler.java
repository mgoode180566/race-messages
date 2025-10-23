package com.mgoode.websocket;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.net.URI;
import java.util.Map;

public class NotifyClientsHandler implements RequestHandler<SQSEvent, Void> {
	private final DynamoDbClient ddb = DynamoDbClient.create();
	private final String apiEndpoint = "wss://79hod0tdn9.execute-api.us-east-1.amazonaws.com/production/";
	private final ApiGatewayManagementApiClient apiGw = ApiGatewayManagementApiClient.builder()
		.endpointOverride(URI.create(apiEndpoint))
		.build();
	
	@Override
	public Void handleRequest(SQSEvent event, Context context) {
		var connections = ddb.scan(ScanRequest.builder().tableName("WebSocketConnections").build()).items();
		
		for (SQSEvent.SQSMessage record : event.getRecords()) {
			String messageBody = record.getBody();
			for (var item : connections) {
				String connectionId = item.get("connectionId").s();
				try {
					apiGw.postToConnection(PostToConnectionRequest.builder()
						.connectionId(connectionId)
						.data(SdkBytes.fromUtf8String(messageBody))
						.build());
				} catch (GoneException e) {
					ddb.deleteItem(DeleteItemRequest.builder()
						.tableName("WebSocketConnections")
						.key(Map.of("connectionId", AttributeValue.builder().s(connectionId).build()))
						.build());
				}
			}
		}
		return null;
	}
}

