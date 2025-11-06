package br.com.voteflix.cliente.net;

import java.util.HashSet;
import java.util.Set;

public class StatusMensagem {

    private static final Set<String> statusConhecidos = new HashSet<>();

    static {
        // --- SUCESSO ---
        statusConhecidos.add("200"); // É uma resposta genérica para quando tudo correu bem.
        statusConhecidos.add("201"); // Ele significa "A sua requisição foi bem-sucedida e, como resultado, um novo recurso foi criado no servidor"

        // --- ERROS DE CLIENTE (4xx) ---
        statusConhecidos.add("400"); // O id fornecido não é válido (ex: está vazio ou no formato incorreto).
        statusConhecidos.add("401"); // Credenciais inválidas
        statusConhecidos.add("403"); // Tentou acessar/excluir um recurso que não lhe pertence ou para o qual não tem privilégios de administrador.
        statusConhecidos.add("404"); // O id do filme/review/usuário não existe.
        statusConhecidos.add("405"); // Será usado quando uma criação ou alteração não segue o REGEX (quantidade min/max, tipo de caracteres) definidos nos requisitos do projeto
        statusConhecidos.add("409"); // Instância já existe
        statusConhecidos.add("422"); // Dados faltantes ou fora do padrão (campos do json faltantes)

        // --- ERROS DE SERVIDOR (5xx) ---
        statusConhecidos.add("500"); // Uma falha inesperada no servidor ou no banco de dados impede que a lista seja retornada.
    }

    public static boolean isStatusValido(String status) {
        return statusConhecidos.contains(status);
    }
}