package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para o Idoso monitorado na plataforma (HU-002 / DRS).
 * Mapeia atributos físicos, clínicos, cadastrais e campos JSONB nativos do Hibernate 6.
 */
@Entity
@Table(name = "idosos")
public class Idoso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String genero;

    @Column(unique = true, length = 14)
    private String cpf;

    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    @Column(name = "tipo_sanguineo", length = 5)
    private String tipoSanguineo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comorbidades", columnDefinition = "jsonb")
    private List<String> comorbidades = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String alergias;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limites_basais", columnDefinition = "jsonb")
    private Map<String, Object> limitesBasais = new HashMap<>();

    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 50)
    private String estado;

    @Column(length = 20)
    private String cep;

    @Column(name = "plano_saude", length = 100)
    private String planoSaude;

    @Column(name = "observacoes_socorros", columnDefinition = "text")
    private String observacoesSocorros;

    @OneToMany(mappedBy = "idoso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VinculoRede> vinculos = new ArrayList<>();

    @ManyToMany(mappedBy = "idosos")
    private List<Medico> medicos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Idoso() {
    }

    public Idoso(UUID id, String nomeCompleto, LocalDate dataNascimento, String genero, String cpf,
                 String fotoPerfilUrl, String tipoSanguineo, List<String> comorbidades, String alergias,
                 Map<String, Object> limitesBasais, String logradouro, String numero, String bairro,
                 String cidade, String estado, String cep, String planoSaude, String observacoesSocorros) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.dataNascimento = dataNascimento;
        this.genero = genero;
        this.cpf = cpf;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.tipoSanguineo = tipoSanguineo;
        this.comorbidades = comorbidades != null ? comorbidades : new ArrayList<>();
        this.alergias = alergias;
        this.limitesBasais = limitesBasais != null ? limitesBasais : new HashMap<>();
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.cep = cep;
        this.planoSaude = planoSaude;
        this.observacoesSocorros = observacoesSocorros;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.comorbidades == null) {
            this.comorbidades = new ArrayList<>();
        }
        if (this.limitesBasais == null) {
            this.limitesBasais = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = Instant.now();
    }

    // --- Getters e Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(String tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public List<String> getComorbidades() {
        return comorbidades;
    }

    public void setComorbidades(List<String> comorbidades) {
        this.comorbidades = comorbidades;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public Map<String, Object> getLimitesBasais() {
        return limitesBasais;
    }

    public void setLimitesBasais(Map<String, Object> limitesBasais) {
        this.limitesBasais = limitesBasais;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getPlanoSaude() {
        return planoSaude;
    }

    public void setPlanoSaude(String planoSaude) {
        this.planoSaude = planoSaude;
    }

    public String getObservacoesSocorros() {
        return observacoesSocorros;
    }

    public void setObservacoesSocorros(String observacoesSocorros) {
        this.observacoesSocorros = observacoesSocorros;
    }

    public List<VinculoRede> getVinculos() {
        return vinculos;
    }

    public void setVinculos(List<VinculoRede> vinculos) {
        this.vinculos = vinculos;
    }

    public List<Medico> getMedicos() {
        return medicos;
    }

    public void setMedicos(List<Medico> medicos) {
        this.medicos = medicos;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    // --- Helper Methods ---

    public void addVinculo(VinculoRede vinculo) {
        vinculos.add(vinculo);
        vinculo.setIdoso(this);
    }

    public void removeVinculo(VinculoRede vinculo) {
        vinculos.remove(vinculo);
        vinculo.setIdoso(null);
    }

    // --- Builder Pattern ---

    public static IdosoBuilder builder() {
        return new IdosoBuilder();
    }

    public static class IdosoBuilder {
        private UUID id;
        private String nomeCompleto;
        private LocalDate dataNascimento;
        private String genero;
        private String cpf;
        private String fotoPerfilUrl;
        private String tipoSanguineo;
        private List<String> comorbidades = new ArrayList<>();
        private String alergias;
        private Map<String, Object> limitesBasais = new HashMap<>();
        private String logradouro;
        private String numero;
        private String bairro;
        private String cidade;
        private String estado;
        private String cep;
        private String planoSaude;
        private String observacoesSocorros;

        IdosoBuilder() {
        }

        public IdosoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public IdosoBuilder nomeCompleto(String nomeCompleto) {
            this.nomeCompleto = nomeCompleto;
            return this;
        }

        public IdosoBuilder dataNascimento(LocalDate dataNascimento) {
            this.dataNascimento = dataNascimento;
            return this;
        }

        public IdosoBuilder genero(String genero) {
            this.genero = genero;
            return this;
        }

        public IdosoBuilder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public IdosoBuilder fotoPerfilUrl(String fotoPerfilUrl) {
            this.fotoPerfilUrl = fotoPerfilUrl;
            return this;
        }

        public IdosoBuilder tipoSanguineo(String tipoSanguineo) {
            this.tipoSanguineo = tipoSanguineo;
            return this;
        }

        public IdosoBuilder comorbidades(List<String> comorbidades) {
            this.comorbidades = comorbidades;
            return this;
        }

        public IdosoBuilder alergias(String alergias) {
            this.alergias = alergias;
            return this;
        }

        public IdosoBuilder limitesBasais(Map<String, Object> limitesBasais) {
            this.limitesBasais = limitesBasais;
            return this;
        }

        public IdosoBuilder logradouro(String logradouro) {
            this.logradouro = logradouro;
            return this;
        }

        public IdosoBuilder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public IdosoBuilder bairro(String bairro) {
            this.bairro = bairro;
            return this;
        }

        public IdosoBuilder cidade(String cidade) {
            this.cidade = cidade;
            return this;
        }

        public IdosoBuilder estado(String estado) {
            this.estado = estado;
            return this;
        }

        public IdosoBuilder cep(String cep) {
            this.cep = cep;
            return this;
        }

        public IdosoBuilder planoSaude(String planoSaude) {
            this.planoSaude = planoSaude;
            return this;
        }

        public IdosoBuilder observacoesSocorros(String observacoesSocorros) {
            this.observacoesSocorros = observacoesSocorros;
            return this;
        }

        public Idoso build() {
            return new Idoso(id, nomeCompleto, dataNascimento, genero, cpf, fotoPerfilUrl,
                    tipoSanguineo, comorbidades, alergias, limitesBasais, logradouro,
                    numero, bairro, cidade, estado, cep, planoSaude, observacoesSocorros);
        }
    }
}
