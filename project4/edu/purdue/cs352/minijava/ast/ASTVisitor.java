package edu.purdue.cs352.minijava.ast;

public abstract class ASTVisitor {
    public abstract Object visit(AssignExp node);
    public abstract Object visit(ASTNode node);
    public abstract Object visit(BinaryExp node);
    public abstract Object visit(BlockStatement node);
    public abstract Object visit(BooleanLiteralExp node);
    public abstract Object visit(CallExp node);
    public abstract Object visit(ClassDecl node);
    public abstract Object visit(Exp node);
    public abstract Object visit(ExpStatement node);
    public abstract Object visit(IfStatement node);
    public abstract Object visit(IndexExp node);
    public abstract Object visit(IntLiteralExp node);
    public abstract Object visit(Main node);
    public abstract Object visit(MemberExp node);
    public abstract Object visit(MethodDecl node);
    public abstract Object visit(NewIntArrayExp node);
    public abstract Object visit(NewObjectExp node);
    public abstract Object visit(NotExp node);
    public abstract Object visit(Parameter node);
    public abstract Object visit(PrintStatement node);
    public abstract Object visit(Program node);
    public abstract Object visit(Statement node);
    public abstract Object visit(ThisExp node);
    public abstract Object visit(TypeBoolean node);
    public abstract Object visit(TypeIntArray node);
    public abstract Object visit(TypeInt node);
    public abstract Object visit(Type node);
    public abstract Object visit(VarDecl node);
    public abstract Object visit(VarExp node);
    public abstract Object visit(WhileStatement node);

    public static class SimpleASTVisitor extends ASTVisitor {
        public Object defaultVisit(ASTNode node) {
            ASTNode[] children = node.children();
            Object[] ret = new Object[children.length];

            for (int i = 0; i < children.length; i++) {
                ret[i] = children[i].accept(this);
            }

            return ret;
        }

        @Override public Object visit(AssignExp node) { return defaultVisit(node); }
        @Override public Object visit(ASTNode node) { return defaultVisit(node); }
        @Override public Object visit(BinaryExp node) { return defaultVisit(node); }
        @Override public Object visit(BlockStatement node) { return defaultVisit(node); }
        @Override public Object visit(BooleanLiteralExp node) { return defaultVisit(node); }
        @Override public Object visit(CallExp node) { return defaultVisit(node); }
        @Override public Object visit(ClassDecl node) { return defaultVisit(node); }
        @Override public Object visit(Exp node) { return defaultVisit(node); }
        @Override public Object visit(ExpStatement node) { return defaultVisit(node); }
        @Override public Object visit(IfStatement node) { return defaultVisit(node); }
        @Override public Object visit(IndexExp node) { return defaultVisit(node); }
        @Override public Object visit(IntLiteralExp node) { return defaultVisit(node); }
        @Override public Object visit(Main node) { return defaultVisit(node); }
        @Override public Object visit(MemberExp node) { return defaultVisit(node); }
        @Override public Object visit(MethodDecl node) { return defaultVisit(node); }
        @Override public Object visit(NewIntArrayExp node) { return defaultVisit(node); }
        @Override public Object visit(NewObjectExp node) { return defaultVisit(node); }
        @Override public Object visit(NotExp node) { return defaultVisit(node); }
        @Override public Object visit(Parameter node) { return defaultVisit(node); }
        @Override public Object visit(PrintStatement node) { return defaultVisit(node); }
        @Override public Object visit(Program node) { return defaultVisit(node); }
        @Override public Object visit(Statement node) { return defaultVisit(node); }
        @Override public Object visit(ThisExp node) { return defaultVisit(node); }
        @Override public Object visit(TypeBoolean node) { return defaultVisit(node); }
        @Override public Object visit(TypeIntArray node) { return defaultVisit(node); }
        @Override public Object visit(TypeInt node) { return defaultVisit(node); }
        @Override public Object visit(Type node) { return defaultVisit(node); }
        @Override public Object visit(VarDecl node) { return defaultVisit(node); }
        @Override public Object visit(VarExp node) { return defaultVisit(node); }
        @Override public Object visit(WhileStatement node) { return defaultVisit(node); }
    }
}
