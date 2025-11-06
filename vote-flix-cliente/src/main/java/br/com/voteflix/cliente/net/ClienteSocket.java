package br.com.voteflix.cliente.net;

import br.com.voteflix.cliente.security.TokenStorage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

public class ClienteSocket {

    private static ClienteSocket instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private Consumer<String> logger;

    private ClienteSocket(String host, int porta, Consumer<String> logger) throws IOException {
        this.logger = logger;
        this.socket = new Socket(host, porta);
        this.out = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        logger.accept("Conectado ao servidor em " + host + ":" + porta);
    }

    public static boolean iniciarConexao(String host, int porta, Consumer<String> logger) {
        try {
            if (instance == null || instance.socket == null || instance.socket.isClosed()) {
                instance = new ClienteSocket(host, porta, logger);
            }
            return true;
        } catch (IOException e) {
            logger.accept("Erro ao conectar: " + e.getMessage());
            instance = null;
            return false;
        }
    }

    public static ClienteSocket getInstance() {
        if (instance == null) {
            System.err.println("ClienteSocket não inicializado. Chame iniciarConexao primeiro.");
        }
        return instance;
    }

    private void enviarRequisicao(JsonObject request, Consumer<JsonObject> callback, Runnable onNetworkComplete) {
        new Thread(() -> {
            JsonObject errorResponse = null;
            try {
                if (socket == null || socket.isClosed() || out == null || in == null) {
                    throw new IOException("Socket não está conectado ou streams não estão inicializados.");
                }

                String operacao = request.has("operacao") ? request.get("operacao").getAsString() : "";
                if (!operacao.equals("LOGIN") && !operacao.equals("CRIAR_USUARIO")) {
                    String token = TokenStorage.getToken();
                    if (token != null) {
                        request.addProperty("token", token);
                    } else if (!operacao.isEmpty()) {
                        logger.accept("Aviso: Token não disponível para a operação " + operacao);
                        if (!operacao.equals("LOGOUT")) {
                            throw new IllegalStateException("Token necessário para operação " + operacao + " não encontrado.");
                        }
                    }
                }

                String jsonRequest = gson.toJson(request);
                logger.accept("Enviando: " + jsonRequest);
                out.println(jsonRequest);
                out.flush();

                String resposta = in.readLine();
                logger.accept("Recebido: " + resposta);

                if (resposta == null) {
                    throw new IOException("A conexão com o servidor foi perdida.");
                }

                JsonObject responseJson = gson.fromJson(resposta, JsonObject.class);
                Platform.runLater(() -> callback.accept(responseJson));

            } catch (JsonSyntaxException e) {
                logger.accept("Erro ao converter a resposta do servidor (JSON inválido): " + e.getMessage());
                errorResponse = new JsonObject();
                errorResponse.addProperty("status", "CLIENT_ERROR");
                errorResponse.addProperty("mensagem", "Resposta inválida do servidor.");
                JsonObject finalErrorResponse1 = errorResponse;
                Platform.runLater(() -> callback.accept(finalErrorResponse1));
            } catch (IOException e) {
                logger.accept("Erro de IO na comunicação: " + e.getMessage());
                errorResponse = new JsonObject();
                errorResponse.addProperty("status", "CLIENT_ERROR");
                errorResponse.addProperty("mensagem", "Falha na comunicação com o servidor (IO).");
                JsonObject finalErrorResponseIO = errorResponse;
                Platform.runLater(() -> callback.accept(finalErrorResponseIO));
            } catch (IllegalStateException e) {
                logger.accept("Erro de estado: " + e.getMessage());
                errorResponse = new JsonObject();
                errorResponse.addProperty("status", "CLIENT_ERROR");
                errorResponse.addProperty("mensagem", e.getMessage());
                JsonObject finalErrorResponseState = errorResponse;
                Platform.runLater(() -> callback.accept(finalErrorResponseState));
            } catch (Exception e) {
                logger.accept("Erro inesperado na comunicação: " + e.getMessage());
                errorResponse = new JsonObject();
                errorResponse.addProperty("status", "CLIENT_ERROR");
                errorResponse.addProperty("mensagem", "Falha inesperada na comunicação com o servidor.");
                JsonObject finalErrorResponseGeneric = errorResponse;
                Platform.runLater(() -> callback.accept(finalErrorResponseGeneric));
            } finally {
                if (onNetworkComplete != null) {
                    Platform.runLater(onNetworkComplete);
                }
            }
        }).start();
    }
    private void enviarRequisicao(JsonObject request, Consumer<JsonObject> callback) {
        enviarRequisicao(request, callback, null);
    }

