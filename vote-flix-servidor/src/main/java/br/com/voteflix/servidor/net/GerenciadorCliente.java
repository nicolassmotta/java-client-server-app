package br.com.voteflix.servidor.net;

import br.com.voteflix.servidor.dao.FilmeDAO;
import br.com.voteflix.servidor.dao.ReviewDAO;
import br.com.voteflix.servidor.dao.UsuarioDAO;
import br.com.voteflix.servidor.model.Filme;
import br.com.voteflix.servidor.model.Review;
import br.com.voteflix.servidor.model.Usuario;
import br.com.voteflix.servidor.security.UtilitarioJwt;
import com.google.gson.Gson;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import com.google.gson.JsonElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;


public class GerenciadorCliente implements Runnable {

    private final Socket socketCliente;
    private final Consumer<String> logger;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private PrintWriter saida;
    private BufferedReader entrada;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final FilmeDAO filmeDAO = new FilmeDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final GerenciadorUsuarios gerenciadorUsuarios;
    private String idUsuarioLogado = null;
    private String loginLogado = null;
    private volatile boolean executando = true;

    public GerenciadorCliente(Socket socket, Consumer<String> logger, GerenciadorUsuarios gerenciadorUsuarios) {
        this.socketCliente = socket;
        this.logger = logger;
        this.gerenciadorUsuarios = gerenciadorUsuarios;
    }

    @Override
    public void run() {
        try {
            saida = new PrintWriter(new java.io.OutputStreamWriter(socketCliente.getOutputStream(), StandardCharsets.UTF_8), true);
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream(), StandardCharsets.UTF_8));

