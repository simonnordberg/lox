package com.simonnordberg.lox;

import com.simonnordberg.lox.Expr.Binary;
import com.simonnordberg.lox.Expr.Grouping;
import com.simonnordberg.lox.Expr.Literal;
import com.simonnordberg.lox.Expr.Unary;

public class AstPrinter implements Expr.Visitor<String> {

  public static void main(String[] args) {
    Expr expression = new Binary(
        new Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Grouping(new Literal(45.67)));
    System.out.println(new AstPrinter().print(expression));
  }

  public String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    return expr.value != null ? expr.value.toString() : "nil";
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  private String parenthesize(String name, Expr... expressions) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : expressions) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }
}
