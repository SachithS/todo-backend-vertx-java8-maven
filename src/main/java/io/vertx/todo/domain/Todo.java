package io.vertx.todo.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sachith Senarathne
 * <p>
 * This class (Todo.java) is responsible for store a single Todo task
 * with its attributes
 * </p>
 */
public class Todo {

	/**
	 * An Integer value that may be updated atomically
	 */
	private static final AtomicInteger COUNTER = new AtomicInteger();
	private final int id;
	private String message;
	private String dateCreated;

	public Todo(String message) {
		
		// Atomically increments by one the current value
		this.id = COUNTER.getAndIncrement();
		this.message = message;
		
		// set the current date to created date
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		// get current date time with Calendar()
		Calendar cal = Calendar.getInstance();
		System.out.println();
		this.dateCreated = dateFormat.format(cal.getTime());
		
	}

	public int getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDateCreated() {
		return dateCreated;
	}


}
