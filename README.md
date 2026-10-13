# Imóvel Fácil API

Projeto Diamante de Java Advanced, desenvolvido a partir do [projeto-base](https://github.com/joaocarloslima/imovel-facil), adaptado à estrutura local com Java 25 e Spring Boot 4.2.0-M2.

## Equipe

| Nome | RM |
| --- | --- |
| Guilherme Soares | 568227 |
| Gabriel Stuani | 566682 |

## Organização

A API usa Spring MVC e está organizada em seis pacotes:

| Pacote | Responsabilidade |
| --- | --- |
| `config` | Configurações e carga inicial dos imóveis |
| `controller` | Rotas HTTP, montagem do HATEOAS e tratamento de erros |
| `dto` | Dados de entrada, filtros e respostas de erro |
| `model` | Entidade e tipos de imóvel |
| `repository` | Persistência e Specifications de busca |
| `service` | Regras de negócio, transações e cache |

## Execução

É necessário ter o JDK 25 configurado no `JAVA_HOME`. O Maven Wrapper baixa o Maven e as dependências na primeira execução.

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```sh
./mvnw spring-boot:run
```

A API inicia na porta 8080, com H2 em memória e 60 imóveis de exemplo. Os dados são recriados a cada execução. Não é necessário configurar um banco externo.

## Rotas

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/properties` | Página de imóveis |
| GET | `/properties/example` | Busca por exemplo |
| GET | `/properties/search` | Busca por Specifications |
| GET | `/properties/{id}` | Detalhe do imóvel |
| POST | `/properties` | Criação com status 201 e cabeçalho Location |
| PUT | `/properties/{id}` | Atualização do imóvel e do cache |
| DELETE | `/properties/{id}` | Exclusão com status 204 e invalidação do cache |

Listagem e buscas aceitam `page`, `size` e `sort`. O padrão é página 0, tamanho 10 e ordenação crescente por `id`. O tamanho máximo é 100.

`/properties/example` aceita `type`, `city`, `neighborhood` e `bedrooms`, com igualdade exata, ignorando caixa nos textos e filtros ausentes.

`/properties/search` aceita `title`, `city`, `minPrice`, `maxPrice`, `minBedrooms` e `minArea`. O título usa busca por trecho literal; cidade usa igualdade sem distinção de caixa; os limites numéricos são inclusivos. Filtros informados são combinados com AND.

```text
http://localhost:8080/properties?page=0&size=10
http://localhost:8080/properties/example?type=APARTMENT&city=S%C3%A3o%20Paulo&bedrooms=2
http://localhost:8080/properties/search?title=varanda&minPrice=2000&maxPrice=6000
```

## Corpo de criação e atualização

```json
{
  "title": "Apartamento com varanda",
  "description": "Perto do metrô",
  "type": "APARTMENT",
  "city": "São Paulo",
  "neighborhood": "Pinheiros",
  "price": 3200.00,
  "bedrooms": 2,
  "area": 68.5
}
```

Tipos aceitos: `APARTMENT`, `HOUSE`, `STUDIO` e `COMMERCIAL`. O preço é o aluguel mensal em reais; a área é em metros quadrados. Os erros de validação e de imóvel inexistente usam `{"error":"..."}`.

## HATEOAS e aplicativo

As respostas individuais incluem os links `self` e `properties`. As páginas usam `_embedded.properties`, metadados em `page` e links de navegação, preservando os filtros. Páginas vazias omitem `_embedded`.

O [aplicativo do professor](https://github.com/joaocarloslima/imovel-facil-app) pode consumir a API sem alterações. No Expo Go, informe o IP do computador na mesma rede; no emulador Android, use `10.0.2.2`. O CORS permite o uso do Expo Web, sem credenciais.

## Cache

O cache `properties` usa Caffeine, com até 1.000 entradas e expiração de 10 minutos. Apenas os detalhes são armazenados; listas e buscas consultam o banco.

1. Consulte `GET /properties/7`: o log `Fetching property 7 from the database` aparece.
2. Repita a consulta: o log não aparece enquanto a entrada estiver no cache.
3. Atualize com `PUT /properties/7`: o próximo GET retorna os dados novos.
4. Exclua com `DELETE /properties/7`: o próximo GET retorna 404.

## Documentação e monitoramento

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [Health](http://localhost:8080/actuator/health)
- [Info](http://localhost:8080/actuator/info)
- [Caches](http://localhost:8080/actuator/caches)

Apenas `health`, `info` e `caches` são expostos pelo Actuator, com acesso de leitura. O console H2 fica desabilitado. A API não exige autenticação, conforme o contrato do exercício.

## Verificação

```powershell
.\mvnw.cmd verify
```

O comando executa os testes e gera o JAR em `target/imovelFacil-0.0.1-SNAPSHOT.jar`.

O PDF do enunciado é exclusivamente local e está excluído pelo `.gitignore`.
