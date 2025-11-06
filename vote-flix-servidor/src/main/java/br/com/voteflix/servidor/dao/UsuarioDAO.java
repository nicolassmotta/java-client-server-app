// vote-flix-servidor/src/main/java/br/com/voteflix/servidor/dao/UsuarioDAO.java
package br.com.voteflix.servidor.dao;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.model.Usuario;
// Importe a biblioteca de hashing se for usar (ex: jBCrypt)
// import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // Constantes para status de retorno do cadastrarUsuario
    public static final int SUCESSO = 1;
    public static final int ERRO_DUPLICADO = 2;
    public static final int ERRO_SQL = 3;
    public static final int ERRO_CONEXAO = 4;

    /**
     * Lista todos os usuários (ID e Login) do banco de dados.
     * Usado principalmente pela funcionalidade de admin.
     * @return Lista de objetos Usuario (apenas com id e nome/login preenchidos) ou null em caso de erro.
     */
    public List<Usuario> listarTodosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        // Seleciona apenas id e login para não expor senhas (mesmo que hasheadas)
        String sql = "SELECT id, login FROM usuarios";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (conn == null) { // Checa conexão logo no início
                System.err.println("Erro de conexão ao listar todos os usuários.");
                return null;
            }

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNome(rs.getString("login")); // Mapeia 'login' do DB para 'nome' no Model
                // Não busca/seta a senha aqui
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar todos os usuários: " + e.getMessage());
            return null; // Retorna null indicando erro
        }
        return usuarios; // Retorna a lista (pode estar vazia)
    }

    /**
     * Cria o usuário 'admin' com senha 'admin' se ele ainda não existir.
     * Chamado na inicialização do servidor.
     */
    public void criarAdminSeNaoExistir() {
        if (obterUsuarioPorLogin("admin") == null) {
            System.out.println("Usuário 'admin' não encontrado, criando...");
            int resultado = cadastrarUsuario("admin", "admin"); // Senha padrão insegura!
            if (resultado == SUCESSO) {
                System.out.println("Usuário 'admin' criado com sucesso.");
            } else {
                System.err.println("Falha ao criar usuário 'admin' automaticamente. Código: " + resultado);
            }
        }
    }

    /**
     * Busca um usuário pelo login.
     * @param login O login a ser buscado.
     * @return Array de String {id, login, senha} se encontrado, null caso contrário ou erro.
     * ATENÇÃO: Retorna a senha como está no banco (texto plano ou hash).
     */
    public String[] obterUsuarioPorLogin(String login) {
        String sql = "SELECT id, login, senha FROM usuarios WHERE login = ?";
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao buscar usuário por login: " + login);
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Retorna ID, Login e Senha (hash ou texto plano)
                        return new String[]{
                                rs.getString("id"),
                                rs.getString("login"),
                                rs.getString("senha") // CUIDADO: Senha exposta aqui
                        };
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar usuário por login '" + login + "': " + e.getMessage());
        }
        return null; // Não encontrado ou erro
    }

    /**
     * Busca um usuário pelo ID.
     * @param id O ID do usuário (como String).
     * @return Array de String {id, login, senha} se encontrado, null caso contrário ou erro.
     * ATENÇÃO: Retorna a senha como está no banco.
     */
    public String[] obterUsuarioPorId(String id) {
        String sql = "SELECT id, login, senha FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao buscar usuário por ID: " + id);
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try {
                    pstmt.setInt(1, Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    System.err.println("Erro: ID inválido para obterUsuarioPorId: " + id);
                    return null; // ID não numérico
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new String[]{
                                rs.getString("id"),
                                rs.getString("login"),
                                rs.getString("senha") // CUIDADO: Senha exposta
                        };
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar usuário por ID '" + id + "': " + e.getMessage());
        }
        return null; // Não encontrado ou erro
    }

    /**
     * Busca apenas o login de um usuário pelo ID.
     * Útil para verificar se o ID a ser excluído pertence ao 'admin'.
     * @param id O ID do usuário (como String).
     * @return O login (String) se encontrado, null caso contrário ou erro.
     */
    public String obterLoginPorId(String id) {
        String sql = "SELECT login FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao obter login por ID: " + id);
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try {
                    pstmt.setInt(1, Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    System.err.println("Erro: ID inválido para obterLoginPorId: " + id);
                    return null;
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("login");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar login por ID (" + id + "): " + e.getMessage());
        }
        return null; // Retorna null se não encontrar ou erro
    }


    /**
     * Cadastra um novo usuário no banco de dados.
     * @param login O login desejado.
     * @param senha A senha em texto plano (INSEGURO!).
     * @return Um código de status: SUCESSO, ERRO_DUPLICADO, ERRO_SQL, ERRO_CONEXAO.
     */
    public int cadastrarUsuario(String login, String senha) {
        // *** ALERTA DE SEGURANÇA: Armazenar senha em texto plano é muito inseguro! ***
        // *** Considere usar hashing com salt (ex: BCrypt) ***
        // Exemplo com jBCrypt: String hashedPassword = BCrypt.hashpw(senha, BCrypt.gensalt());

        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Não foi possível conectar ao banco de dados para cadastro.");
                return ERRO_CONEXAO;
            }

            // 1. Verifica duplicidade ANTES de tentar inserir
            if (obterUsuarioPorLogin(login) != null) {
                System.err.println("Erro ao cadastrar: Usuário '" + login + "' já existe.");
                return ERRO_DUPLICADO;
            }

            // 2. Insere o novo usuário
            String sql = "INSERT INTO usuarios (login, senha) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, senha); // Armazena texto plano (INSEGURO)
                // Usando hash: pstmt.setString(2, hashedPassword);
                int affectedRows = pstmt.executeUpdate();
                return (affectedRows > 0) ? SUCESSO : ERRO_SQL; // Retorna sucesso ou erro SQL genérico
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao cadastrar usuário '" + login + "': " + e.getMessage());
            return ERRO_SQL; // Retorna código de erro SQL genérico
        }
    }

    /**
     * Valida as credenciais de login.
     * @param login O login fornecido.
     * @param senha A senha em texto plano fornecida (INSEGURO!).
     * @return Array de String {id, login, funcao} se as credenciais forem válidas, null caso contrário ou erro.
     */
    public String[] validarLogin(String login, String senha) {
        // *** ALERTA DE SEGURANÇA: Comparar senha em texto plano é muito inseguro! ***
        String sql = "SELECT id, login, senha FROM usuarios WHERE login = ?"; // Busca pelo login
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao validar login para: " + login);
                return null;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String id = rs.getString("id");
                        String loginUsuario = rs.getString("login");
                        String senhaDoBanco = rs.getString("senha"); // Senha (texto plano ou hash) do DB

                        // *** Comparação INSEGURA de senha em texto plano ***
                        if (senha.equals(senhaDoBanco)) {
                            // Determina a função baseada no login
                            String funcao = "admin".equalsIgnoreCase(loginUsuario) ? "admin" : "user";
                            return new String[]{id, loginUsuario, funcao}; // Login bem-sucedido
                        }

                        /* Exemplo com jBCrypt:
                        String hashedPasswordFromDB = rs.getString("senha"); // Supondo que a coluna 'senha' armazena o hash
                        if (BCrypt.checkpw(senha, hashedPasswordFromDB)) {
                            String funcao = "admin".equalsIgnoreCase(loginUsuario) ? "admin" : "user";
                            return new String[]{id, loginUsuario, funcao}; // Login bem-sucedido
                        }
                        */
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao validar login para '" + login + "': " + e.getMessage());
        }
        return null; // Falha no login (usuário não encontrado, senha errada ou erro no DB)
    }


    /**
     * Atualiza a senha de um usuário existente.
     * @param id O ID do usuário (como String).
     * @param novaSenha A nova senha em texto plano (INSEGURO!).
     * @return true se a atualização foi bem-sucedida, false caso contrário (usuário não encontrado ou erro).
     */
    public boolean atualizarUsuario(String id, String novaSenha) {
        // *** ALERTA DE SEGURANÇA: Atualizar para senha em texto plano é inseguro! ***
        // *** Considere gerar hash da novaSenha antes de salvar ***
        // Ex: String newHashedPassword = BCrypt.hashpw(novaSenha, BCrypt.gensalt());

        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            System.err.println("Tentativa de atualizar senha para vazia para ID: " + id);
            return false; // Nada para atualizar
        }

        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        int usuarioIdInt;
        try {
            usuarioIdInt = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            System.err.println("Erro: ID inválido para atualizarUsuario: " + id);
            return false; // ID não numérico
        }


        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao atualizar usuário ID: " + id);
                return false;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, novaSenha); // Salva texto plano (INSEGURO)
                // Usando hash: pstmt.setString(1, newHashedPassword);
                pstmt.setInt(2, usuarioIdInt);
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0; // Retorna true se alguma linha foi afetada
            }

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao atualizar usuário ID '" + id + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Apaga um usuário e suas reviews associadas do banco de dados (em uma transação).
     * @param id O ID do usuário (como String).
     * @return true se o usuário e suas reviews foram apagados com sucesso, false caso contrário.
     */
    public boolean apagarUsuario(String id) {
        // CORRIGIDO: RF 1.6 - SQL para buscar as reviews antes de deletar
        String selectReviewsSql = "SELECT filme_id, nota FROM reviews WHERE usuario_id = ?";
        String deleteReviewsSql = "DELETE FROM reviews WHERE usuario_id = ?";
        String deleteUsuarioSql = "DELETE FROM usuarios WHERE id = ?";
        int usuarioId;

        try {
            usuarioId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            System.err.println("ID de usuário inválido para apagar: " + id);
            return false;
        }

        Connection conn = null; // Declarado fora para rollback/commit no catch/finally
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) {
                System.err.println("Erro de conexão ao apagar usuário ID: " + id);
                return false;
            }

            conn.setAutoCommit(false); // 1. Inicia transação

            // CORREÇÃO (RF 1.6): Recalcular médias ANTES de apagar reviews
            ReviewDAO reviewDAO = new ReviewDAO(); // Instancia o DAO para usar seu método
            try (PreparedStatement pstmtSelectReviews = conn.prepareStatement(selectReviewsSql)) {
                pstmtSelectReviews.setInt(1, usuarioId);
                try (ResultSet rsReviews = pstmtSelectReviews.executeQuery()) {
                    while (rsReviews.next()) {
                        int filmeId = rsReviews.getInt("filme_id");
                        int notaAntiga = rsReviews.getInt("nota");
                        // Chamar o método do ReviewDAO para recalcular (notaNova = 0)
                        reviewDAO.recalcularMediaFilme(conn, filmeId, 0, notaAntiga);
                    }
                }
            }


            // 2. Excluir reviews associadas (agora seguro)
            try (PreparedStatement pstmtReviews = conn.prepareStatement(deleteReviewsSql)) {
                pstmtReviews.setInt(1, usuarioId);
                pstmtReviews.executeUpdate();
                System.out.println("Reviews para usuário ID " + usuarioId + " excluídas (se existiam).");
            }

            // 3. Excluir o usuário
            try (PreparedStatement pstmtUsuario = conn.prepareStatement(deleteUsuarioSql)) {
                pstmtUsuario.setInt(1, usuarioId);
                int affectedRows = pstmtUsuario.executeUpdate();
                if (affectedRows == 0) {
                    System.err.println("Usuário com ID " + usuarioId + " não encontrado para exclusão.");
                    conn.rollback(); // Desfaz a transação
                    return false; // Usuário não encontrado
                }
                System.out.println("Usuário com ID " + usuarioId + " excluído.");
            }

            conn.commit(); // 4. Confirma a transação
            return true;

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao apagar usuário ID " + id + " e/ou reviews: " + e.getMessage());
            try {
                if (conn != null) {
                    System.err.println("Realizando rollback da transação.");
                    conn.rollback(); // 5. Desfaz em caso de erro
                }
            } catch (SQLException ex) {
                System.err.println("Erro CRÍTICO ao tentar realizar rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            // Garante que AutoCommit volte ao normal e a conexão seja fechada
            try {
                if (conn != null) {
                    if (!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Erro ao resetar AutoCommit ou fechar conexão após apagar usuário: " + e.getMessage());
            }
        }
    }
}