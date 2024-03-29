package com.simonnordberg.lox;

import static com.simonnordberg.lox.TokenType.AND;
import static com.simonnordberg.lox.TokenType.BANG;
import static com.simonnordberg.lox.TokenType.BANG_EQUAL;
import static com.simonnordberg.lox.TokenType.CLASS;
import static com.simonnordberg.lox.TokenType.COMMA;
import static com.simonnordberg.lox.TokenType.DOT;
import static com.simonnordberg.lox.TokenType.ELSE;
import static com.simonnordberg.lox.TokenType.EOF;
import static com.simonnordberg.lox.TokenType.EQUAL;
import static com.simonnordberg.lox.TokenType.EQUAL_EQUAL;
import static com.simonnordberg.lox.TokenType.FALSE;
import static com.simonnordberg.lox.TokenType.FOR;
import static com.simonnordberg.lox.TokenType.FUN;
import static com.simonnordberg.lox.TokenType.GREATER;
import static com.simonnordberg.lox.TokenType.GREATER_EQUAL;
import static com.simonnordberg.lox.TokenType.IDENTIFIER;
import static com.simonnordberg.lox.TokenType.IF;
import static com.simonnordberg.lox.TokenType.LEFT_BRACE;
import static com.simonnordberg.lox.TokenType.LEFT_PAREN;
import static com.simonnordberg.lox.TokenType.LESS;
import static com.simonnordberg.lox.TokenType.LESS_EQUAL;
import static com.simonnordberg.lox.TokenType.MINUS;
import static com.simonnordberg.lox.TokenType.NIL;
import static com.simonnordberg.lox.TokenType.NUMBER;
import static com.simonnordberg.lox.TokenType.OR;
import static com.simonnordberg.lox.TokenType.PLUS;
import static com.simonnordberg.lox.TokenType.PRINT;
import static com.simonnordberg.lox.TokenType.RETURN;
import static com.simonnordberg.lox.TokenType.RIGHT_BRACE;
import static com.simonnordberg.lox.TokenType.RIGHT_PAREN;
import static com.simonnordberg.lox.TokenType.SEMICOLON;
import static com.simonnordberg.lox.TokenType.SLASH;
import static com.simonnordberg.lox.TokenType.STAR;
import static com.simonnordberg.lox.TokenType.STRING;
import static com.simonnordberg.lox.TokenType.SUPER;
import static com.simonnordberg.lox.TokenType.THIS;
import static com.simonnordberg.lox.TokenType.TRUE;
import static com.simonnordberg.lox.TokenType.VAR;
import static com.simonnordberg.lox.TokenType.WHILE;

import com.simonnordberg.lox.Expr.Binary;
import com.simonnordberg.lox.Expr.Get;
import com.simonnordberg.lox.Expr.Grouping;
import com.simonnordberg.lox.Expr.Literal;
import com.simonnordberg.lox.Expr.Super;
import com.simonnordberg.lox.Expr.This;
import com.simonnordberg.lox.Expr.Unary;
import com.simonnordberg.lox.Expr.Variable;
import com.simonnordberg.lox.Stmt.Block;
import com.simonnordberg.lox.Stmt.Class;
import com.simonnordberg.lox.Stmt.If;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Expression Grammar:
 * program        → declaration* EOF ;
 * declaration    → classDecl
 *                | funDecl
 *                | varDecl
 *                | statement ;
 * classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
 *                  "{" function* "}" ;
 * funDecl        → "fun" function ;
 * function       → IDENTIFIER "(" parameters? ")" block ;
 * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement      → exprStmt
 *                | forStmt
 *                | ifStmt
 *                | printStmt
 *                | returnStmt
 *                | whileStmt
 *                | block ;
 * exprStmt       → expression ";" ;
 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
 *                  expression? ";"
 *                  expression? ")" statement ;
 * ifStmt         → "if" "(" expression ")" statement
 *                ( "else" statement )? ;
 * printStmt      → "print" expression ";" ;
 * returnStmt     → "return" expression? ";" ;
 * whileStmt      → "while" "(" expression ")" statement ;
 * block          → "{" declaration* "}" ;
 * expression     → assignment ;
 * assignment     → ( call "." )? IDENTIFIER "=" assignment
 *                | logic_or ;
 * logic_or       → logic_and ( "or" logic_and )* ;
 * logic_and      → equality ( "and" equality )* ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary | call ;
 * unary          → ( "!" | "-" ) unary
 * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
 * arguments      → expression ( "," expression )* ;
 * primary        → "true" | "false" | "nil" | "this"
 *                | NUMBER | STRING | IDENTIFIER | "(" expression ")"
 *                | "super" "." IDENTIFIER ;
 */

