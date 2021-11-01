package com.tfg.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;


import java.net.URI;
import java.net.URISyntaxException;


public final class App {

	private static final String BASE_URI = "http://192.168.1.7:8080/api/";

	private static HttpServer createConection() throws URISyntaxException {
		ResourceConfig rc = new ResourceConfig().packages("com.tfg.api.resources");
		rc.register(MultiPartFeature.class);
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI),rc);
		return server;
	}

	/**
	 * Says hello to the world.
	 * 
	 * @param args The arguments of the program.
	 */
	public static void main(String[] args) {
		try {

			HttpServer server = createConection();
			System.out.println(
					String.format("Starting Server at addres %sapplication.wadl \nPress any key to shutdown ...", BASE_URI));
			System.in.read();
			server.shutdown();
		} catch (Exception e) {
			System.out.println("Hello");
			e.printStackTrace();
		}
	}
}
