import javafx.application.Application;
import javafx.stage.Stage;

public class PSLJavaFX extends Application {

    public static void main(String ...args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(getParameters().getRaw().get(0));
        primaryStage.show();
    }


    public static void kar(String title) {
        main(title);
    }
}
