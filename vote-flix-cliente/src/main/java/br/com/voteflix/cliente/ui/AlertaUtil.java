package br.com.voteflix.cliente.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

public class AlertaUtil {

    private static void aplicarCss(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        if (AlertaUtil.class.getResource("/styles.css") != null) {
            dialogPane.getStylesheets().add(
                    AlertaUtil.class.getResource("/styles.css").toExternalForm()
            );
            dialogPane.getStyleClass().add("my-dialog");
        }
    }

    public static void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        aplicarCss(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    public static void mostrarInformacao(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        aplicarCss(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    public static boolean mostrarConfirmacao(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        aplicarCss(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);

        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }
}