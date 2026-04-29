# SCA Hazelcast OpenShift MVP

MVP funcional de um serviço **Spring Boot 3.4.5** usando **Hazelcast embedded 5.5.0** para compartilhar o `IMap` chamado `scaContext` entre múltiplos pods no **OpenShift/Kubernetes**.

A proposta é permitir que um serviço stateless, executado em mais de um pod, consiga manter e recuperar dados transitórios de SSO em um cache distribuído comum.

> Ideia central: cada pod sobe uma instância Hazelcast embedded e todas essas instâncias formam um único cluster. Assim, o map `scaContext` passa a ser distribuído entre os pods.

---

## 1. Problema resolvido

Antes:

```text
Pod 1 -> Hazelcast local -> scaContext local
Pod 2 -> Hazelcast local -> scaContext local
Pod 3 -> Hazelcast local -> scaContext local
```

Cada pod tinha seu próprio map isolado. Um dado gravado no Pod 1 não era necessariamente visível no Pod 2.

Depois:

```text
Pod 1 ┐
Pod 2 ├── Hazelcast Cluster embedded ── IMap scaContext
Pod 3 ┘
```

Todos os pods formam um cluster Hazelcast e compartilham o mesmo `IMap<String, ScaContext>`.

---

## 2. Componentes da solução

| Componente | Responsabilidade |
|---|---|
| Spring Boot | Serviço REST da aplicação |
| Hazelcast embedded | Cache distribuído dentro dos próprios pods |
| `IMap scaContext` | Armazena contexto transitório de SSO |
| Service headless | Permite discovery entre pods Hazelcast |
| Deployment OpenShift | Executa múltiplas réplicas da aplicação |
| Route OpenShift | Expõe a API HTTP externamente |
| PodDisruptionBudget | Evita indisponibilidade durante manutenções |

---

## 3. Estrutura do projeto

```text
sca-hazelcast-openshift-mvp/
├── pom.xml
├── Dockerfile
├── README.md
├── src/
│   ├── main/
│   │   ├── java/br/com/mprj/sca/
│   │   │   ├── ScaHazelcastOpenShiftApplication.java
│   │   │   ├── api/
│   │   │   │   ├── ClusterController.java
│   │   │   │   └── ScaContextController.java
│   │   │   ├── config/
│   │   │   │   ├── HazelcastConfig.java
│   │   │   │   └── HazelcastProperties.java
│   │   │   └── context/
│   │   │       ├── ScaContext.java
│   │   │       ├── ScaContextRequest.java
│   │   │       └── ScaContextService.java
│   │   └── resources/application.yml
│   └── test/java/br/com/mprj/sca/context/ScaContextServiceTest.java
├── openshift/
│   ├── 01-service-hazelcast-headless.yml
│   ├── 02-service-http.yml
│   ├── 03-deployment.yml
│   ├── 04-route.yml
│   └── 05-pod-disruption-budget.yml
├── scripts/
│   ├── build-local.sh
│   └── deploy-openshift.sh
└── .github/workflows/build.yml
```

---

## 4. Como o Hazelcast forma cluster no OpenShift

O ponto mais importante da solução é o arquivo:

```text
openshift/01-service-hazelcast-headless.yml
```

Ele cria um **Service headless**:

```yaml
clusterIP: None
```

Com isso, o DNS interno do Kubernetes/OpenShift consegue resolver os IPs dos pods associados ao label:

```yaml
selector:
  app: sca-service
```

O Hazelcast usa esse service para descobrir os outros membros do cluster.

Configuração usada pela aplicação:

```yaml
hazelcast:
  cluster-name: sca-cluster
  map-name: scaContext
  kubernetes:
    enabled: true
    service-name: sca-service-hazelcast
    namespace: ${OPENSHIFT_NAMESPACE:default}
```

No OpenShift, a variável `OPENSHIFT_NAMESPACE` é preenchida automaticamente a partir do namespace do pod:

```yaml
- name: OPENSHIFT_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
```

---

## 5. Configuração do map `scaContext`

O map foi configurado com:

| Propriedade | Valor padrão | Finalidade |
|---|---:|---|
| `backup-count` | `1` | Mantém uma cópia de backup em outro membro |
| `ttl-seconds` | `1800` | Expira registros após 30 minutos |
| `max-idle-seconds` | `900` | Expira registros sem uso após 15 minutos |
| `max-size-per-node` | `10000` | Limita tamanho por pod |
| `eviction-policy` | `LRU` | Remove entradas menos usadas |

