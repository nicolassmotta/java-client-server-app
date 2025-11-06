package br.com.voteflix.cliente.security;

public class TokenStorage {

    private static String token;

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        TokenStorage.token = token;
    }

    public static void clearToken() {
        token = null;
    }
}