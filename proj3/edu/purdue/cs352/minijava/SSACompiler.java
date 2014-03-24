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

    // Global position counter for parameters
    public static int pos = 0;

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
        for(Parameter para : method.getParameters())
	{
	    //para.accept(compiler);
	    parameters.add((SSAStatement)para.accept(compiler));
	}

	for(SSAStatement param : parameters)
	{
	    String name = ((Parameter)param.getASTNode()).getName();
	    SSAStatement varAssg = new SSAStatement(method, SSAStatement.Op.VarAssg, param, null, name);

	    compiler.symbolTable.put(name, varAssg);
	    compiler.body.add(varAssg);
	}

	// Reset position for next parameter list
	pos = 0;

        // and the variable declarations
        for(VarDecl var : method.getVarDecls())
	{
	    var.accept(compiler);
	}

        // then compile the body
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

    // Parameters
    @Override public Object visit(Parameter param)
    {
	
	SSAStatement ret = new SSAStatement(param, SSAStatement.Op.Parameter, this.pos);
	
	this.body.add(ret);
	
	pos++;
	return ret;
    }

    // VarDecl
    @Override public Object visit(VarDecl varDecl)
    {
	SSAStatement ret;

	ret = new SSAStatement(varDecl, SSAStatement.Op.Null, varDecl.getType());
	
	this.symbolTable.put(varDecl.getName(), ret);

	this.body.add(ret);
	return ret;
    }

    // ExpStatement
    @Override public Object visit(ExpStatement expStatement)
    {
	return expStatement.getExp().accept(this);

    }

    // Exp
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
	    ret = (SSAStatement)((BinaryExp)exp).accept(this);
	}
	else if(exp instanceof IntLiteralExp)
	{
	    ret = (SSAStatement)((IntLiteralExp)exp).accept(this);
	}
	else
        {
            throw new Error("Invalid Expression: " + exp.getClass().getSimpleName());
        }

	return ret;
    }

    // BlockStatement
    @Override public Object visit(BlockStatement block)
    {
	for (Statement stmt : block.getBody())
	    stmt.accept(this);

	return null;
    }

    // IfStatement
    @Override public Object visit(IfStatement ifStatement)
    {
	SSAStatement ret = null;
	
	// Get current time to make sure to avoid label collisions
	long currTime = System.nanoTime() / 1000;

	// Create temporary compiler to keep track of symbolTable
	SSACompiler temp = new SSACompiler();
	temp.symbolTable = new HashMap<String, SSAStatement>(this.symbolTable);

	// Condition
	SSAStatement condition = (SSAStatement)ifStatement.getCondition().accept(this);

	// Not Branch
	SSAStatement notBranch = new SSAStatement(ifStatement, SSAStatement.Op.NBranch, condition, null);
	this.body.add(notBranch);

	// If Part
	SSAStatement ifPart = (SSAStatement)ifStatement.getIfPart().accept(this);
	
	// Goto Statement
	SSAStatement gotoStatement = new SSAStatement(ifStatement, SSAStatement.Op.Goto);
	this.body.add(gotoStatement);

	// Else Label
	SSAStatement elseLabel = new SSAStatement(ifStatement, SSAStatement.Op.Label, ("IfElse-" + currTime));
	this.body.add(elseLabel);

	// Else Part
	if(ifStatement.getElsePart() != null)
	{
	    SSAStatement elsePart = (SSAStatement)ifStatement.getElsePart().accept(temp);
	}

	// Add the temp body back to this body
	for(SSAStatement ssa : temp.getBody())
	    this.body.add(ssa);

	// Final Label
	SSAStatement finalLabel = new SSAStatement(ifStatement, SSAStatement.Op.Label, "IfFinal-" + currTime);
	this.body.add(finalLabel);

	// Set special fields of statements that were declared before these existed
	notBranch.setSpecial(elseLabel.getSpecial());
	gotoStatement.setSpecial(finalLabel.getSpecial());

	// Unify
	for (Map.Entry<String, SSAStatement> sym : (this.symbolTable).entrySet())
	{
	    SSAStatement originalEntry = sym.getValue();
	    SSAStatement newEntry =  temp.symbolTable.get(sym.getKey());
	    
	    // Update the changed variables
	    if(newEntry != null && originalEntry.getIndex() != newEntry.getIndex())
	    {
		SSAStatement unify = new SSAStatement(ifStatement, SSAStatement.Op.Unify, originalEntry, newEntry);
		this.body.add(unify);
		this.symbolTable.put(sym.getKey(), unify);
	    }
	}	
	
	ret = notBranch;
	return ret;
    }

    // While Statement
    @Override public Object visit(WhileStatement whileStatement)
    {
	SSACompiler temp = new SSACompiler();
	temp.symbolTable = new HashMap<String, SSAStatement>(this.symbolTable);

	// Get current time to make sure to avoid label collisions
	long currTime = System.nanoTime() / 1000;

	// Condition Label
	SSAStatement conditionLabel = new SSAStatement(whileStatement, SSAStatement.Op.Label, ("WhileCondition-" + currTime));
	this.body.add(conditionLabel);
	
	// Condition
	SSAStatement condition = (SSAStatement)whileStatement.getCondition().accept(this);

	// Not Branch
	SSAStatement notBranch = new SSAStatement(whileStatement, SSAStatement.Op.NBranch, condition, null);
	this.body.add(notBranch);

	// Loop Body
	SSAStatement body = (SSAStatement)whileStatement.getBody().accept(temp);
	
	// Add the temp body back to this body
	for(SSAStatement ssa : temp.getBody())
	    this.body.add(ssa);

	// Goto Statement
	SSAStatement gotoStatement = new SSAStatement(whileStatement, SSAStatement.Op.Goto, conditionLabel.getSpecial());
	this.body.add(gotoStatement);

	// Final Label
	SSAStatement finalLabel = new SSAStatement(whileStatement, SSAStatement.Op.Label, ("WhileEnd-" + currTime));
	this.body.add(finalLabel);

	notBranch.setSpecial(finalLabel.getSpecial());

	for (Map.Entry<String, SSAStatement> sym : (this.symbolTable).entrySet())
	{
	    SSAStatement originalEntry = sym.getValue();
	    SSAStatement newEntry =  temp.symbolTable.get(sym.getKey());
	    
	    if(newEntry != null && originalEntry.getIndex() != newEntry.getIndex())
	    {
		SSAStatement unify = new SSAStatement(whileStatement, SSAStatement.Op.Unify, originalEntry, newEntry);
		this.body.add(unify);
		this.symbolTable.put(sym.getKey(), unify);
	    }
	}

	return notBranch;
    }

    // PrintStatement
    @Override public Object visit(PrintStatement statement)
    {
        SSAStatement ret;
	
	SSAStatement value = (SSAStatement)((PrintStatement)statement).getValue().accept(this);
	
	ret = new SSAStatement(statement, SSAStatement.Op.Print, value, null, null);

	this.body.add(ret);
	return ret;

    }

    // Assign Expression
    @Override public Object visit(AssignExp exp)
    {
        // what sort of statement we make, if any, depends on the LHS
        Exp target = exp.getTarget();
        SSAStatement ret;
        
	// Assign to variable
	if (target instanceof VarExp )
        { 
	    String name = ((VarExp)target).getName();
		
	    // Check if this variable is in the symbol table, if not its a member of this
	    if(this.symbolTable.get(name) == null)
	    {
		SSAStatement thisExp = new SSAStatement(exp, SSAStatement.Op.This, null);
		this.body.add(thisExp);

		SSAStatement value = (SSAStatement)exp.getValue().accept(this);
		ret = new SSAStatement(exp, SSAStatement.Op.MemberAssg, thisExp, value, name);
	    }
	    else
	    {
		SSAStatement value = (SSAStatement)exp.getValue().accept(this);
		ret = new SSAStatement(exp, SSAStatement.Op.VarAssg, value, null, name);            
		this.symbolTable.put(name, ret);
	    }
	}
	// Assign to member
        else if (target instanceof MemberExp )
        {
	    String name = ((MemberExp)target).getMember();
	   
	    SSAStatement member = (SSAStatement)((MemberExp)target).getSub().accept(this);

	    SSAStatement value = (SSAStatement)exp.getValue().accept(this);
            ret = new SSAStatement(exp, SSAStatement.Op.MemberAssg, member.getLeft(), value, name);

        }
	// Assign to array
        else if (target instanceof IndexExp)
        {
	    SSAStatement array = (SSAStatement)((IndexExp)target).getTarget().accept(this);
	    SSAStatement index = (SSAStatement)((IndexExp)target).getIndex().accept(this);	    
	    SSAStatement value = (SSAStatement)exp.getValue().accept(this);
            
	    ret = new SSAStatement(exp, SSAStatement.Op.IndexAssg, array, value, index);
        }
        else
        {
            throw new Error("Invalid LHS: " + target.getClass().getSimpleName());
        }

	this.body.add(ret);
        return ret;
    }

    // Binary Expression
    @Override public Object visit(BinaryExp binExp)
    {
	SSAStatement ret;
	
	SSAStatement left = (SSAStatement)(binExp.getLeft().accept(this));
	SSAStatement right = (SSAStatement)(binExp.getRight().accept(this));
	SSAStatement.Op op = determineOp(binExp.getOp());

	ret = new SSAStatement(binExp, op, left, right);

	this.body.add(ret);
	return ret;
    }

    // NotExp
    @Override public Object visit(NotExp not)
    {
	SSAStatement ret = new SSAStatement(not, SSAStatement.Op.Not, (SSAStatement)not.getSub().accept(this), null);

	this.body.add(ret);
	return ret;
    }
    // IndexExp
    @Override public Object visit(IndexExp indexExp)
    {
	SSAStatement ret = new SSAStatement(indexExp, SSAStatement.Op.Index, (SSAStatement)indexExp.getTarget().accept(this), (SSAStatement)indexExp.getIndex().accept(this));

	this.body.add(ret);
	return ret;
    }

    // CallExp
    @Override public Object visit(CallExp callExp)
    {
	SSAStatement ret;

	SSACompiler newCompiler = new SSACompiler();

	SSAStatement target = (SSAStatement)(callExp.getTarget().accept(this));

	// Build an SSAStatement argument list from Exp list
	ArrayList<SSAStatement> args = new ArrayList<SSAStatement>();
	int index = 0;
	for(Exp argument : callExp.getArguments())
	{
	    SSAStatement arg = new SSAStatement(callExp, SSAStatement.Op.Arg, (SSAStatement)argument.accept(this), null, index);
	    args.add(arg);
	    this.body.add(arg);
	    index++;
	}

	SSACall ssaCall = new SSACall(callExp.getMethod(), args);

	ret = new SSAStatement(callExp, SSAStatement.Op.Call, target, null, ssaCall);

	this.body.add(ret);
	return ret;
    }

    // MemberExp
    @Override public Object visit(MemberExp member)
    {
	SSAStatement ret = new SSAStatement(member, SSAStatement.Op.Member, (SSAStatement)member.getSub().accept(this), null, member.getMember());

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
	    SSAStatement thisExp = new SSAStatement(var, SSAStatement.Op.This, null);
	    this.body.add(thisExp);
	
	    ret = new SSAStatement(var, SSAStatement.Op.Member, thisExp, null, var.getName());
	    
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

    // Return
    public void compileReturn(Exp retExp)
    {
	SSAStatement ret;

	ret = new SSAStatement(retExp, SSAStatement.Op.Return, (SSAStatement)retExp.accept(this) , null, null);
	
	this.body.add(ret);
    }

    // Convert operation tokens to enums
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