Esses valores podem ser ajustados via variáveis de ambiente:

```text
HZ_MAP_TTL_SECONDS
HZ_MAP_MAX_IDLE_SECONDS
HZ_MAP_BACKUP_COUNT
HZ_MAP_MAX_SIZE_PER_NODE
```

---

## 6. Observação importante sobre SSO e tokens

Este MVP usa os nomes:

```java
accessTokenReference
refreshTokenReference
```

A recomendação é **não armazenar tokens puros** no Hazelcast, principalmente se forem tokens JWT ou refresh tokens sensíveis.

Preferível:

- armazenar uma referência opaca;
- criptografar o valor antes de salvar;
- usar TTL curto;
- remover o contexto no logout;
- auditar acessos sensíveis.

Hazelcast é rápido, mas não é cofre de banco suíço. É cache distribuído — poderoso, mas precisa de cinto de segurança.

---

## 7. Endpoints disponíveis

### 7.1 Verificar cluster

```http
GET /api/cluster
```

Resposta esperada com 3 pods:

```json
{
  "pod": "sca-service-xxxx",
  "clusterName": "sca-cluster",
  "membersCount": 3,
  "members": [
    "10.128.0.10:5701",
    "10.128.0.11:5701",
    "10.128.0.12:5701"
  ]
}
```

Se `membersCount` vier como `1`, os pods não estão formando cluster.

---

### 7.2 Criar contexto SSO

```http
POST /api/sca-context/{sessionId}
```

Exemplo:

```bash
curl -X POST http://localhost:8080/api/sca-context/ABC123 \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "marcao",
    "accessTokenReference": "access-ref-001",
    "refreshTokenReference": "refresh-ref-001",
    "attributes": {
      "perfil": "ADMIN",
      "origem": "SSO"
    }
  }'
```

---

### 7.3 Consultar contexto SSO

```http
GET /api/sca-context/{sessionId}
```

Exemplo:

```bash
curl http://localhost:8080/api/sca-context/ABC123
```

---

### 7.4 Atualizar contexto SSO

```http
PUT /api/sca-context/{sessionId}
```

---

### 7.5 Remover contexto SSO

```http
DELETE /api/sca-context/{sessionId}
```

Use este endpoint no logout para limpar o contexto da sessão.

---

## 8. Executando localmente

```bash
mvn clean package
java -jar target/sca-service.jar
```

Por padrão, localmente o Kubernetes discovery fica desligado:

```yaml
HZ_KUBERNETES_ENABLED=false
```

Teste:

```bash
curl http://localhost:8080/api/cluster
```

---

## 9. Build da imagem Docker

```bash
mvn clean package -DskipTests
docker build -t sca-service:latest .
```

---

## 10. Deploy no OpenShift

Ajuste a imagem no arquivo:

```text
openshift/03-deployment.yml
```

Trecho atual:

```yaml
image: image-registry.openshift-image-registry.svc:5000/sca-dev/sca-service:latest
```

Depois aplique os manifests:

```bash
oc project sca-dev

oc apply -f openshift/01-service-hazelcast-headless.yml
oc apply -f openshift/02-service-http.yml
oc apply -f openshift/03-deployment.yml
oc apply -f openshift/04-route.yml
oc apply -f openshift/05-pod-disruption-budget.yml
```

Verifique os pods:

```bash
oc get pods -l app=sca-service -o wide
```

Verifique logs:

```bash
oc logs -l app=sca-service --tail=200
```

---

## 11. Teste de cluster no OpenShift

Obtenha a route:

```bash
oc get route sca-service
```

Teste o cluster:

```bash
curl https://SUA-ROTA/api/cluster
```

Crie um contexto:

```bash
curl -X POST https://SUA-ROTA/api/sca-context/ABC123 \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "marcao",
    "accessTokenReference": "access-ref-001",
    "refreshTokenReference": "refresh-ref-001",
    "attributes": {
      "perfil": "ADMIN"
    }
  }'
```

Consulte várias vezes:

```bash
curl https://SUA-ROTA/api/sca-context/ABC123
```

Você deve observar:

```json
"clusterMembers": 3
```

E o contexto deve ser encontrado mesmo quando a requisição cair em pods diferentes.

