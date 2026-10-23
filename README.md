Cave Programming Language

Llenguatge de programacio simple inspirat en un entorn "caveman".

## Requisits

- Java JDK 11 o superior
- Windows, Linux o macOS amb terminal

## Instal·lacio

No cal instal·lar cap dependencia addicional, nomes Java.

Per comprovar que Java esta instal·lat:

```bash
java -version
javac -version
```

## Compilacio

Compila tot el projecte amb:

```bash
javac -d out src/lexer/*.java src/parser/*.java src/semantic/*.java src/controller/*.java src/tac/*.java src/MIPS/*.java src/app/*.java
```

Les classes compilades es generen al directori `out`.

## Execucio

El punt d'entrada es `app.Main` i admet diverses maneres d'execucio:

### 1) Sense arguments (test per defecte)

```bash
java -cp out app.Main
```

Executa `tests/valid/1.cave`.

### 2) Amb mode (`valid` o `invalid`) i numero de test

```bash
java -cp out app.Main valid 3
java -cp out app.Main invalid 7
```

Els tests disponibles son:
- `tests/valid/1.cave` fins a `tests/valid/10.cave`
- `tests/invalid/1.cave` fins a `tests/invalid/20.cave`

### 3) Amb ruta directa a un fitxer `.cave`

```bash
java -cp out app.Main tests/valid/1.cave
```

Aquest format tambe permet passar qualsevol ruta personalitzada a un fitxer `.cave`.

En una execucio correcta, el compilador mostra la taula de simbols i l'arbre sintactic.
