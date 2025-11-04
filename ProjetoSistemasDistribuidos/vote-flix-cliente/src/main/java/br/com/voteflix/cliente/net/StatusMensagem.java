package br.com.voteflix.cliente.net;

import java.util.HashSet;
import java.util.Set;

/**
 * Classe utilitária para validar códigos de status retornados pelo servidor.
 */
public class StatusMensagem {

    // Usamos um Set estático para armazenar os códigos de status conhecidos.
    private static final Set<String> statusConhecidos = new HashSet<>();

    // Bloco estático para inicializar o set
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

    /**
     * Verifica se um código de status é conhecido pelo cliente.
     * @param status O código de status (ex: "401")
     * @return true se o código é conhecido, false caso contrário.
     */
    public static boolean isStatusValido(String status) {
        return statusConhecidos.contains(status);
    }

    /**
     * Retorna uma mensagem genérica caso o status não seja validado
     * ou não haja mensagem do servidor.
     * @param status O código de status recebido.
     * @return Uma mensagem de erro padrão.
     */
    public static String getMensagemPadraoErro(String status) {
        if (!isStatusValido(status)) {
            return "Erro desconhecido ou inesperado retornado pelo servidor (Status: " + status + ")";
        }
        // Se o status é válido mas o servidor não enviou mensagem (pouco provável)
        return "Ocorreu um erro. Status: " + status;
    }
}