---

## 12. Troubleshooting

### Problema: `clusterMembers = 1`

Verifique:

1. O Service headless existe?

```bash
oc get svc sca-service-hazelcast -o yaml
```

2. Os labels batem com o selector?

```bash
oc get pods --show-labels
```

O pod precisa ter:

```text
app=sca-service
```

3. A porta 5701 está exposta no container e no service?

```yaml
containerPort: 5701
```

4. A variável `HZ_KUBERNETES_ENABLED` está `true`?

```bash
oc describe pod POD_NAME | grep HZ_KUBERNETES_ENABLED -A2
```

5. O namespace está correto?

```bash
oc project
```

---

### Problema: dados somem quando pods reiniciam

Isso é esperado em Hazelcast embedded. O dado fica em memória nos pods.

Para reduzir risco:

- use `backup-count >= 1`;
- use pelo menos 3 réplicas;
- configure `PodDisruptionBudget`;
- evite derrubar todos os pods ao mesmo tempo.

Para persistência real, considere Hazelcast Enterprise com persistence, Redis, banco de dados ou Hazelcast em modo client/server separado da aplicação.

---

### Problema: autoscaling causando instabilidade

Evite HPA muito agressivo para workloads com cache embedded.

Sugestão:

- mínimo de 3 pods;
- escala gradual;
- readiness probe bem configurada;
- não usar scale-to-zero.

---

## 13. Quando usar embedded e quando separar Hazelcast

### Embedded é bom quando:

- o volume de dados é moderado;
- o dado é transitório;
- perda em restart total é aceitável;
- você quer simplicidade operacional;
- a aplicação e o cache têm ciclo de vida parecido.

### Hazelcast separado é melhor quando:

- o cache é crítico;
- há muitos serviços consumidores;
- você quer escalar aplicação e cache separadamente;
- precisa de governança operacional melhor;
- quer reduzir impacto de restart da aplicação no cluster.

---

## 14. Próximos passos recomendados

Para evoluir este MVP para produção:

1. Criar um `HealthIndicator` específico para verificar quantidade mínima de membros Hazelcast.
2. Criptografar dados sensíveis antes de gravar no map.
3. Integrar remoção do `scaContext` ao fluxo real de logout do SSO.
4. Adicionar métricas Micrometer para tamanho do map, hits, misses e remoções.
5. Criar testes de integração com Testcontainers ou ambiente Kubernetes de teste.
6. Avaliar Hazelcast client/server se o ambiente crescer.

---

## 15. Comandos úteis

```bash
# Build local
./scripts/build-local.sh

# Deploy OpenShift
./scripts/deploy-openshift.sh sca-dev sca-service

# Ver pods
oc get pods -l app=sca-service -o wide

# Ver serviços
oc get svc

# Ver route
oc get route sca-service

# Escalar manualmente
oc scale deployment/sca-service --replicas=3

# Logs agregados
oc logs -l app=sca-service --tail=200
```

---

## 16. Resumo

Este MVP entrega:

- aplicação Spring Boot funcional;
- Hazelcast embedded configurado;
- map distribuído `scaContext`;
- discovery Kubernetes/OpenShift;
- manifests OpenShift completos;
- endpoints REST para teste;
- Dockerfile;
- scripts de build/deploy;
- teste unitário básico;
- GitHub Actions para build.

Com isso, múltiplos pods do serviço conseguem compartilhar o mesmo contexto SSO em memória distribuída.



### Testes
curl -X POST http://localhost:8081/api/sca-context/ABC123 -H 'Content-Type: application/json'  -d '{ "username": "marcao","accessTokenReference": "access-ref-001","refreshTokenReference": "refresh-ref-001", "attributes": { "perfil": "ADMIN"}}'
curl -X POST https://sca-hazelcast-openshift-mvp-mprj-marco-carvalho-dev.apps.rm1.0a51.p1.openshiftapps.com/api/sca-context/ABC123 -H 'Content-Type: application/json'  -d '{ "username": "marcao","accessTokenReference": "access-ref-001","refreshTokenReference": "refresh-ref-001", "attributes": { "perfil": "ADMIN"}}'

curl https://sca-hazelcast-openshift-mvp-mprj-marco-carvalho-dev.apps.rm1.0a51.p1.openshiftapps.com/api/sca-context/ABC123
