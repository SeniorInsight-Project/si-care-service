package com.seniorinsight.backendcore.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tratamento global de exceções da API REST.
 * Retorna respostas padronizadas em formato JSON sem expor stacktraces internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ComorbidadeJaCadastradaException.class)
    public ResponseEntity<Map<String, Object>> handleComorbidadeJaCadastrada(ComorbidadeJaCadastradaException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Comorbidade Já Cadastrada", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(VinculoJaExistenteException.class)
    public ResponseEntity<Map<String, Object>> handleVinculoJaExistente(VinculoJaExistenteException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Vínculo Já Existente", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConviteInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleConviteInvalido(ConviteInvalidoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Convite Inválido", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConviteExpiradoException.class)
    public ResponseEntity<Map<String, Object>> handleConviteExpirado(ConviteExpiradoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.GONE, "Convite Expirado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PerfilNaoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilNaoAutorizado(PerfilNaoAutorizadoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Acesso Proibido", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Acesso Proibido", "Acesso não autorizado para o perfil do usuário.", request.getRequestURI());
    }

    @ExceptionHandler(IdosoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleIdosoNaoEncontrado(IdosoNaoEncontradoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Idoso Não Encontrado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RotinaMedicamentoNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleRotinaMedicamentoNaoEncontrada(RotinaMedicamentoNaoEncontradaException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Rotina de Medicamento Não Encontrada", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MedicamentoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleMedicamentoNaoEncontrado(MedicamentoNaoEncontradoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Medicamento Não Encontrado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ComorbidadeNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleComorbidadeNaoEncontrada(ComorbidadeNaoEncontradaException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Comorbidade Não Encontrada", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AlarmeNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleAlarmeNaoEncontrado(AlarmeNaoEncontradoException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Alarme Não Encontrado", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Requisição Inválida", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Recurso Não Encontrado",
                "O recurso solicitado '" + request.getRequestURI() + "' não foi encontrado.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erro de Validação");
        body.put("message", "Um ou mais campos contêm valores inválidos.");
        body.put("errors", fieldErrors);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Exceção não tratada na requisição [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno",
                "Ocorreu um erro inesperado ao processar sua solicitação.", request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