public class Parser {

  private final List<Token> tokens;
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      // Trick to move beyond a single token lookahead
      Token equals = previous();
      Expr value = assignment();

      // Support
      if (expr instanceof Expr.Variable) {
        Token name = ((Variable) expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Get) {
        Expr.Get get = (Get) expr;
        return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target");
    }
    return expr;
  }

  private Expr or() {
    Expr expr = and();
    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr and() {
    Expr expr = equality();
    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Stmt declaration() {
    try {
      if (match(CLASS)) {
        return classDeclaration();
      }
      if (match(FUN)) {
        return function("function");
      }
      if (match(VAR)) {
        return varDeclaration();
      }

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name");

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name");
      superclass = new Expr.Variable(previous());
    }

    consume(LEFT_BRACE, "Expect '{' before class body");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' after class body");
    return new Class(name, superclass, methods);
  }

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name");
    consume(LEFT_PAREN, "Expect '(' after " + kind + "name");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters");
        }
        parameters.add(consume(IDENTIFIER, "Expect parameter name"));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters");
    consume(LEFT_BRACE, "Expect '{' before " + kind + " body");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name");
    Expr initializer = match(EQUAL) ? expression() : null;
    consume(SEMICOLON, "Expect ';' after variable declaration");
    return new Stmt.Var(name, initializer);
  }

  private Stmt statement() {
    if (match(FOR)) {
      return forStatement();
    }
    if (match(IF)) {
      return ifStatement();
    }
    if (match(PRINT)) {
      return printStatement();
    }
    if (match(RETURN)) {
      return returnStatement();
    }
    if (match(WHILE)) {
      return whileStatement();
    }
    if (match(LEFT_BRACE)) {
      return new Block(block());
    }
    return expressionStatement();
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = !check(SEMICOLON) ? expression() : null;
    consume(SEMICOLON, "Expect ';' after return value");
    return new Stmt.Return(keyword, value);
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'");
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = !check(SEMICOLON) ? expression() : null;
    consume(SEMICOLON, "Expect ';' after loop condition");

    Expr increment = !check(RIGHT_PAREN) ? expression() : null;
    consume(RIGHT_PAREN, "Expect ')' after for clauses");

    Stmt body = statement();

    // Use existing primitives to create the forLoop
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }

    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after while condition");
    Stmt body = statement();
    return new Stmt.While(condition, body);
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition");

    Stmt thenBranch = statement();
    Stmt elseBranch = match(ELSE) ? statement() : null;
    return new If(condition, thenBranch, elseBranch);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    consume(RIGHT_BRACE, "Expect '}' after block");
    return statements;
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value");
    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after value");
    return new Stmt.Expression(expr);
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Unary(operator, right);
    }
    return call();
  }

  private Expr call() {
    Expr expr = primary();
    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "Expect property name after '.'");
        expr = new Get(expr, name);
      } else {
        break;
      }
    }
    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }
    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");
    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (match(FALSE)) {
      return new Literal(false);
    }
    if (match(TRUE)) {
      return new Literal(true);
    }
    if (match(NIL)) {
      return new Literal(null);
    }
    if (match(NUMBER, STRING)) {
      return new Literal(previous().literal);
    }
    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'");
      Token method = consume(IDENTIFIER, "Expect superclass method name");
      return new Super(keyword, method);
    }
    if (match(THIS)) {
      return new This(previous());
    }
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression");
      return new Grouping(expr);
    }
    throw error(peek(), "Expect expression");
  }

  // Helpers below
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);

  }

  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }
      advance();
    }
  }

  private static class ParseError extends RuntimeException {

  }
}
