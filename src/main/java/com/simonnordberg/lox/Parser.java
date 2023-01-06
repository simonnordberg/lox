package com.simonnordberg.lox;

import static com.simonnordberg.lox.TokenType.BANG;
import static com.simonnordberg.lox.TokenType.BANG_EQUAL;
import static com.simonnordberg.lox.TokenType.EOF;
import static com.simonnordberg.lox.TokenType.EQUAL_EQUAL;
import static com.simonnordberg.lox.TokenType.FALSE;
import static com.simonnordberg.lox.TokenType.GREATER;
import static com.simonnordberg.lox.TokenType.GREATER_EQUAL;
import static com.simonnordberg.lox.TokenType.LEFT_PAREN;
import static com.simonnordberg.lox.TokenType.LESS;
import static com.simonnordberg.lox.TokenType.LESS_EQUAL;
import static com.simonnordberg.lox.TokenType.MINUS;
import static com.simonnordberg.lox.TokenType.NIL;
import static com.simonnordberg.lox.TokenType.NUMBER;
import static com.simonnordberg.lox.TokenType.PLUS;
import static com.simonnordberg.lox.TokenType.RIGHT_PAREN;
import static com.simonnordberg.lox.TokenType.SEMICOLON;
import static com.simonnordberg.lox.TokenType.SLASH;
import static com.simonnordberg.lox.TokenType.STAR;
import static com.simonnordberg.lox.TokenType.STRING;
import static com.simonnordberg.lox.TokenType.TRUE;

import com.simonnordberg.lox.Expr.Binary;
import com.simonnordberg.lox.Expr.Grouping;
import com.simonnordberg.lox.Expr.Literal;
import com.simonnordberg.lox.Expr.Unary;
import java.util.List;

/**
 *
 * Expression Grammar:
 * expression     → equality ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                | primary ;
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")" ;
 */

public class Parser {

  private final List<Token> tokens;
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return equality();
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
    return primary();
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
