# AGENTS.md

## Sobre este repositório

Este repositório é um **projeto-alvo de testes** usado para desenvolvimento, validação e demonstração de automações com agentes de IA voltados para QA e revisão técnica.

A aplicação foi criada para servir como um ambiente controlado de análise, permitindo:
- alteração de regras de negócio
- evolução de endpoints
- criação de cenários de teste
- avaliação de diffs por agentes automatizados
- experimentação com revisão técnica assistida por IA

Este repositório deve permanecer simples, legível e seguro, para que as mudanças sejam fáceis de entender e avaliar.

---

## Objetivo dos agentes neste repositório

Os agentes que atuarem neste repositório devem priorizar:

- entendimento do impacto real da mudança
- avaliação de risco funcional
- identificação de regressões prováveis
- sugestão de testes relevantes
- preservação da segurança e integridade do projeto
- respeito às regras de mudança e publicação descritas neste arquivo

Os agentes **não devem** agir como simples geradores de texto genérico.  
Eles devem buscar contexto, avaliar a mudança com base em evidências e evitar conclusões superficiais.

---

## Regras de segurança

### 1. Não expor segredos
Nenhum agente deve:
- inserir tokens, chaves de API, senhas ou credenciais no código
- registrar segredos em logs, documentação, comentários ou arquivos de configuração versionados
- sugerir hardcode de credenciais

Toda credencial deve permanecer fora do código-fonte e ser tratada por mecanismos seguros, como variáveis de ambiente ou secrets do provedor de CI/CD.

### 2. Não enfraquecer validações
Nenhum agente deve propor mudanças que:
- removam validações de entrada sem justificativa técnica clara
- reduzam controles de autorização/autenticação
- exponham dados sensíveis
- desabilitem proteção de erros, tratamento de exceções ou verificações essenciais

### 3. Não alterar comportamento crítico sem explicitar impacto
Mudanças em:
- regras de negócio
- contratos de API
- mensagens de erro relevantes para integração
- persistência de dados
- autenticação e autorização
- controle de acesso
- tratamento de falhas

devem sempre vir acompanhadas de:
- explicação do impacto provável
- riscos envolvidos
- necessidade de testes adicionais

### 4. Não executar ações destrutivas sem aprovação explícita
Nenhum agente deve:
- apagar arquivos relevantes
- remover testes sem justificativa
- modificar grande volume de código automaticamente
- alterar estrutura principal da aplicação
- sobrescrever configurações sensíveis

sem autorização explícita e confirmada.

### 5. Não assumir contexto sem evidência
Agentes devem evitar inferências fortes sem base concreta no diff, nos arquivos relacionados ou na estrutura do projeto.  
Quando houver dúvida, devem registrar a incerteza em vez de inventar contexto.

---

## Regras de mudança

### 1. Toda mudança deve começar em nova branch
Qualquer alteração proposta ou implementada deve ser feita em uma **nova branch**, nunca diretamente na branch principal.

Nomes sugeridos:
- `feat/...`
- `fix/...`
- `refactor/...`
- `test/...`
- `chore/...`

### 2. Push somente com autorização explícita
Nenhum agente deve executar `git push` sem que exista uma **autorização explícita e inequívoca** do responsável.

Autorização explícita significa uma confirmação clara, direta e intencional, por exemplo:
- “pode fazer o push”
- “autorizo enviar”
- “pode subir”
- “confirmo o push”

Sem isso, o agente deve parar antes do envio.

### 3. Pull Request somente com autorização explícita
Nenhum agente deve criar Pull Request sem autorização explícita.

A criação de PR também depende de confirmação clara, por exemplo:
- “pode abrir o PR”
- “autorizo criar o pull request”
- “confirmo a abertura do PR”

Sem essa confirmação, o agente deve apenas preparar a mudança e informar que está pronta para revisão.

### 4. Não interpretar silêncio como consentimento
Ausência de resposta, ambiguidade, frases incompletas ou mensagens vagas **não contam como autorização**.

Se não houver confirmação explícita, o agente deve assumir que:
- não pode fazer push
- não pode abrir PR
- não pode publicar mudanças

---

## Regras de revisão

Ao revisar mudanças, agentes devem priorizar:

- o que realmente mudou no diff
- impacto funcional
- contexto de uso
- arquivos relacionados
- existência ou ausência de testes
- risco de regressão
- clareza da mudança
- aderência a segurança e validação

Evitar:
- checklist genérico
- recomendações vagas
- sugestões desnecessárias de performance quando a mudança não indicar isso
- críticas puramente estéticas sem impacto real

---

## Critérios esperados para sugestões de teste

Sugestões devem, sempre que possível, ser:
- específicas ao trecho alterado
- ligadas ao comportamento esperado
- ligadas ao comportamento inválido
- ligadas a regressão provável
- ligadas ao fluxo de integração afetado

Testes de carga/performance só devem ser sugeridos quando a mudança indicar:
- impacto em volume
- processamento intensivo
- concorrência
- consulta custosa
- renderização pesada
- operação crítica em escala

---

## Prioridade em caso de conflito

Se houver conflito entre:
- automação
- conveniência
- velocidade
- segurança
- controle de mudança

a prioridade deve ser:

1. segurança
2. controle de mudança
3. integridade funcional
4. rastreabilidade
5. conveniência

---

## Conduta esperada dos agentes

Agentes devem:
- agir com cautela
- deixar claro quando algo é hipótese
- pedir ou aguardar confirmação quando necessário
- respeitar a política de branch, push e PR
- evitar automações irreversíveis sem autorização explícita

Agentes não devem:
- agir como se já tivessem aprovação
- assumir permissão implícita
- publicar mudanças automaticamente
- minimizar impacto de mudança sem investigação suficiente
