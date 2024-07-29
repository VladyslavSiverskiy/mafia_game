package com.vsiverskyi.utils;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Component
public class SettingsUtil {
    private static final String SETTINGS_FILE = "settings.properties";

    public static int getSecondsPerMove() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            properties.load(reader);

            // Get the value for secondsPerMove
            String secondsPerMove = properties.getProperty("secondsPerMove");
            if (secondsPerMove != null) {
                return Integer.parseInt(secondsPerMove);
            }
        } catch (IOException e) {
            System.out.println("Error loading settings: " + e.getMessage());
        }
        return 10; // Default value
    }

    public static int getSecondsPerDefence() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            properties.load(reader);

            // Get the value for secondsPerMove
            String secondsPerDefence = properties.getProperty("secondsPerDefence");
            if (secondsPerDefence != null) {
                return Integer.parseInt(secondsPerDefence);
            }
        } catch (IOException e) {
            System.out.println("Error loading settings: " + e.getMessage());
        }
        return 10; // Default value
    }

    public static int getSecondsPerPresentation() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            properties.load(reader);
            // Get the value for secondsPerMove
            String secondsPerMove = properties.getProperty("secondsPerPresentation");
            if (secondsPerMove != null) {
                return Integer.parseInt(secondsPerMove);
            }
        } catch (IOException e) {
            System.out.println("Error loading settings: " + e.getMessage());
        }
        return 10; // Default value
    }

    public static int getSecondsPerDiscussion() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            properties.load(reader);

            // Get the value for secondsPerMove
            String secondsPerMove = properties.getProperty("secondsPerDiscussion");
            if (secondsPerMove != null) {
                return Integer.parseInt(secondsPerMove);
            }
        } catch (IOException e) {
            System.out.println("Error loading settings: " + e.getMessage());
        }
        return 120; // Default value
    }
}
