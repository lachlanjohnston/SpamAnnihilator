package sample;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

// Naive bayesian classifier

public class Main extends Application {

    private File dir = null;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Spam Annihilator");

        BorderPane container = new BorderPane();

        Button selectData = new Button("Data");
        Button run = new Button("Annihilate Spam");
        HBox top = new HBox();
        top.getChildren().addAll(selectData, run);
        container.setTop(top);

        HBox bottom = new HBox();
        TextArea console = new TextArea();
        console.setMinSize(600, 100);
        bottom.getChildren().add(console);
        container.setBottom(bottom);

        TableView emails = new TableView();
        TableColumn names = new TableColumn("Name");
        TableColumn probs = new TableColumn("Probability");
        names.setCellValueFactory(new PropertyValueFactory<>("name"));
        names.setMinWidth(350);
        probs.setCellValueFactory(new PropertyValueFactory<>("probability"));
        probs.setMinWidth(250);
        emails.getColumns().addAll(names, probs);
        container.setCenter(emails);

        selectData.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(new File("."));
                dir = directoryChooser.showDialog(primaryStage);
            }
        });

        run.setOnAction(event -> {
            if (dir != null) {
                Classifier classifier = new Classifier(dir);
                Thread thread = new Thread(classifier);

                classifier.setListener(new Classifier.ClassifierListener() {
                    @Override
                    public void onCompleted() {
                        emails.setItems(classifier.getEmails());
                    }

                    public void print(String text) {
                        console.appendText(text);
                    }
                });
                thread.start();
            } else {
                console.appendText("Please select a directory\n");
            }
        });

        Scene scene = new Scene(container);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