            while (executando && socketCliente != null && !socketCliente.isClosed() && entrada != null) {
                String linhaDoCliente = entrada.readLine();
                if (linhaDoCliente == null) {
                    logger.accept("Linha nula recebida, cliente desconectou: " + (loginLogado != null ? loginLogado : socketCliente.getInetAddress().getHostAddress()));
                    break;
                }
                logger.accept("Recebido: " + linhaDoCliente);
                processarMensagem(linhaDoCliente);
            }
        } catch (IOException e) {
            if (executando) {
                logger.accept("Erro de IO para " + (loginLogado != null ? loginLogado : (socketCliente != null ? socketCliente.getInetAddress().getHostAddress() : "socket desconhecido")) + ": " + e.getMessage());
            }
        } catch (Exception e) {
            logger.accept("Erro inesperado no GerenciadorCliente para " + (loginLogado != null ? loginLogado : (socketCliente != null ? socketCliente.getInetAddress().getHostAddress() : "socket desconhecido")) + ": " + e.getMessage());
            if (saida != null && socketCliente != null && !socketCliente.isClosed()) {
                enviarErroComMensagem(500, "Erro: Falha interna inesperada no servidor.");
            }
        }
        finally {
            encerrarConexao();
        }
    }

    // Modificado para ser public (ou package-private) para ser chamado por GerenciadorUsuarios
    void encerrarConexao() {
        if (!executando) return;

        executando = false;
        String clientAddress = (socketCliente != null && !socketCliente.isClosed()) ? socketCliente.getInetAddress().getHostAddress() : "Endereço Desconhecido";
        String userIdentifier = loginLogado != null ? loginLogado : clientAddress;

        if (idUsuarioLogado != null) {
            // A remoção agora acontece PRIMEIRO em GerenciadorUsuarios.desconectarUsuarioPorId
            // ou aqui se a desconexão for por outro motivo (erro, readLine nulo, etc.)
            if (gerenciadorUsuarios.contemUsuario(idUsuarioLogado)) {
                gerenciadorUsuarios.remover(idUsuarioLogado);
                logger.accept("Usuário '" + loginLogado + "' removido da lista de ativos (Desconexão direta/Erro).");
            }

        } else {
            logger.accept("Cliente " + clientAddress + " desconectado (sem login ativo na sessão).");
        }

        try {
            if (entrada != null) {
                try { entrada.close(); } catch (IOException e) { logger.accept("WARN: Erro ao fechar BufferedReader para " + userIdentifier + ": " + e.getMessage()); }
            }
            if (saida != null) {
                saida.close();
                if(saida.checkError()){ logger.accept("WARN: Erro ao fechar/flushing PrintWriter para " + userIdentifier); }
            }
            if (socketCliente != null && !socketCliente.isClosed()) {
                try { socketCliente.close(); } catch (IOException e) { logger.accept("WARN: Erro ao fechar Socket para " + userIdentifier + ": " + e.getMessage()); }
                logger.accept("Socket fechado para: " + userIdentifier);
            }
        } finally {
            entrada = null;
            saida = null;
            idUsuarioLogado = null;
            loginLogado = null;
            logger.accept("-------------------- FIM DA SESSÃO PARA " + userIdentifier + " --------------------");
        }
    }

    private void processarMensagem(String json) {
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

            if (!jsonObject.has("operacao") || !jsonObject.get("operacao").isJsonPrimitive() || !jsonObject.get("operacao").getAsJsonPrimitive().isString()) {
                enviarErroComMensagem(400, "Erro: Operação não encontrada ou inválida");
                return;
            }
            String operacao = jsonObject.get("operacao").getAsString();

            if (!operacao.equals("LOGIN") && !operacao.equals("CRIAR_USUARIO")) {
                if (!jsonObject.has("token")) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                if (!jsonObject.get("token").isJsonPrimitive() || !jsonObject.get("token").getAsJsonPrimitive().isString()){
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
            }

            switch (operacao) {
                case "CRIAR_USUARIO":
                    processarCadastro(jsonObject);
                    break;
                case "LOGIN":
                    processarLogin(jsonObject);
                    break;
                case "LISTAR_PROPRIO_USUARIO":
                    processarLeituraDados(jsonObject);
                    break;
                case "EDITAR_PROPRIO_USUARIO":
                    processarAtualizacao(jsonObject);
                    break;
                case "EXCLUIR_PROPRIO_USUARIO":
                    processarDelete(jsonObject);
                    break;
                case "LOGOUT":
                    processarLogout(jsonObject);
                    break;
                case "LISTAR_FILMES":
                    processarListarFilmes(jsonObject);
                    break;
                case "BUSCAR_FILME_ID":
                    processarBuscarFilmePorId(jsonObject);
                    break;
                case "CRIAR_REVIEW":
                    processarCriarReview(jsonObject);
                    break;
                case "EDITAR_REVIEW":
                    processarEditarReview(jsonObject);
                    break;
                case "EXCLUIR_REVIEW":
                    processarExcluirReview(jsonObject);
                    break;
                case "LISTAR_REVIEWS_USUARIO":
                    processarListarReviewsUsuario(jsonObject);
                    break;
                case "CRIAR_FILME":
                    processarCriarFilme(jsonObject);
                    break;
                case "EDITAR_FILME":
                    processarEditarFilme(jsonObject);
                    break;
                case "EXCLUIR_FILME":
                    processarExcluirFilme(jsonObject);
                    break;
                case "LISTAR_USUARIOS":
                    processarListarUsuarios(jsonObject);
                    break;
                case "ADMIN_EDITAR_USUARIO":
                    processarAdminEditarUsuario(jsonObject);
                    break;
                case "ADMIN_EXCLUIR_USUARIO":
                    processarAdminExcluirUsuario(jsonObject);
                    break;
                default:
                    logger.accept("Operação desconhecida recebida: " + operacao);
                    enviarErroComMensagem(400, "Erro: Operação não encontrada ou inválida");
                    break;
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            logger.accept("Erro de sintaxe JSON: " + e.getMessage() + " | JSON: " + json);
            enviarErroComMensagem(400, "Erro: Operação não encontrada ou inválida");
        }
        catch (Exception e) {
            logger.accept("Erro inesperado ao processar mensagem (" + json + "): " + e.getMessage());
            e.printStackTrace();
            enviarErroComMensagem(500, "Erro: Falha interna do servidor");
        }
    }

    private void enviarResposta(JsonObject resposta) {
        if (saida == null || socketCliente == null || socketCliente.isClosed() || saida.checkError()) {
            logger.accept("Não foi possível enviar resposta, conexão fechada ou erro no stream: " + gson.toJson(resposta));
            return;
        }
        String jsonResposta = gson.toJson(resposta);
        logger.accept("Enviado: " + jsonResposta);
        saida.println(jsonResposta);
        saida.flush();
        if (saida.checkError()) {
            logger.accept("ERRO: Erro detectado após flush no PrintWriter para " + (loginLogado != null ? loginLogado : socketCliente.getInetAddress().getHostAddress()));
            encerrarConexao();
        }
    }

    private void enviarErroComMensagem(int statusCode, String mensagem) {
        JsonObject resposta = new JsonObject();
        resposta.addProperty("status", String.valueOf(statusCode));
        resposta.addProperty("mensagem", mensagem);
        enviarResposta(resposta);
    }

    private void enviarSucessoComMensagem(int statusCode, String mensagem) {
        JsonObject resposta = new JsonObject();
        resposta.addProperty("status", String.valueOf(statusCode));
        resposta.addProperty("mensagem", mensagem);
        enviarResposta(resposta);
    }

    private void enviarSucessoComDados(int statusCode, String mensagem, String dataKey, JsonElement data) {
        JsonObject resposta = new JsonObject();
        resposta.addProperty("status", String.valueOf(statusCode));
        resposta.addProperty("mensagem", mensagem);
        if (dataKey != null && data != null) {
            resposta.add(dataKey, data);
        }
        enviarResposta(resposta);
    }

    private void processarComAutenticacao(JsonObject jsonObject, Consumer<Claims> action) {
        try {
            if (!jsonObject.has("token")) {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }
            if (!jsonObject.get("token").isJsonPrimitive() || !jsonObject.get("token").getAsJsonPrimitive().isString()){
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }

            String token = jsonObject.get("token").getAsString();
            Claims claims = UtilitarioJwt.validarToken(token);

            if (claims != null) {
                String tokenId = claims.get("id", String.class);
                String tokenSubject = claims.getSubject();

                if (this.idUsuarioLogado == null && tokenId != null) {
                    this.idUsuarioLogado = tokenId;
                    this.loginLogado = tokenSubject;
                    if (!gerenciadorUsuarios.contemUsuario(this.idUsuarioLogado)) {
                        gerenciadorUsuarios.adicionar(this.idUsuarioLogado, this.loginLogado, this); // Passa 'this' (GerenciadorCliente)
                    }
                    logger.accept("Token validado e associado à conexão para '" + this.loginLogado + "'.");
                }
                else if (this.idUsuarioLogado != null && tokenId != null && !tokenId.equals(this.idUsuarioLogado)) { // Adicionado tokenId != null
                    logger.accept("ALERTA: Token com ID (" + tokenId + ") diferente do usuário logado (" + this.idUsuarioLogado + ") na mesma conexão. Desconectando.");
                    enviarErroComMensagem(401, "Erro: Token inválido");
                    encerrarConexao();
                    return;
                } else if (this.idUsuarioLogado == null && tokenId == null) {
                    logger.accept("ALERTA: Token válido mas sem ID de usuário. Rejeitando.");
                    enviarErroComMensagem(401, "Erro: Token inválido");
                    return;
                }

                action.accept(claims);

            } else {
                String clientIp = (socketCliente != null && !socketCliente.isClosed()) ? socketCliente.getInetAddress().getHostAddress() : "IP desconhecido";
                logger.accept("Token inválido recebido de " + (loginLogado != null ? loginLogado : clientIp));
                enviarErroComMensagem(401, "Erro: Token inválido");
            }
        } catch (Exception e) {
            logger.accept("Erro durante validação do token ou execução da ação: " + e.getMessage());
            enviarErroComMensagem(401, "Erro: Token inválido");
        }
    }

    private void processarCadastro(JsonObject jsonObject) {
        try {
            if (!jsonObject.has("usuario") || !jsonObject.get("usuario").isJsonObject()) {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }
            JsonObject usuarioJson = jsonObject.getAsJsonObject("usuario");
            if (!usuarioJson.has("nome") || !usuarioJson.has("senha") ||
                    !usuarioJson.get("nome").isJsonPrimitive() || !usuarioJson.get("nome").getAsJsonPrimitive().isString() ||
                    !usuarioJson.get("senha").isJsonPrimitive() || !usuarioJson.get("senha").getAsJsonPrimitive().isString())
            {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }

            String nome = usuarioJson.get("nome").getAsString().trim();
            String senha = usuarioJson.get("senha").getAsString();

            if (nome.isEmpty() || senha.isEmpty()) {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }
            if (nome.length() < 3 || nome.length() > 20 || !nome.matches("^[a-zA-Z0-9]+$") ||
                    senha.length() < 3 || senha.length() > 20 || !senha.matches("^[a-zA-Z0-9]+$")) {
                enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                return;
            }
            if (nome.equalsIgnoreCase("admin")) {
                enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                return;
            }

            int resultadoCadastro = usuarioDAO.cadastrarUsuario(nome, senha);

            switch (resultadoCadastro) {
                case UsuarioDAO.SUCESSO:
                    logger.accept("Operação CRIAR_USUARIO concluída: Usuário '" + nome + "' cadastrado.");
                    enviarSucessoComMensagem(201, "Sucesso: Recurso cadastrado");
                    break;
                case UsuarioDAO.ERRO_DUPLICADO:
                    logger.accept("Falha ao cadastrar: Usuário '" + nome + "' já existe.");
                    enviarErroComMensagem(409, "Erro: Recurso ja existe");
                    break;
                case UsuarioDAO.ERRO_CONEXAO:
                case UsuarioDAO.ERRO_SQL:
                default:
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    break;
            }
        } catch (Exception e) {
            logger.accept("Erro inesperado ao processar cadastro: " + e.getMessage());
            e.printStackTrace();
            enviarErroComMensagem(500, "Erro: Falha interna do servidor");
        }
    }

    private void processarLogin(JsonObject jsonObject) {
        try {
            if (!jsonObject.has("usuario") || !jsonObject.has("senha") ||
                    !jsonObject.get("usuario").isJsonPrimitive() || !jsonObject.get("senha").isJsonPrimitive())
            {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }

            String usuario = jsonObject.get("usuario").getAsString();
            String senha = jsonObject.get("senha").getAsString();

            if (usuario.trim().isEmpty() || senha.trim().isEmpty()) {
                enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                return;
            }

            String[] dadosUsuario = usuarioDAO.validarLogin(usuario, senha);

            if (dadosUsuario != null) {
                this.idUsuarioLogado = dadosUsuario[0];
                this.loginLogado = dadosUsuario[1];
                String funcao = dadosUsuario[2];

                gerenciadorUsuarios.adicionar(this.idUsuarioLogado, this.loginLogado, this); // Passa 'this'
                logger.accept("Operação LOGIN bem-sucedida para '" + this.loginLogado + "' (" + funcao + ").");

                String token = UtilitarioJwt.gerarToken(this.idUsuarioLogado, this.loginLogado, funcao);

                JsonObject resposta = new JsonObject();
                resposta.addProperty("status", "200");
                resposta.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
                resposta.addProperty("token", token);
                enviarResposta(resposta);
            } else {
                enviarErroComMensagem(403, "Erro: Sem permissão.");
            }
        } catch (Exception e) {
            logger.accept("Erro inesperado durante o login para usuário '" + (jsonObject.has("usuario") ? jsonObject.get("usuario").getAsString() : "DESCONHECIDO") + "': " + e.getMessage());
            enviarErroComMensagem(500, "Erro: Falha interna do servidor");
        }
    }

    private void processarLeituraDados(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userId = claims.get("id", String.class);
                try {
                    Integer.parseInt(userId);
                } catch (NumberFormatException e) {
                    logger.accept("ERROR: Erro CRÍTICO: ID no token não é um número válido: " + userId);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                String[] dadosUsuario = usuarioDAO.obterUsuarioPorId(userId);

                if (dadosUsuario != null) {
                    String loginRetornado = dadosUsuario[1];

                    logger.accept("Operação LISTAR_PROPRIO_USUARIO concluída para '" + claims.getSubject() + "'.");

                    JsonObject resposta = new JsonObject();
                    resposta.addProperty("status", "200");
                    resposta.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
                    resposta.addProperty("usuario", loginRetornado);

                    enviarResposta(resposta);

                } else {
                    logger.accept("Usuário com ID " + userId + " (do token) não encontrado no banco para LISTAR_PROPRIO_USUARIO.");
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                }
            } catch (Exception e) {
                logger.accept("ERROR: Erro inesperado ao ler dados do usuário ID " + claims.get("id", String.class) + ": " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarAtualizacao(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userId = claims.get("id", String.class);
                try {
                    Integer.parseInt(userId);
                } catch (NumberFormatException e) {
                    logger.accept("Erro CRÍTICO: ID no token não é um número válido: " + userId);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                if (!jsonObject.has("usuario") || !jsonObject.get("usuario").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                JsonObject usuarioJson = jsonObject.getAsJsonObject("usuario");
                if (!usuarioJson.has("senha") || !usuarioJson.get("senha").isJsonPrimitive() || !usuarioJson.get("senha").getAsJsonPrimitive().isString()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                String novaSenha = usuarioJson.get("senha").getAsString();

                if (novaSenha.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (novaSenha.length() < 3 || novaSenha.length() > 20 || !novaSenha.matches("^[a-zA-Z0-9]+$")) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (usuarioDAO.atualizarUsuario(userId, novaSenha)) {
                    logger.accept("Operação EDITAR_PROPRIO_USUARIO concluída para '" + claims.getSubject() + "'.");
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else {
                    logger.accept("Falha ao atualizar senha para usuário ID " + userId + ". Verificando se existe...");
                    if (usuarioDAO.obterUsuarioPorId(userId) == null) {
                        enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    } else {
                        enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    }
                }
            } catch (Exception e) {
                logger.accept("Erro inesperado ao atualizar senha do usuário ID " + claims.get("id", String.class) + ": " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarDelete(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userId = claims.get("id", String.class);
                String subject = claims.getSubject();

                try {
                    Integer.parseInt(userId);
                } catch (NumberFormatException e) {
                    logger.accept("ERROR: Erro CRÍTICO: ID no token não é um número válido: " + userId);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                if ("admin".equalsIgnoreCase(subject)) {
                    logger.accept("Tentativa negada de excluir o próprio usuário 'admin' (ID: " + userId + ").");
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                if (usuarioDAO.apagarUsuario(userId)) {
                    logger.accept("Operação EXCLUIR_PROPRIO_USUARIO concluída para '" + subject + "' (ID: " + userId + ").");
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                    encerrarConexao();
                } else {
                    logger.accept("WARN: Falha no DAO ao tentar excluir usuário ID " + userId + ". Verificando se existe...");
                    if (usuarioDAO.obterUsuarioPorId(userId) == null) {
                        enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    } else {
                        enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    }
                    encerrarConexao();
                }
            } catch (Exception e) {
                String userIdForLog = (claims != null && claims.get("id", String.class) != null) ? claims.get("id", String.class) : "[ID desconhecido]";
                logger.accept("ERROR: Erro inesperado ao excluir usuário ID " + userIdForLog + ": " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                encerrarConexao();
            }
        });
    }

    private void processarLogout(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String id = claims.get("id", String.class);
                String subject = claims.getSubject();
                String clientAddress = (socketCliente != null && !socketCliente.isClosed()) ? socketCliente.getInetAddress().getHostAddress() : "Endereço Desconhecido";

                logger.accept("Usuário '" + subject + "' fez logout voluntário.");
                enviarSucessoComMensagem(200, "Sucesso: Operação realizada com sucesso");
                encerrarConexao();

            } catch (Exception e) {
                String userIdForLog = (claims != null) ? claims.getSubject() : "TOKEN INVÁLIDO";
                String clientAddress = (socketCliente != null && !socketCliente.isClosed()) ? socketCliente.getInetAddress().getHostAddress() : "Endereço Desconhecido";
                logger.accept("ERROR: Erro interno durante o processamento do logout para " + userIdForLog + ": " + e.getMessage());

                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                encerrarConexao();
            }
        });
    }

    private void processarListarFilmes(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                List<Filme> filmes = filmeDAO.listarFilmes();

                if (filmes != null) {
                    enviarSucessoComDados(
                            200,
                            "Sucesso: Operação realizada com sucesso",
                            "filmes",
                            gson.toJsonTree(filmes)
                    );
                } else {
                    logger.accept("Erro no DAO ao listar filmes (retornou null).");
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }
            } catch (Exception e) {
                logger.accept("Erro inesperado ao listar filmes: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarBuscarFilmePorId(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                if (!jsonObject.has("id_filme") || !jsonObject.get("id_filme").isJsonPrimitive()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                int filmeId;
                try {
                    filmeId = Integer.parseInt(jsonObject.get("id_filme").getAsString());
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                Filme filme = filmeDAO.obterFilmePorId(filmeId);

                if (filme == null) {
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    return;
                }

                List<Review> reviews = reviewDAO.listarReviewsPorFilme(filmeId);

                if (reviews == null) {
                    logger.accept("Erro no DAO ao buscar reviews para filme ID " + filmeId + " (retornou null).");
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                logger.accept("Filme ID " + filmeId + " e " + reviews.size() + " reviews encontradas.");
                JsonObject respostaFinal = new JsonObject();
                respostaFinal.addProperty("status", "200");
                respostaFinal.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
                respostaFinal.add("filme", gson.toJsonTree(filme));
                respostaFinal.add("reviews", gson.toJsonTree(reviews));
                enviarResposta(respostaFinal);

            } catch (Exception e) {
                String idFilmeStr = jsonObject.has("id_filme") ? jsonObject.get("id_filme").toString() : "[ID não informado]";
                logger.accept("Erro inesperado ao buscar filme por ID " + idFilmeStr + ": " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarCriarReview(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String funcao = claims.get("funcao", String.class);
                if ("admin".equalsIgnoreCase(funcao)) {
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                if (!jsonObject.has("review") || !jsonObject.get("review").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                JsonObject reviewJson = jsonObject.getAsJsonObject("review");

                if (!reviewJson.has("id_filme") || !reviewJson.has("titulo") ||
                        !reviewJson.has("descricao") || !reviewJson.has("nota") ||
                        !reviewJson.get("id_filme").isJsonPrimitive() ||
                        !reviewJson.get("titulo").isJsonPrimitive() || !reviewJson.get("titulo").getAsJsonPrimitive().isString() ||
                        !reviewJson.get("descricao").isJsonPrimitive() || !reviewJson.get("descricao").getAsJsonPrimitive().isString() ||
                        !reviewJson.get("nota").isJsonPrimitive() )
                {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }

                String titulo = reviewJson.get("titulo").getAsString().trim();
                String descricao = reviewJson.get("descricao").getAsString().trim();
                int filmeId;
                int nota;
                int usuarioId;

                try {
                    filmeId = Integer.parseInt(reviewJson.get("id_filme").getAsString());
                    nota = Integer.parseInt(reviewJson.get("nota").getAsString());
                    String userIdStr = claims.get("id", String.class);
                    if (userIdStr == null) throw new NumberFormatException("ID do usuário nulo no token");
                    usuarioId = Integer.parseInt(userIdStr);
                } catch (NumberFormatException e) {
                    logger.accept("Erro ao converter dados numéricos ao criar review: " + e.getMessage());
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres"); //
                    return;
                }

                if (titulo.isEmpty() || descricao.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (nota < 1 || nota > 5) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (titulo.length() > 50 || descricao.length() > 250) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                Review review = new Review();
                review.setId(usuarioId);
                review.setIdFilme(filmeId);
                review.setTitulo(titulo);
                review.setDescricao(descricao);
                review.setNota(nota);

                if (reviewDAO.criarReview(review)) {
                    logger.accept("Review criada com sucesso para filme ID " + filmeId + " pelo usuário ID " + usuarioId);
                    enviarSucessoComMensagem(201, "Sucesso: Recurso cadastrado");
                } else {
                    logger.accept("Falha ao criar review para filme ID " + filmeId + " pelo usuário ID " + usuarioId + " (provável duplicidade ou erro DAO).");
                    enviarErroComMensagem(409, "Erro: Recurso ja existe");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao criar review: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarEditarReview(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userIdStr = claims.get("id", String.class);
                String funcao = claims.get("funcao", String.class);
                int usuarioId;

                try {
                    usuarioId = Integer.parseInt(userIdStr);
                } catch (NumberFormatException e) {
                    logger.accept("Erro CRÍTICO ao converter ID do usuário do token para int: " + userIdStr);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                if (!jsonObject.has("review") || !jsonObject.get("review").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                JsonObject reviewJson = jsonObject.getAsJsonObject("review");

                if (!reviewJson.has("id") || !reviewJson.has("titulo") || !reviewJson.has("descricao") || !reviewJson.has("nota") ||
                        !reviewJson.get("id").isJsonPrimitive() ||
                        !reviewJson.get("titulo").isJsonPrimitive() || !reviewJson.get("titulo").getAsJsonPrimitive().isString() ||
                        !reviewJson.get("descricao").isJsonPrimitive() || !reviewJson.get("descricao").getAsJsonPrimitive().isString() ||
                        !reviewJson.get("nota").isJsonPrimitive() )
                {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }

                String titulo = reviewJson.get("titulo").getAsString().trim();
                String descricao = reviewJson.get("descricao").getAsString().trim();
                int reviewId;
                int nota;

                try {
                    reviewId = Integer.parseInt(reviewJson.get("id").getAsString());
                    nota = Integer.parseInt(reviewJson.get("nota").getAsString());
                } catch (NumberFormatException | ClassCastException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (titulo.isEmpty() || descricao.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (nota < 1 || nota > 5) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (titulo.length() > 50 || descricao.length() > 250) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                Review review = new Review();
                review.setId(reviewId);
                review.setTitulo(titulo);
                review.setDescricao(descricao);
                review.setNota(nota);

                if (reviewDAO.editarReview(review, usuarioId)) {
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else {
                    logger.accept("Falha ao editar review ID " + reviewId + " para usuário ID " + usuarioId + ". Review inexistente ou permissão negada.");
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao editar review: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarExcluirReview(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userIdStr = claims.get("id", String.class);
                String funcao = claims.get("funcao", String.class);
                int usuarioId;

                try {
                    usuarioId = Integer.parseInt(userIdStr);
                } catch (NumberFormatException e) {
                    logger.accept("Erro CRÍTICO ao converter ID do usuário do token para int: " + userIdStr);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                if (!jsonObject.has("id") || !jsonObject.get("id").isJsonPrimitive()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                int reviewId;
                try {
                    reviewId = Integer.parseInt(jsonObject.get("id").getAsString());
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres"); //
                    return;
                }

                if (reviewDAO.excluirReview(reviewId, usuarioId, funcao)) {
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else {
                    logger.accept("Falha ao excluir review ID " + reviewId + " para usuário ID " + usuarioId + ". Review inexistente ou permissão negada.");
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao excluir review: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

        private void processarListarReviewsUsuario(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            try {
                String userIdStr = claims.get("id", String.class);
                int userId;

                try {
                    userId = Integer.parseInt(userIdStr);
                } catch (NumberFormatException e) {
                    logger.accept("Erro CRÍTICO ao converter ID do usuário do token para int: " + userIdStr);
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    return;
                }

                List<Review> reviews = reviewDAO.listarReviewsPorUsuario(userId);

                if (reviews != null) {
                    logger.accept("Listadas " + reviews.size() + " reviews para usuário ID " + userId);
                    enviarSucessoComDados(
                            200,
                            "Sucesso: Operação realizada com sucesso",
                            "reviews",
                            gson.toJsonTree(reviews)
                    );
                } else {
                    logger.accept("Erro no DAO ao listar reviews do usuário ID " + userId + " (retornou null).");
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }
            } catch (Exception e) {
                logger.accept("Erro inesperado ao listar reviews do usuário: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private boolean isAdmin(Claims claims) {
        String funcao = claims.get("funcao", String.class);
        if (!"admin".equalsIgnoreCase(funcao)) {
            enviarErroComMensagem(403, "Erro: sem permissão");
            return false;
        }
        return true;
    }

    private void processarCriarFilme(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                if (!jsonObject.has("filme") || !jsonObject.get("filme").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                JsonObject filmeJson = jsonObject.getAsJsonObject("filme");

                if (!filmeJson.has("titulo") || !filmeJson.has("diretor") || !filmeJson.has("ano") ||
                        !filmeJson.has("sinopse") || !filmeJson.has("genero") ||
                        !filmeJson.get("titulo").isJsonPrimitive() || !filmeJson.get("titulo").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("diretor").isJsonPrimitive() || !filmeJson.get("diretor").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("ano").isJsonPrimitive() || !filmeJson.get("ano").getAsJsonPrimitive().isString() || // <-- MUDANÇA (espera String)
                        !filmeJson.get("sinopse").isJsonPrimitive() || !filmeJson.get("sinopse").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("genero").isJsonArray())
                {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }

                String titulo = filmeJson.get("titulo").getAsString().trim();
                String diretor = filmeJson.get("diretor").getAsString().trim();
                String sinopse = filmeJson.get("sinopse").getAsString().trim();
                JsonArray generos = filmeJson.getAsJsonArray("genero");
                String anoStr = filmeJson.get("ano").getAsString(); // <-- MUDANÇA (lê como String)
                int ano;

                try {
                    ano = Integer.parseInt(anoStr);
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres"); //
                    return;
                }

                if (titulo.isEmpty() || diretor.isEmpty() || sinopse.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (generos.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (titulo.length() > 30 || diretor.length() > 100 || sinopse.length() > 250) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                Filme filme = new Filme();
                filme.setTitulo(titulo);
                filme.setDiretor(diretor);
                filme.setAno(ano);
                filme.setSinopse(sinopse);

                int resultado = filmeDAO.criarFilme(filme, generos);

                if (resultado == FilmeDAO.SUCESSO) {
                    enviarSucessoComMensagem(201, "Sucesso: Recurso cadastrado");
                } else if (resultado == FilmeDAO.ERRO_DUPLICADO) {
                    enviarErroComMensagem(409, "Erro: Recurso ja existe");
                } else {
                    // Cobre ERRO_SQL e ERRO_CONEXAO
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao admin criar filme: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarEditarFilme(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                if (!jsonObject.has("filme") || !jsonObject.get("filme").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                JsonObject filmeJson = jsonObject.getAsJsonObject("filme");

                if (!filmeJson.has("id") || !filmeJson.has("titulo") || !filmeJson.has("diretor") || !filmeJson.has("ano") ||
                        !filmeJson.has("sinopse") || !filmeJson.has("genero") ||
                        !filmeJson.get("id").isJsonPrimitive() || !filmeJson.get("id").getAsJsonPrimitive().isString() || // <-- MUDANÇA
                        !filmeJson.get("titulo").isJsonPrimitive() || !filmeJson.get("titulo").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("diretor").isJsonPrimitive() || !filmeJson.get("diretor").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("ano").isJsonPrimitive() || !filmeJson.get("ano").getAsJsonPrimitive().isString() || // <-- MUDANÇA
                        !filmeJson.get("sinopse").isJsonPrimitive() || !filmeJson.get("sinopse").getAsJsonPrimitive().isString() ||
                        !filmeJson.get("genero").isJsonArray())
                {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }

                String titulo = filmeJson.get("titulo").getAsString().trim();
                String diretor = filmeJson.get("diretor").getAsString().trim();
                String sinopse = filmeJson.get("sinopse").getAsString().trim();
                JsonArray generos = filmeJson.getAsJsonArray("genero");
                String filmeIdStr = filmeJson.get("id").getAsString();
                String anoStr = filmeJson.get("ano").getAsString();
                int filmeId;
                int ano;

                try {
                    filmeId = Integer.parseInt(filmeIdStr);
                    ano = Integer.parseInt(anoStr);
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres"); //
                    return;
                }

                if (titulo.isEmpty() || diretor.isEmpty() || sinopse.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (generos.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (titulo.length() > 30 || diretor.length() > 100 || sinopse.length() > 250) { //
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                Filme filme = new Filme();
                filme.setId(filmeId);
                filme.setTitulo(titulo);
                filme.setDiretor(diretor);
                filme.setAno(ano);
                filme.setSinopse(sinopse);

                int resultado = filmeDAO.editarFilme(filme, generos);

                if (resultado == FilmeDAO.SUCESSO) {
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else if (resultado == FilmeDAO.ERRO_DUPLICADO) {
                    enviarErroComMensagem(409, "Erro: Recurso ja existe");
                } else if (resultado == FilmeDAO.ERRO_NAO_ENCONTRADO) {
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                } else {
                    // Cobre ERRO_SQL e ERRO_CONEXAO
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao admin editar filme: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarExcluirFilme(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                if (!jsonObject.has("id") || !jsonObject.get("id").isJsonPrimitive()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                int filmeId;
                try {
                    filmeId = Integer.parseInt(jsonObject.get("id").getAsString());
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (filmeDAO.excluirFilme(filmeId)) {
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else {
                    logger.accept("Falha ao excluir filme ID " + filmeId + ". Verificando se existia...");
                    if (filmeDAO.obterFilmePorId(filmeId) == null) {
                        enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    } else {
                        enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    }
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao admin excluir filme: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarListarUsuarios(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                List<Usuario> usuarios = usuarioDAO.listarTodosUsuarios();

                if (usuarios != null) {
                    logger.accept("Listados " + usuarios.size() + " usuários para admin.");

                    JsonArray usuariosJson = new JsonArray();
                    for (Usuario u : usuarios) {
                        JsonObject userObj = new JsonObject();
                        userObj.addProperty("id", String.valueOf(u.getId()));
                        userObj.addProperty("nome", u.getNome());
                        usuariosJson.add(userObj);
                    }

                    enviarSucessoComDados(
                            200,
                            "Sucesso: operação realizada com sucesso",
                            "usuarios",
                            usuariosJson
                    );
                } else {
                    logger.accept("Erro no DAO ao listar todos os usuários (retornou null).");
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }
            } catch (Exception e) {
                logger.accept("Erro inesperado ao listar usuários: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarAdminEditarUsuario(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                if (!jsonObject.has("id") || !jsonObject.has("usuario") ||
                        !jsonObject.get("id").isJsonPrimitive() || !jsonObject.get("id").getAsJsonPrimitive().isString() ||
                        !jsonObject.get("usuario").isJsonObject()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                String targetUserId = jsonObject.get("id").getAsString();
                JsonObject usuarioJson = jsonObject.getAsJsonObject("usuario");

                if (!usuarioJson.has("senha") || !usuarioJson.get("senha").isJsonPrimitive() || !usuarioJson.get("senha").getAsJsonPrimitive().isString()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                String novaSenha = usuarioJson.get("senha").getAsString();

                try {
                    Integer.parseInt(targetUserId);
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                if (novaSenha.isEmpty()) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                if (novaSenha.length() < 3 || novaSenha.length() > 20 || !novaSenha.matches("^[a-zA-Z0-9]+$")) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }
                String adminUserId = claims.get("id", String.class);
                if (targetUserId.equals(adminUserId)) {
                    logger.accept("Admin (ID: " + adminUserId + ") tentou editar a própria senha via ADMIN_EDITAR_USUARIO. Negado.");
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                String targetLogin = usuarioDAO.obterLoginPorId(targetUserId);
                if ("admin".equalsIgnoreCase(targetLogin)) {
                    logger.accept("Admin (ID: " + adminUserId + ") tentou editar a senha do usuário 'admin' (ID: " + targetUserId + "). Negado.");
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                if (usuarioDAO.atualizarUsuario(targetUserId, novaSenha)) {
                    logger.accept("Admin (ID: " + adminUserId + ") editou a senha do usuário ID: " + targetUserId);
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                } else {
                    logger.accept("Falha ao admin editar usuário ID " + targetUserId + ". Verificando se existe...");
                    if (usuarioDAO.obterUsuarioPorId(targetUserId) == null) {
                        enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    } else {
                        enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                    }
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao admin editar usuário: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }

    private void processarAdminExcluirUsuario(JsonObject jsonObject) {
        processarComAutenticacao(jsonObject, claims -> {
            if (!isAdmin(claims)) {
                return;
            }

            try {
                if (!jsonObject.has("id") || !jsonObject.get("id").isJsonPrimitive() || !jsonObject.get("id").getAsJsonPrimitive().isString()) {
                    enviarErroComMensagem(422, "Erro: Chaves faltantes ou invalidas");
                    return;
                }
                String targetUserIdStr = jsonObject.get("id").getAsString();
                int targetUserId;

                try {
                    targetUserId = Integer.parseInt(targetUserIdStr);
                } catch (NumberFormatException e) {
                    enviarErroComMensagem(405, "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
                    return;
                }

                String adminUserId = claims.get("id", String.class);
                if (targetUserIdStr.equals(adminUserId)) {
                    logger.accept("Admin (ID: " + adminUserId + ") tentou se auto-excluir via ADMIN_EXCLUIR_USUARIO. Negado.");
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                String targetLogin = usuarioDAO.obterLoginPorId(targetUserIdStr);
                if (targetLogin == null) {
                    enviarErroComMensagem(404, "Erro: Recurso inexistente");
                    return;
                }
                if ("admin".equalsIgnoreCase(targetLogin)) {
                    logger.accept("Admin (ID: " + adminUserId + ") tentou excluir o usuário 'admin' (ID: " + targetUserIdStr + "). Negado.");
                    enviarErroComMensagem(403, "Erro: sem permissão");
                    return;
                }

                if (usuarioDAO.apagarUsuario(targetUserIdStr)) {
                    logger.accept("Admin (ID: " + adminUserId + ") excluiu usuário '" + targetLogin + "' (ID: " + targetUserIdStr + ").");
                    enviarSucessoComMensagem(200, "Sucesso: operação realizada com sucesso");
                    gerenciadorUsuarios.desconectarUsuarioPorId(targetUserIdStr);
                } else {
                    logger.accept("Falha no DAO ao tentar excluir usuário ID " + targetUserIdStr + " (que existia).");
                    enviarErroComMensagem(500, "Erro: Falha interna do servidor");
                }

            } catch (Exception e) {
                logger.accept("Erro inesperado ao admin excluir usuário: " + e.getMessage());
                e.printStackTrace();
                enviarErroComMensagem(500, "Erro: Falha interna do servidor");
            }
        });
    }
}