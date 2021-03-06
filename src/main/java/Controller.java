import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
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
    @FXML
    public ProgressIndicator progressIndicator;
    @FXML
    public Button cancelButton;
    private Task currentTask;

    private Optional<ButtonType> showAlert(
        Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        return alert.showAndWait();
    }

    private void showExceptionDialog(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("检测到错误！");
        alert.setHeaderText(e.getLocalizedMessage());

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

    private <T> void startTask(Task<T> task) {
        currentTask = task;
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    protected void reset() {
        executeButton.setDisable(false);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0);
        progressIndicator.setVisible(false);
        cancelButton.setDisable(true);
    }

    @FXML
    protected void handleSubmitButtonAction(ActionEvent event) {
        if (nameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "姓名不能为空!", "姓名不能为空！", null);
            return;
        }

        String filePath = fileLocation.getText();
        if (!filePath.endsWith(".xls") && !filePath.endsWith(".xlsx")) {
            showAlert(Alert.AlertType.WARNING, "文件格式错误!",
                "文件格式错误，请打开 .xls 或 .xlsx 文件!", null);
            return;
        }

        String name = nameField.getText();
        Optional<ButtonType> result = showAlert(Alert.AlertType.CONFIRMATION, "确认执行?",
            String.format("确认使用姓名: %s 开始自动查询？", name),
            "邮政运单号文件为: " + filePath);
        if (result.isPresent() && result.get() != ButtonType.OK) {
            // Exit execution on cancel or close.
            return;
        }

        reset();
        executeButton.setDisable(true);
        cancelButton.setDisable(false);

        Task<File> queryTask = new QueryTask(name, filePath);

        // Reset on failed and cancelled
        queryTask.setOnCancelled(e -> reset());
        queryTask.setOnFailed(e -> {
            showExceptionDialog(e.getSource().getException());
            reset();
        });

        // Show info alert on succeeded
        queryTask.setOnSucceeded(e -> {
            File file = (File) (e.getSource().getValue());
            executeButton.setDisable(false);
            cancelButton.setDisable(true);
            progressIndicator.setVisible(true);
            showAlert(Alert.AlertType.INFORMATION, "自动查询成功！",
                "查询结果存储在: " + file.getAbsolutePath(), null);
        });

        progressBar.progressProperty().bind(queryTask.progressProperty());
        startTask(queryTask);
    }

    @FXML
    public void handleCancelButtonAction(ActionEvent actionEvent) {
        if (currentTask == null || currentTask.isDone()) {
            return;
        }
        currentTask.cancel();
        reset();
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
