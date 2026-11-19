package arajou.br.com.help_desk.common;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Interface utilitária implementada pelos controllers que precisam montar o header
 * {@code Location} numa resposta 201 Created (padrão REST: apontar para a URI do
 * recurso recém-criado).
 */
public interface GenericController {

    /**
     * Monta a URI do recurso recém-criado a partir da URL da requisição atual, anexando
     * o id - ex: {@code POST /tickets} responde com {@code Location: /tickets/{id}}.
     *
     * @param id id do recurso recém-criado
     * @return a URI completa do recurso
     */
    // default: método com implementação na própria interface - qualquer controller que
    // implemente GenericController ganha esse comportamento pronto, sem precisar reescrever.
    default URI generateLocationHeader(Long id){
        return ServletUriComponentsBuilder
                .fromCurrentRequest() // Parte da URL da requisição HTTP atual (ex: "/tickets")
                .path("/{id}") // Acrescenta o id como mais um segmento do caminho
                .buildAndExpand(id)
                .toUri();
    }
}
