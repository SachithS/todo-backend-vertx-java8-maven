package io.vertx.todo;

import java.util.HashMap;
import java.util.Map;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.todo.domain.Todo;

/**
 * @author Sachith Senarathne
 * <p>
 * This class (TodoApplication.java) is responsible for creating the router
 * object , starter method which will create the web server and to
 * define the routes for the
 * </p>
 *
 */
/**
 * @author sachith
 *
 */
public class TodoApplication extends AbstractVerticle {

	private Map<Integer, Todo> todomap = new HashMap<Integer, Todo>();
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Future<Void> fut) {

		// Initiating the todo map by adding one todo task
		createInitialMap();
		
		// Create a router object. which will help to serve the route end-points
		Router todorouter = Router.router(vertx);

		// set Statis handler to handle and serve staic page
		todorouter.route("/assets/*").handler(StaticHandler.create("assets"));

		// define route for retrieve all todos available and giving the method
		// "getAll" to handler to execute
		todorouter.get("/api/todos").handler(this::getAllTodos);

		// define route for creating a todo and giving the method "createTodo"
		// to handler to execute
		todorouter.post("/api/todo").handler(this::createTodo);

		// define route for getting a specific todo and giving the method
		// "getOneTodo" to handler to execute
		todorouter.get("/api/todo/:id").handler(this::getOneTodo);

		// define route for update a specific todo and giving the method
		// "updateTodo" to handler to execute
		todorouter.put("/api/todo/:id").handler(this::updateTodo);

		// define route for delete all todos and giving the method "deleteAll"
		// to handler to execute
		todorouter.delete("/api/todos").handler(this::deleteAll);

		// Create the HTTP server and pass the "accept" method to the request
		// handler.
		vertx.createHttpServer().requestHandler(todorouter::accept).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				config().getInteger("http.port", 8080), result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});
	}

	/**
	 * @param routingContext
	 * <p>
	 * Method to create a Todo by getting the user's message, getting
	 * the routingContext which receiving by the accept
	 * </p>
	 */
	private void createTodo(RoutingContext routingContext) {
		// Read the request's content and create an Todo task.
		final Todo todo = Json.decodeValue(routingContext.getBodyAsString(), Todo.class);
		// Add it to the map
		todomap.put(todo.getId(), todo);

		// Return the created todo task as JSON
		routingContext.response().setStatusCode(201).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(todo));
	}

	/**
	 * @param routingContext
	 * <p>
	 * Method will read the request parameter for the "id" and will
	 * find the id in the map or return the status code as 400 (Bad
	 * Request) to indicate user if id is null
	 * </p>
	 */
	private void getOneTodo(RoutingContext routingContext) {

		final String id = routingContext.request().getParam("id");
		if (id == null) {
			routingContext.response().setStatusCode(400).end();
		} else {
			final Integer todoId = Integer.valueOf(id);
			Todo todo = todomap.get(todoId);

			if (todo == null) {
				// sending the not found status 
				routingContext.response().setStatusCode(404).end();
			} else {

				// setting the respons header and returning the todo as a encoded json
				routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(todo));
			}
		}
	}
	
	/**
	 * @param routingContext
	 * <p>
	 * Method will read the request parameter for the "id" and will
	 * find the id in the map and update the todo as required. 
	 * 400(Bad Request) or 404(Not Found) as required.
	 * </p>
	 */
	private void updateTodo(RoutingContext routingContext) {

		final String todoId = routingContext.request().getParam("id");
		JsonObject json = routingContext.getBodyAsJson();
		if (todoId == null || json == null) {
			routingContext.response().setStatusCode(400).end();
		} else {
			final Integer idAsInteger = Integer.valueOf(todoId);
			Todo todo = todomap.get(idAsInteger);
			if (todo == null) {
				routingContext.response().setStatusCode(404).end();
			} else {
				// set the updated content to the todo task
				todo.setMessage(json.getString("message"));

				// setting the respons header and returning the todo as a encoded json
				routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(todo));
			}
		}
	}
	
	/**
	 * @param routingContext
	 * <p>
	 * Method will delete all the todos in the local map
	 * </p>
	 */
	private void deleteAll(RoutingContext routingContext) {

		// clear the map
		todomap.clear();
		
		// send 204 (No content) to indicate user
		routingContext.response().setStatusCode(204).end();

	}

	/**
	 * @param routingContext
	 * <p>
	 * Responsible for retrieving all todos from the list and send as encoded json to user
	 * </p>
	 */
	private void getAllTodos(RoutingContext routingContext) { 

		// Write the HTTP response
		// The response is in JSON using the utf-8 encoding
		// We returns the list of bottles
		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(todomap.values()));
	}
	
	
	/**
	 * <p>Adding a todo for the map</p>
	 */
	private void createInitialMap(){
		
		Todo todo = new Todo("Start Lerning Vert.x Today");
		todomap.put(todo.getId(), todo);
	}

}
