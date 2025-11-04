package br.com.voteflix.cliente.security;

public class TokenStorage {

    private static String token;
    // private static String funcao; // Removido

    public static String getToken() {
        return token;
    }

    // Modificado para aceitar apenas o token
    public static void setToken(String token) {
        TokenStorage.token = token;
        // TokenStorage.funcao = null; // Removido
    }

    public static void clearToken() {
        token = null;
        // funcao = null; // Removido
    }

    // Método getFuncao() removido, pois não é mais armazenado aqui
    // public static String getFuncao() {
    //     return funcao;
    // }
}