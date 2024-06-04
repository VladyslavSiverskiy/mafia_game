package com.vsiverskyi;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 *  Class that run JavaFx application
 *
 * * */
@SpringBootApplication
public class ApplicationRunner{

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
