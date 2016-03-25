package io.vertx.todo;

import static io.vertx.ext.sync.Sync.awaitResult;
import java.util.HashMap;
import java.util.Map;
import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sync.SyncVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.rxjava.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.todo.domain.Todo;
import static io.vertx.ext.sync.Sync.*;


/**
 * @author Sachith Senarathne
 * <p>
 * This class (TodoApplication.java) is responsible for creating the router
 * object , starter method which will create the web server and to
 * define the routes for the
 * </p>
 *
 */
public class TodoApplication extends SyncVerticle {

	private Map<Integer, Todo> todomap = new HashMap<Integer, Todo>();
	private RedisClient client = null;
	
	/* (non-Javadoc)
	 * @see io.vertx.ext.sync.SyncVerticle#start(io.vertx.core.Future)
	 */
	@Suspendable
	@Override
	public void start(Future<Void> fut) {
		
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
		vertx.createHttpServer().requestHandler(fiberHandler(todorouter::accept )).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				config().getInteger("http.port", 8080), result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});
		
		/*vertx.createHttpServer().requestHandler(fiberHandler(req -> {

		      // Send a message to address and wait for a reply
		      Message<String> reply = awaitResult(h -> eb.send(ADDRESS, "blah", h));

		      System.out.println("Got reply: " + reply.body());

		      req.response().end("blah");

		    })).listen(8080, "localhost");*/

		// Initializing the Redis vertx client and setting the host to local
		// host, port will be by default 6379
		//client = RedisClient.create(vertx, new RedisOptions().setHost("127.0.0.1"));
		client = RedisClient.create(vertx, new RedisOptions().setHost("127.0.0.1"));
	}

	/**
	 * @param routingContext
	 * <p>
	 * Method to create a Todo by getting the user's message, getting
	 * the routingContext which receiving by the accept and storing in redis
	 * </p>
	 */
	private void createTodo(RoutingContext routingContext) {

		// Read the request's content and create an Todo task.
		final String message = routingContext.request().getParam("msg");
        Todo todo = new Todo(message);
        System.out.println(message);
        
		// Adding todo task to redis db using client
		client.set(Integer.toString(todo.getId()), todo.getMessage(), r -> {
			if (!r.succeeded()) {
				// showing the cause of faliure
				System.out.println("Operation Failed " + r.cause());
			} else {
				// Return the created todo task as JSON
				routingContext.response().setStatusCode(201)
						.putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(todo));
			}
		});
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

		// getting the todo message for received id(key)
		client.get(id, r -> {
			if (r.succeeded()) {
				// setting the respons header and returning the todo as a
				// encoded json
				if (r.result().length() > 0) {
					routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
							.end(Json.encodePrettily(new Todo(Integer.parseInt(id), r.result())));
				} else {
					// sending the not found status
					routingContext.response().setStatusCode(404).end();
				}
			} else {
				// showing the error cause
				System.out.println("Connection or Operation Failed " + r.cause());
			}
		});
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

		// temp storing id and updated message for the task
		final String id = routingContext.request().getParam("id");
		final String newTask = routingContext.request().getParam("message");

		// getting the task and updating, message will overwritten since its
		// same key
		client.get(id, r -> {
			if (r.succeeded()) {

				if (r.result().length() > 0) {

					client.set(id, newTask, s -> {
						if (r.succeeded()) {

							// setting the respons header and returning the todo
							// as a encoded json
							routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
									.end(Json.encodePrettily(new Todo(Integer.parseInt(id), newTask)));
						} else {
							System.out.println("Connection or Operation Failed " + r.cause());
						}
					});
				} else {
					// sending the not found status
					routingContext.response().setStatusCode(404).end();
				}
			} else {
				// showing the error cause
				System.out.println("Connection or Operation Failed " + r.cause());
			}
		});
	}
	
	/**
	 * @param routingContext
	 * <p>
	 * Method will delete all the todos in the local map
	 * </p>
	 */
	private void deleteAll(RoutingContext routingContext) {

		// flushdb the selected db by the program to delete all
		client.flushdb(r -> {
			if (r.succeeded()) {
				// send 204 (No content) to indicate user
				routingContext.response().setStatusCode(204).end();
			} else {
				// showing the error cause
				System.out.println("Connection or Operation Failed " + r.cause());
			}
		});
	}

	/**
	 * @param routingContext
	 * <p>
	 * Responsible for retrieving all todos from the list and send as encoded json to user
	 * </p>
	 */
	private void getAllTodos(RoutingContext routingContext) {

		Map<Integer, String> todos = new HashMap<Integer, String>();

		JsonArray keys = awaitResult(r -> client.keys("*", r));

		for (Object k : keys) {
			String key = (String) k;
			String todomessage = awaitResult(h -> client.get(key, h));
			todos.put(Integer.parseInt(key), todomessage);
		}

		// Write the HTTP response
		// The response is in JSON using the utf-8 encoding
		// will return map of
		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(todos.values()));
	}
}
