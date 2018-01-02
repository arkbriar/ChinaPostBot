import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import task.ProgressUpdater;
import task.QueryTask;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
public class Controller {
    private final Logger logger = Logger.getLogger(Controller.class.getName());

    @FXML
    public TextField fileLocation;

    @FXML
    public TextField nameField;

    @FXML
    public ProgressBar progressBar;

    @FXML
    public Button executeButton;

    private Optional<ButtonType> showAlert(
        Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        return alert.showAndWait();
    }

    private void showExceptionDialog(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Exception Details!");
        alert.setContentText(e.getLocalizedMessage());

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    @FXML
    protected void handleSubmitButtonAction(ActionEvent event) {
        if (nameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "姓名不能为空!", "姓名不能为空！", null);
            return;
        }

        String name = nameField.getText();
        Optional<ButtonType> result = showAlert(Alert.AlertType.CONFIRMATION, "确认执行?",
            String.format("确认使用姓名: %s 开始自动查询？", name), null);
        if (result.isPresent() && result.get() != ButtonType.OK) {
            // Exit execution on cancel or close.
            return;
        }

        String filePath = fileLocation.getText();
        if (!filePath.endsWith(".xls") || !filePath.endsWith(".xlsx")) {
            showAlert(Alert.AlertType.WARNING, "文件格式错误!",
                "文件格式错误，请打开 .xls 或 .xlsx 文件!", null);
            return;
        }

        // Disable button and execute the query task.
        executeButton.setDisable(true);

        FutureTask<File> futureTask =
            new FutureTask<>(new QueryTask(name, filePath, new ProgressUpdater() {
                @Override
                public double get() {
                    return progressBar.getProgress();
                }

                @Override
                public void update(double value) {
                    progressBar.setProgress(value);
                }
            }));
        try {
            File outputFile = futureTask.get();
            showAlert(Alert.AlertType.INFORMATION, "执行成功!", "查询已完成!",
                "文件存储在: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            showExceptionDialog(e);
        }

        executeButton.setDisable(false);
    }

    @FXML
    public void handleFileLocationTextFieldClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择运单号文件");
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("xls", "xlsx"));
            if (!fileLocation.getText().isEmpty()) {
                String filePath = fileLocation.getText();
                fileChooser.setInitialDirectory(Paths.get(filePath).toFile().getParentFile());
                fileChooser.setInitialFileName(Paths.get(filePath).getFileName().toString());
            }
            File file = fileChooser.showOpenDialog(fileLocation.getScene().getWindow());
            if (file != null) {
                // Update fileLocation
                fileLocation.setText(file.getAbsolutePath());
            }
        } else {
            // ignore other mouse events
            logger.info("Ignore other mouse events on fileLocation text field!");
        }
    }
}
