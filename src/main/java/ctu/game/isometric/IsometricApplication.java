package ctu.game.isometric;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IsometricApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsometricApplication.class, args);
	}

	@Bean
	public CommandLineRunner gameRunner() {
		return args -> {
			// Configure LibGDX window
			Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
			config.setTitle("CHRONO VEIL");
			config.setWindowedMode(1280, 720);
			config.setResizable(false);

			// Launch the game
			new Lwjgl3Application(new IsometricGame(), config);
		};
	}
}
