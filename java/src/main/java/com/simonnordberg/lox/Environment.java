package com.simonnordberg.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  public Environment() {
    this.enclosing = null;
  }

  public Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token token) {
    if (values.containsKey(token.lexeme)) {
      return values.get(token.lexeme);
    }

    if (enclosing != null) {
      return enclosing.get(token);
    }

    throw new RuntimeError(token, "Undefined variable '" + token.lexeme + "'");
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  public Object getAt(Integer distance, String name) {
    return ancestor(distance).values.get(name);
  }

  private Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }

  void assign(Token token, Object value) {
    if (values.containsKey(token.lexeme)) {
      values.put(token.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(token, value);
      return;
    }

    throw new RuntimeError(token, "Undefined variable '" + token.lexeme + "'");
  }

  public void assignAt(Integer distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }
}
