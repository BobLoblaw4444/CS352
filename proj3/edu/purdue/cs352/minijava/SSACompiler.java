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
    HashMap<String, SSAStatement> symbolTable = new HashMap<String, SSAStatement>();

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
	int idx = 0;
	ArrayList<SSAField> variables = new ArrayList<SSAField>();
        for(VarDecl var : method.getVarDecls())
	{
	    var.accept(compiler);
	    //variables.add(new SSAField(var, var.getName(), idx));
	    //idx++;
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

    @Override public Object visit(VarDecl varDecl)
    {
	SSAStatement ret;

	ret = new SSAStatement(varDecl, SSAStatement.Op.Null, varDecl.getType());
	
	symbolTable.put(varDecl.getName(), ret);
	
	this.body.add(ret);
	return ret;
    }

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
	    ret = (SSAStatement)((AssignExp)exp).accept(this);
	}
	else if(exp instanceof BinaryExp)
	{
	    SSAStatement left = (SSAStatement)((BinaryExp)exp).getLeft().accept(this);
	    SSAStatement right = (SSAStatement)((BinaryExp)exp).getRight().accept(this);
	    SSAStatement.Op op = determineOp(((BinaryExp)exp).getOp());

	    ret = new SSAStatement(exp, op, left, right);
	}
	else if(exp instanceof IntLiteralExp)
	{
	    ret = (SSAStatement)((IntLiteralExp)exp).accept(this);
	}
	else
        {
            throw new Error("Invalid Expression: " + exp.getClass().getSimpleName());
        }
	this.body.add(ret);
	return ret;
    }

    @Override public Object visit(PrintStatement statement)
    {
	 // what sort of statement we make, if any, depends on the LHS
        SSAStatement ret;
	
	if(statement instanceof PrintStatement)
	{
	    SSAStatement value = (SSAStatement)((PrintStatement)statement).getValue().accept(this);
	
	    ret = new SSAStatement(statement, SSAStatement.Op.Print, value, null, null);
	}
	else
        {
            throw new Error("Invalid Statement: " + statement.getClass().getSimpleName());
        }

	this.body.add(ret);
	return ret;

    }

    @Override public Object visit(AssignExp exp)
    {
        // what sort of statement we make, if any, depends on the LHS
        Exp target = exp.getTarget();
	SSAStatement value = (SSAStatement)exp.getValue().accept(this);
        SSAStatement ret;
        
	if (target instanceof VarExp)
        { 
	    String name = ((VarExp)target).getName();

	    ret = new SSAStatement(exp, SSAStatement.Op.VarAssg, value, null, name);            
	    
	    this.symbolTable.put(name, ret);
        }
        else if (target instanceof MemberExp)
        {
	    String name = ((MemberExp)target).getMember();
	   
	    SSAStatement member = (SSAStatement)((MemberExp)target).getSub().accept(this);

            ret = new SSAStatement(exp, SSAStatement.Op.MemberAssg, member, value, name);

        }
        else if (target instanceof IndexExp)
        {
	    SSAStatement array = (SSAStatement)((IndexExp)target).getTarget().accept(this);
	    SSAStatement index = (SSAStatement)((IndexExp)target).getIndex().accept(this);	    
            ret = new SSAStatement(exp, SSAStatement.Op.IndexAssg, array, value, index);

        }
        else
        {
            throw new Error("Invalid LHS: " + target.getClass().getSimpleName());
        }

	this.body.add(ret);
        return ret;
    }

    // CallExp
    @Override public Object visit(CallExp callExp)
    {
	SSAStatement ret;

	SSAStatement target = (SSAStatement)(callExp.getTarget().accept(this));

	// Build an SSAStatement argument list from Exp list
	ArrayList<SSAStatement> args = new ArrayList<SSAStatement>();
	for(Exp argument : callExp.getArguments())
	{
	    args.add((SSAStatement)argument.accept(this));
	}

	SSACall ssaCall = new SSACall(callExp.getMethod(), args);

	ret = new SSAStatement(callExp, SSAStatement.Op.Call, target, null, ssaCall);

	this.body.add(ret);
	return ret;
    }

    // IntLiteral
    @Override public Object visit(IntLiteralExp intLit)
    {
	SSAStatement ret = new SSAStatement(intLit, SSAStatement.Op.Int, intLit.getValue());
	
	this.body.add(ret);
	return ret;
    }

    // BooleanLiteral
    @Override public Object visit(BooleanLiteralExp boolLit)
    {
	SSAStatement ret = new SSAStatement(boolLit, SSAStatement.Op.Boolean, boolLit.getValue());
	
	this.body.add(ret);
	return ret;
    }

    // VarExp
    @Override public Object visit(VarExp var)
    {
	SSAStatement ret = this.symbolTable.get(var.getName());
	
	if(ret == null)
	{ 
	    ret = new SSAStatement(var, SSAStatement.Op.Null, var.getName());
	   
	    
	    this.body.add(ret);
	}
	
	return ret;
    }

    // This
    @Override public Object visit(ThisExp thisExp)
    {
	SSAStatement ret = new SSAStatement(thisExp, SSAStatement.Op.This, null);
	
	this.body.add(ret);
	return ret;
    }

    // NewIntArray
    @Override public Object visit(NewIntArrayExp intArray)
    {
	SSAStatement size = (SSAStatement)(intArray.getSize().accept(this));
	SSAStatement ret = new SSAStatement(intArray, SSAStatement.Op.NewIntArray, size, null, null);
	
	this.body.add(ret);
	return ret;
    }

    // NewObject
    @Override public Object visit(NewObjectExp newObj)
    {
	SSAStatement ret = new SSAStatement(newObj, SSAStatement.Op.NewObj, newObj.getName());
	
	this.body.add(ret);
	return ret;
    }

    public void compileReturn(Exp retExp)
    {
        // ...
	SSAStatement ret;

	ret = new SSAStatement(retExp, SSAStatement.Op.Return, (SSAStatement)retExp.accept(this) , null, null);
	
	this.body.add(ret);
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
