package edu.purdue.cs352.minijava;

import java.util.*;

import edu.purdue.cs352.minijava.ast.*;

public class ASTToSExp extends ASTVisitor.SimpleASTVisitor {
    private static String escape(String str) {
        return "\"" +
            str
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"")
                .replace("\\", "\\\\")
            + "\"";
    }

    @Override public Object defaultVisit(ASTNode node) {
        String ret = "(" + node.getClass().getSimpleName();
        ASTNode[] children = node.children();
        for (ASTNode c : children) {
            ret += " " + c.accept(this);
        }
        ret += ")";
        return ret;
    }

    @Override public Object visit(BinaryExp node) {
        return "(" + node.getOp() +
               " " + node.getLeft().accept(this) +
               " " + node.getRight().accept(this) +
               ")";
    }

    @Override public Object visit(BooleanLiteralExp node) {
        return "(boolean " + node.getValue() + ")";
    }

    @Override public Object visit(CallExp node) {
        String ret = "(CallExp " +
               node.getTarget().accept(this) +
               " " + escape(node.getMethod());

        List<Exp> args = node.getArguments();
        for (Exp arg : args) {
            ret += " " + arg.accept(this);
        }

        ret += ")";
        return ret;
    }

    @Override public Object visit(ClassDecl node) {
        String eggstends = node.getExtends();
        String ret = "(ClassDecl " +
            escape(node.getName()) +
            " " + ((eggstends == null) ? "null" : escape(eggstends));

        for (ASTNode c : node.getFields()) ret += " " + c.accept(this);
        for (ASTNode c : node.getMethods()) ret += " " + c.accept(this);

        ret += ")";
        return ret;
    }

    @Override public Object visit(IntLiteralExp node) {
        return "(int " + node.getValue() + ")";
    }

    @Override public Object visit(MemberExp node) {
        return "(MemberExp " +
            node.getSub().accept(this) +
            " " + escape(node.getMember()) +
            ")";
    }

    @Override public Object visit(MethodDecl node) {
        String ret = "(MethodDecl " +
            node.getType().accept(this) +
            " " + escape(node.getName());

        // parameters
        ret += " (Parameters";
        for (Parameter c : node.getParameters()) ret += " " + c.accept(this);

        // variable declarations
        ret += ") (VarDecls";
        for (VarDecl c : node.getVarDecls()) ret += " " + c.accept(this);

        // body
        ret += ") (Statements";
        for (Statement c : node.getBody()) ret += " " + c.accept(this);

        // and return
        ret += ") (Return " + node.getRetExp().accept(this) + "))";

        return ret;
    }

    @Override public Object visit(NewObjectExp node) {
        return "(new " + escape(node.getName()) + ")";
    }

    @Override public Object visit(Parameter node) {
        return "(Parameter " +
            node.getType().accept(this) +
            " " + escape(node.getName()) +
            ")";
    }

    @Override public Object visit(Type node) {
        return "(Type " +
            escape(node.getName()) +
            ")";
    }

    @Override public Object visit(VarDecl node) {
        return "(VarDecl " +
            node.getType().accept(this) +
            " " + escape(node.getName()) +
            ")";
    }

    @Override public Object visit(VarExp node) {
        return "(VarExp " +
            escape(node.getName()) +
            ")";
    }
}
