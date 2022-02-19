package com.tfg.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.URISyntaxException;

import com.tfg.api.filters.AdminJWTTokenNeededFilter;
import com.tfg.api.filters.CorsFilter;
import com.tfg.api.filters.JWTTokenNeededFilter;

public final class App {

	//TODO: No tengo metodo para obtener los proyectos de un usuario
	//TODO: Mejorar las urls de los ProjectResources
	//TODO: Tener cuidado a la hora de hacer las consultas con offset o limits, porque puede dar lugar a perder algun archivo si el orden cambia
	
	private static final String BASE_URI = Dotenv.load().get("API_URL");

	private static HttpServer createConection() throws URISyntaxException {
		ResourceConfig rc = new ResourceConfig().packages("com.tfg.api.resources");
		rc.register(new CorsFilter());
		rc.register(MultiPartFeature.class);
		rc.register(JWTTokenNeededFilter.class);
		rc.register(AdminJWTTokenNeededFilter.class);
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
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
			e.printStackTrace();
		}
	}
}
