package br.com.voteflix.servidor.dao;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public static final int SUCESSO = 1;
    public static final int ERRO_DUPLICADO = 2;
    public static final int ERRO_SQL = 3;
    public static final int ERRO_CONEXAO = 4;

    public List<Usuario> listarTodosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id, login FROM usuarios";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (conn == null) {
                System.err.println("Erro de conexão ao listar todos os usuários.");
                return null;
            }

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNome(rs.getString("login"));
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar todos os usuários: " + e.getMessage());
            return null;
        }
        return usuarios;
    }

    public void criarAdminSeNaoExistir() {
        if (obterUsuarioPorLogin("admin") == null) {
            System.out.println("Usuário 'admin' não encontrado, criando...");
            int resultado = cadastrarUsuario("admin", "admin");
            if (resultado == SUCESSO) {
                System.out.println("Usuário 'admin' criado com sucesso.");
            } else {
                System.err.println("Falha ao criar usuário 'admin' automaticamente. Código: " + resultado);
            }
        }
    }

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
                        return new String[]{
                                rs.getString("id"),
                                rs.getString("login"),
                                rs.getString("senha")
                        };
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar usuário por login '" + login + "': " + e.getMessage());
        }
        return null;
    }

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
                    return null;
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new String[]{
                                rs.getString("id"),
                                rs.getString("login"),
                                rs.getString("senha")
                        };
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar usuário por ID '" + id + "': " + e.getMessage());
        }
        return null;
    }

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
        return null;
    }

    public int cadastrarUsuario(String login, String senha) {

        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Não foi possível conectar ao banco de dados para cadastro.");
                return ERRO_CONEXAO;
            }

            if (obterUsuarioPorLogin(login) != null) {
                System.err.println("Erro ao cadastrar: Usuário '" + login + "' já existe.");
                return ERRO_DUPLICADO;
            }

            String sql = "INSERT INTO usuarios (login, senha) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, senha);
                int affectedRows = pstmt.executeUpdate();
                return (affectedRows > 0) ? SUCESSO : ERRO_SQL;
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao cadastrar usuário '" + login + "': " + e.getMessage());
            return ERRO_SQL;
        }
    }

    public String[] validarLogin(String login, String senha) {
        String sql = "SELECT id, login, senha FROM usuarios WHERE login = ?";
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
                        String senhaDoBanco = rs.getString("senha");

                        if (senha.equals(senhaDoBanco)) {
                            String funcao = "admin".equalsIgnoreCase(loginUsuario) ? "admin" : "user";
                            return new String[]{id, loginUsuario, funcao};
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao validar login para '" + login + "': " + e.getMessage());
        }
        return null;
    }

    public boolean atualizarUsuario(String id, String novaSenha) {

        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            System.err.println("Tentativa de atualizar senha para vazia para ID: " + id);
            return false;
        }

        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        int usuarioIdInt;

        try {
            usuarioIdInt = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            System.err.println("Erro: ID inválido para atualizarUsuario: " + id);
            return false;
        }

        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) {
                System.err.println("Erro de conexão ao atualizar usuário ID: " + id);
                return false;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, novaSenha);
                pstmt.setInt(2, usuarioIdInt);
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            }

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao atualizar usuário ID '" + id + "': " + e.getMessage());
            return false;
        }
    }

    public boolean apagarUsuario(String id) {
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

        Connection conn = null;
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) {
                System.err.println("Erro de conexão ao apagar usuário ID: " + id);
                return false;
            }

            conn.setAutoCommit(false);

            ReviewDAO reviewDAO = new ReviewDAO();
            try (PreparedStatement pstmtSelectReviews = conn.prepareStatement(selectReviewsSql)) {
                pstmtSelectReviews.setInt(1, usuarioId);
                try (ResultSet rsReviews = pstmtSelectReviews.executeQuery()) {
                    while (rsReviews.next()) {
                        int filmeId = rsReviews.getInt("filme_id");
                        int notaAntiga = rsReviews.getInt("nota");
                        reviewDAO.recalcularMediaFilme(conn, filmeId, 0, notaAntiga);
                    }
                }
            }

            try (PreparedStatement pstmtReviews = conn.prepareStatement(deleteReviewsSql)) {
                pstmtReviews.setInt(1, usuarioId);
                pstmtReviews.executeUpdate();
                System.out.println("Reviews para usuário ID " + usuarioId + " excluídas (se existiam).");
            }

            try (PreparedStatement pstmtUsuario = conn.prepareStatement(deleteUsuarioSql)) {
                pstmtUsuario.setInt(1, usuarioId);
                int affectedRows = pstmtUsuario.executeUpdate();
                if (affectedRows == 0) {
                    System.err.println("Usuário com ID " + usuarioId + " não encontrado para exclusão.");
                    conn.rollback();
                    return false;
                }
                System.out.println("Usuário com ID " + usuarioId + " excluído.");
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao apagar usuário ID " + id + " e/ou reviews: " + e.getMessage());
            try {
                if (conn != null) {
                    System.err.println("Realizando rollback da transação.");
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Erro CRÍTICO ao tentar realizar rollback: " + ex.getMessage());
            }
            return false;
        } finally {
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