    private void processarRespostaSimples(JsonObject response, String successStatus, BiConsumer<Boolean, String> callback) {
        if (response == null) {
            callback.accept(false, "Falha na comunicação (resposta nula)");
            return;
        }
        try {
            if (!response.has("status") || !response.has("mensagem")) {
                callback.accept(false, "Resposta incompleta do servidor.");
                return;
            }

            String status = response.get("status").getAsString();
            String mensagem = response.get("mensagem").getAsString();

            if (!StatusMensagem.isStatusValido(status) && !status.equals("CLIENT_ERROR")) {
                logger.accept("Aviso: Status '" + status + "' desconhecido recebido do servidor.");
            }

            callback.accept(successStatus.equals(status), mensagem);

        } catch (Exception e) {
            logger.accept("Erro ao processar resposta simples: " + e.getMessage());
            callback.accept(false, "Erro ao interpretar resposta do servidor.");
        }
    }

    private void processarRespostaComDados(JsonObject response, String dataKey, TriConsumer<Boolean, JsonElement, String> callback) {
        if (response == null) {
            callback.accept(false, null, "Falha na comunicação (resposta nula)");
            return;
        }
        try {
            if (!response.has("status") || !response.has("mensagem")) {
                callback.accept(false, null, "Resposta incompleta do servidor.");
                return;
            }
            String status = response.get("status").getAsString();
            String mensagem = response.get("mensagem").getAsString();

            if (!StatusMensagem.isStatusValido(status) && !status.equals("CLIENT_ERROR")) {
                logger.accept("Aviso: Status '" + status + "' desconhecido recebido do servidor.");
            }

            boolean success = "200".equals(status) || "201".equals(status) || "404".equals(status);
            JsonElement data = null;
            if (success && response.has(dataKey)) {
                data = response.get(dataKey);
            } else if (success) {
                logger.accept("Resposta de sucesso status " + status + " sem a chave de dados esperada '" + dataKey + "'.");
            }

            callback.accept(success, data, mensagem);

        } catch (Exception e) {
            logger.accept("Erro ao processar resposta com dados: " + e.getMessage());
            callback.accept(false, null, "Erro ao interpretar resposta do servidor.");
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public void enviarLogin(String login, String senha, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LOGIN");
        request.addProperty("usuario_login", login);
        request.addProperty("usuario_senha", senha);

        enviarRequisicao(request, response -> {
            if (response == null) {
                callback.accept(false, "Falha na comunicação com o servidor.");
                return;
            }
            try {
                if (!response.has("status") || !response.has("mensagem")) {
                    callback.accept(false, "Resposta incompleta do servidor.");
                    return;
                }
                String status = response.get("status").getAsString();
                String mensagem = response.get("mensagem").getAsString();

                if (!StatusMensagem.isStatusValido(status) && !status.equals("CLIENT_ERROR")) {
                    logger.accept("Aviso: Status '" + status + "' desconhecido recebido no login.");
                }

                if ("200".equals(status) && response.has("token")) {
                    String token = response.get("token").getAsString();
                    TokenStorage.setToken(token);
                    callback.accept(true, mensagem);
                }
                else {
                    if("401".equals(status)) {
                        TokenStorage.clearToken();
                    }
                    callback.accept(false, mensagem);
                }
            } catch (Exception e) {
                logger.accept("Erro ao processar resposta de login: " + e.getMessage());
                callback.accept(false, "Erro ao interpretar resposta do servidor.");
            }
        });
    }


    public void enviarCadastro(String login, String senha, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "CRIAR_USUARIO");
        JsonObject usuario = new JsonObject();
        usuario.addProperty("usuario_login", login);
        usuario.addProperty("usuario_senha", senha);
        request.add("usuario_dados", usuario);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "201", callback);
        });
    }

    public void enviarListarDados(TriConsumer<Boolean, JsonElement, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LISTAR_PROPRIO_USUARIO");
        enviarRequisicao(request, response -> {
            processarRespostaComDados(response, "usuario_login", callback);
        });
    }

    public void enviarEdicaoUsuario(String novaSenha, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EDITAR_PROPRIO_USUARIO");
        JsonObject usuario = new JsonObject();
        usuario.addProperty("nova_senha", novaSenha);
        request.add("usuario_dados", usuario);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void enviarExcluirUsuario(BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EXCLUIR_PROPRIO_USUARIO");
        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
            if (response != null && "200".equals(response.get("status").getAsString())) {
                TokenStorage.clearToken();
                logger.accept("Token local limpo após exclusão da conta.");
            }
        });
    }

    private void enviarLogout(Runnable onCompleteCallback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LOGOUT");

        if (TokenStorage.getToken() == null) {
            logger.accept("Logout não enviado ao servidor: token local já estava nulo.");
            if (onCompleteCallback != null) {
                Platform.runLater(onCompleteCallback);
            }
            return;
        }

        enviarRequisicao(request, response -> {
            if (response != null && response.has("status")) {
                logger.accept("Resposta do servidor ao logout: Status " + response.get("status").getAsString());
            } else {
                logger.accept("Nenhuma resposta ou resposta inválida do servidor ao logout.");
            }
        }, onCompleteCallback);
    }

    public void solicitarLogoutEFechamento(Runnable onGuiCompletion) {
        if (instance == null) {
            if (onGuiCompletion != null) Platform.runLater(onGuiCompletion);
            return;
        }

        Runnable cleanupAndGuiUpdate = () -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    logger.accept("Conexão com o servidor fechada após logout.");
                }
            } catch (IOException e) {
                logger.accept("Erro ao fechar conexão após logout: " + e.getMessage());
            } finally {
                TokenStorage.clearToken();
                logger.accept("Token local limpo após logout/fechamento.");
                instance = null;
                logger.accept("Instância do ClienteSocket limpa.");
                if (onGuiCompletion != null) {
                    Platform.runLater(onGuiCompletion);
                }
            }
        };

        enviarLogout(cleanupAndGuiUpdate);
    }

    public void enviarListarFilmes(TriConsumer<Boolean, JsonElement, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LISTAR_FILMES");
        enviarRequisicao(request, response -> {
            processarRespostaComDados(response, "filmes", callback);
        });
    }

    public void enviarBuscarFilmePorId(String idFilme, TriConsumer<Boolean, JsonElement, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "BUSCAR_FILME_ID");
        request.addProperty("id_filme", idFilme);
        enviarRequisicao(request, response -> {
            if (response == null) {
                callback.accept(false, null, "Falha na comunicação (resposta nula)");
                return;
            }
            try {
                if (!response.has("status") || !response.has("mensagem")) {
                    callback.accept(false, null, "Resposta incompleta do servidor.");
                    return;
                }
                String status = response.get("status").getAsString();
                String mensagem = response.get("mensagem").getAsString();

                if (!StatusMensagem.isStatusValido(status) && !status.equals("CLIENT_ERROR")) {
                    logger.accept("Aviso: Status '" + status + "' desconhecido recebido ao buscar filme.");
                }

                if ("200".equals(status) && response.has("filme") && response.has("reviews")) {
                    callback.accept(true, response, mensagem);
                } else if ("404".equals(status)){
                    callback.accept(false, null, mensagem);
                }
                else {
                    callback.accept(false, null, mensagem);
                }
            } catch (Exception e) {
                logger.accept("Erro ao processar resposta de buscar filme: " + e.getMessage());
                callback.accept(false, null, "Erro ao interpretar resposta do servidor.");
            }
        });
    }

    public void enviarCriarReview(String idFilme, String titulo, String descricao, String nota, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "CRIAR_REVIEW");

        JsonObject review = new JsonObject();
        review.addProperty("id_filme", idFilme);
        review.addProperty("titulo", titulo);
        review.addProperty("descricao", descricao);
        review.addProperty("nota", nota);
        request.add("review", review);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "201", callback);
        });
    }

    public void enviarEditarReview(String idReview, String titulo, String descricao, String nota, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EDITAR_REVIEW");

        JsonObject review = new JsonObject();
        review.addProperty("id", idReview);
        review.addProperty("titulo", titulo);
        review.addProperty("descricao", descricao);
        review.addProperty("nota", nota);
        request.add("review", review);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void enviarExcluirReview(String idReview, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EXCLUIR_REVIEW");
        request.addProperty("id", idReview);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void enviarListarReviewsUsuario(TriConsumer<Boolean, JsonElement, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LISTAR_REVIEWS_USUARIO");
        enviarRequisicao(request, response -> {
            processarRespostaComDados(response, "reviews", callback);
        });
    }

    public void adminCriarFilme(JsonObject filmeJson, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "CRIAR_FILME");
        request.add("filme", filmeJson);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "201", callback);
        });
    }

    public void adminEditarFilme(JsonObject filmeJson, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EDITAR_FILME");
        request.add("filme", filmeJson);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void adminExcluirFilme(String idFilme, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "EXCLUIR_FILME");
        request.addProperty("id", idFilme);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void adminListarUsuarios(TriConsumer<Boolean, JsonElement, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "LISTAR_USUARIOS");
        enviarRequisicao(request, response -> {
            processarRespostaComDados(response, "usuarios", callback);
        });
    }

    public void adminEditarUsuario(String idUsuario, String novaSenha, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "ADMIN_EDITAR_USUARIO");
        request.addProperty("id", idUsuario);

        JsonObject usuario = new JsonObject();
        usuario.addProperty("nova_senha", novaSenha);
        request.add("usuario_dados", usuario);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }

    public void adminExcluirUsuario(String idUsuario, BiConsumer<Boolean, String> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("operacao", "ADMIN_EXCLUIR_USUARIO");
        request.addProperty("id", idUsuario);

        enviarRequisicao(request, response -> {
            processarRespostaSimples(response, "200", callback);
        });
    }
}