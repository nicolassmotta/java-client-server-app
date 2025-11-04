package br.com.voteflix.servidor.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class GerenciadorUsuarios {

    private final ConcurrentMap<String, String> usuariosConectados = new ConcurrentHashMap<>();
    private final Consumer<Collection<String>> onUsuariosChanged;
    // Mapa para associar userId a GerenciadorCliente (necessário para desconexão forçada)
    private final ConcurrentMap<String, GerenciadorCliente> clienteHandlers = new ConcurrentHashMap<>();


    public GerenciadorUsuarios(Consumer<Collection<String>> onUsuariosChanged) {
        this.onUsuariosChanged = onUsuariosChanged;
        notificarUI();
    }

    // Modificado para aceitar o handler do cliente
    public void adicionar(String id, String login, GerenciadorCliente handler) {
        usuariosConectados.put(id, login);
        clienteHandlers.put(id, handler); // Armazena a referência
        notificarUI();
    }

    public void remover(String id) {
        if (usuariosConectados.containsKey(id)) {
            usuariosConectados.remove(id);
            clienteHandlers.remove(id); // Remove a referência
            notificarUI();
        }
    }

    // Método adicionado
    public boolean contemUsuario(String id) {
        return usuariosConectados.containsKey(id);
    }

    // Método adicionado (com lógica para desconectar)
    public void desconectarUsuarioPorId(String id) {
        GerenciadorCliente handler = clienteHandlers.get(id);
        if (handler != null) {
            System.out.println("Forçando desconexão para usuário ID: " + id); // Log
            handler.encerrarConexao(); // Chama o método para fechar a conexão do cliente
            // A remoção dos mapas ocorrerá dentro do encerrarConexao via chamada a this.remover(id)
        } else {
            System.out.println("Tentativa de desconectar usuário ID " + id + " falhou: Handler não encontrado."); // Log
            // Mesmo se o handler não for encontrado (raro), remove dos usuários conectados
            if (usuariosConectados.containsKey(id)) {
                remover(id); // Garante a remoção da lista
            }
        }
    }


    private void notificarUI() {
        if (onUsuariosChanged != null) {
            onUsuariosChanged.accept(new ArrayList<>(usuariosConectados.values()));
        }
    }
}