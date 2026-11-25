package smarterhoppers.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

import smarterhoppers.SmarterHoppers;

/**
 * {@code config/smarter-hoppers.json}, read once at startup. Missing or invalid values fall back to
 * the defaults, and the file is rewritten with every option so it documents itself.
 */
public final class SmarterHoppersConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "smarter-hoppers.json";

	private static Values values = new Values();

	/** The JSON shape. Field names are the keys in the file. */
	private static final class Values {
		/** Copies of each item a marked container always keeps (R1). */
		int filter_items_kept = 2;
		/** Empty slots of each tower chest that R4 never counts on, as a safety margin. */
		int chest_margin = 0;
	}

	private SmarterHoppersConfig() {
	}

	public static int filterItemsKept() {
		return values.filter_items_kept;
	}

	public static int chestMargin() {
		return values.chest_margin;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		Values loaded = new Values();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				Values read = GSON.fromJson(reader, Values.class);
				if (read != null) {
					loaded = read;
				}
			} catch (IOException | JsonParseException e) {
				SmarterHoppers.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}
		loaded.filter_items_kept = Math.clamp(loaded.filter_items_kept, 0, 64);
		loaded.chest_margin = Math.clamp(loaded.chest_margin, 0, 54);
		values = loaded;
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(values, writer);
			}
		} catch (IOException e) {
			SmarterHoppers.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
