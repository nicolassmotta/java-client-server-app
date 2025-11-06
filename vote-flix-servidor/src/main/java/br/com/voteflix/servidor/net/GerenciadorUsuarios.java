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
    private final ConcurrentMap<String, GerenciadorCliente> clienteHandlers = new ConcurrentHashMap<>();

    public GerenciadorUsuarios(Consumer<Collection<String>> onUsuariosChanged) {
        this.onUsuariosChanged = onUsuariosChanged;
        notificarUI();
    }

    public void adicionar(String id, String login, GerenciadorCliente handler) {
        usuariosConectados.put(id, login);
        clienteHandlers.put(id, handler);
        notificarUI();
    }

    public void remover(String id) {
        if (usuariosConectados.containsKey(id)) {
            usuariosConectados.remove(id);
            clienteHandlers.remove(id);
            notificarUI();
        }
    }

    public boolean contemUsuario(String id) {
        return usuariosConectados.containsKey(id);
    }

    public void desconectarUsuarioPorId(String id) {
        GerenciadorCliente handler = clienteHandlers.get(id);
        if (handler != null) {
            System.out.println("Forçando desconexão para usuário ID: " + id);
            handler.encerrarConexao();
        } else {
            System.out.println("Tentativa de desconectar usuário ID " + id + " falhou: Handler não encontrado.");
            if (usuariosConectados.containsKey(id)) {
                remover(id);
            }
        }
    }

    private void notificarUI() {
        if (onUsuariosChanged != null) {
            onUsuariosChanged.accept(new ArrayList<>(usuariosConectados.values()));
        }
    }
}