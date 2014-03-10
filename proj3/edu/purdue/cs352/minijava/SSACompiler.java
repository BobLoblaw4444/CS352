package edu.purdue.cs352.minijava;

import java.util.*;

import edu.purdue.cs352.minijava.ast.*;
import edu.purdue.cs352.minijava.ssa.*;
import edu.purdue.cs352.minijava.parser.Token;

public class SSACompiler extends ASTVisitor.SimpleASTVisitor
{
    // The method body currently being compiled
    List<SSAStatement> body = new ArrayList<SSAStatement>();

    // Create a field for local variables (symbol table)
    HashMap<String, SSAField> symbols = new HashMap<String, SSAField>();

    public static SSAProgram compile(Program prog)
    {
        SSAMethod main = compile(prog.getMain());
        List<SSAClass> classes = new ArrayList<SSAClass>();

        for (ClassDecl cl : prog.getClasses())
            classes.add(compile(cl));

        return new SSAProgram(main, classes);
    }

    public static SSAClass compile(ClassDecl cl)
    {
        List<SSAMethod> methods = new ArrayList<SSAMethod>();
        for (MethodDecl md : cl.getMethods())
            methods.add(compile(md));
        return new SSAClass(cl, methods);
    }

    public static SSAMethod compile(Main main)
    {
        SSACompiler compiler = new SSACompiler();

        // there's only a body
        main.getBody().accept(compiler);

        return new SSAMethod(main, compiler.getBody());
    }

    public static SSAMethod compile(MethodDecl method)
    {
        SSACompiler compiler = new SSACompiler();

        // visit the parameters
	ArrayList<SSAStatement> parameters = new ArrayList<SSAStatement>();
	int pos = 0;

        for(Parameter para : method.getParameters())
	{
	    parameters.add(new SSAStatement(para, SSAStatement.Op.Parameter, pos));
	    pos++;
	}

        // and the variable declarations
	ArrayList<SSAStatement> variables = new ArrayList<SSAStatement>();
        for(VarDecl var : method.getVarDecls())
	{
	    //variables.add(visit(var.));
	}
        // then compile the body
        ArrayList<SSAStatement> body = new ArrayList<SSAStatement>();
	for(Statement statement : method.getBody())
	    statement.accept(compiler);

        // and the return
        compiler.compileReturn(method.getRetExp());

        return new SSAMethod(method, compiler.getBody());
    }

    @Override public Object defaultVisit(ASTNode node)
    {
        throw new Error("Unsupported visitor in SSACompiler: " + node.getClass().getSimpleName());
    }

    // ...

  /*  @Override public Object visit(PostfixOp postfix)
    {
	 // what sort of statement we make, if any, depends on the LHS
        SSAStatement ret;
	
	if(postfix instanceof )
	{
	    SSAStatement temp = new SSAStatement(statement, SSAStatement.Op.Int, 12);
	
	    return new SSAStatement(statement, SSAStatement.Op.Print, temp);
	}
	else
        {
            throw new Error("Invalid LHS: " + statement.getClass().getSimpleName());
        }

    }
*/

    @Override public Object visit(ExpStatement expStatement)
    {
	return expStatement.getExp().accept(this);

    }
    @Override public Object visit(Exp exp)
    {   
	//Exp exp = expStatement.getExp();
	SSAStatement ret;

	if(exp instanceof AssignExp)
	{
	    ret = (SSAStatement)visit(((AssignExp)exp));
	}
	else if(exp instanceof BinaryExp)
	{
	    SSAStatement left = (SSAStatement)visit(((BinaryExp)exp).getLeft());
	    SSAStatement right = (SSAStatement)visit(((BinaryExp)exp).getRight());
	    SSAStatement.Op op = determineOp(((BinaryExp)exp).getOp());

	    ret = new SSAStatement(exp, op, left, right);
	}
	else if(exp instanceof IntLiteralExp)
	{
	    ret = new SSAStatement(exp, SSAStatement.Op.Int, null, null, ((IntLiteralExp)exp).getValue());
	}
	else
        {
            throw new Error("Invalid Expression: " + exp.getClass().getSimpleName());
        }
	return ret;
    }

    @Override public Object visit(PrintStatement statement)
    {
	 // what sort of statement we make, if any, depends on the LHS
        SSAStatement ret;
	
	if(statement instanceof PrintStatement)
	{
	    SSAStatement value = (SSAStatement)visit(((PrintStatement)statement).getValue());
	
	    ret = new SSAStatement(statement, SSAStatement.Op.Print, value);
	}
	else
        {
            throw new Error("Invalid Statement: " + statement.getClass().getSimpleName());
        }

	return ret;

    }

    @Override public Object visit(AssignExp exp)
    {
        // what sort of statement we make, if any, depends on the LHS
        Exp target = exp.getTarget();
	SSAStatement value = (SSAStatement)visit(exp.getValue());
        SSAStatement ret;
        
	if (target instanceof VarExp)
        { 
	    String name = ((VarExp)target).getName();

	    ret = new SSAStatement(exp, SSAStatement.Op.VarAssg, value, null, name);            

        }
        else if (target instanceof MemberExp)
        {
	    String name = ((MemberExp)target).getMember();
	   

	    SSAStatement member = (SSAStatement)visit(((MemberExp)target).getSub());

            ret = new SSAStatement(exp, SSAStatement.Op.MemberAssg, member, value, name);

        }
        else if (target instanceof IndexExp)
        {
	    SSAStatement array = (SSAStatement)visit(((IndexExp)target).getTarget());

	    SSAStatement index = (SSAStatement)visit(((IndexExp)target).getIndex());	    

            ret = new SSAStatement(exp, SSAStatement.Op.IndexAssg, array, value, index);

        }
        else
        {
            throw new Error("Invalid LHS: " + target.getClass().getSimpleName());
        }

        return ret;
    }
/*    @Override public Object visit(VarExp exp)
    {
	
    }
*/

    public void compileReturn(Exp retExp)
    {
        // ...
	retExp.accept(this);
	
    }

    private SSAStatement.Op determineOp(Token op)
    {
	String symbol = op.toString();
	
	if(symbol.equals("<"))
	{
	    return SSAStatement.Op.Lt;
	}
	else if(symbol.equals("<="))
	{
	    return SSAStatement.Op.Le;
	}
	else if(symbol.equals("=="))
	{
	    return SSAStatement.Op.Eq;
	}
	else if(symbol.equals("!="))
	{
	    return SSAStatement.Op.Ne;
	}
	else if(symbol.equals(">"))
	{
	    return SSAStatement.Op.Gt;
	}
	else if(symbol.equals(">="))
	{
	    return SSAStatement.Op.Ge;
	}
	else if(symbol.equals("&&"))
	{
	    return SSAStatement.Op.And;
	}
	else if(symbol.equals("||"))
	{
	    return SSAStatement.Op.Or;
	}
	else if(symbol.equals("+"))
	{
	    return SSAStatement.Op.Plus;
	}
	else if(symbol.equals("-"))
	{
	    return SSAStatement.Op.Minus;
	}
	else if(symbol.equals("*"))
	{
	    return SSAStatement.Op.Mul;
	}
	else if(symbol.equals("/"))
	{
	    return SSAStatement.Op.Div;
	}
	else if(symbol.equals("%"))
	{
	    return SSAStatement.Op.Mod;
	}

	return null;
    }

    public List<SSAStatement> getBody() { return body; }
}
