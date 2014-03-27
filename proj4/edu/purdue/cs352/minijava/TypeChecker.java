package edu.purdue.cs352.minijava;

import java.util.*;

import edu.purdue.cs352.minijava.ast.*;
import edu.purdue.cs352.minijava.ssa.*;
import edu.purdue.cs352.minijava.types.*;

public class TypeChecker {
    SSAProgram prog;
    Map<String, StaticType> types;

    public TypeChecker(SSAProgram prog) 
    {
        this.prog = prog;
        types = new HashMap<String, StaticType>();
        types.put("Object", new ObjectType("Object", null));
    }

    public void typeCheck() 
    {
        // FILLIN
        // (Hint: Create types, then assign types to SSAFields/SSAMethods, then check types in SSAStatements)
	
	// Add Primitive types
	types.put("int", new PrimitiveType.IntType());
	types.put("boolean", new PrimitiveType.BooleanType());
	types.put("void", new VoidType());
	types.put("int[]", new ObjectType("int[]", (ObjectType)types.get("Object")));
			    
	try
	{
	    // Add class types to map
	    for(SSAClass cl : prog.getClassesOrdered())
	    {
		String className = cl.getASTNode().getName();
    		SSAClass supercl = cl.superclass(prog);

		// Add class type to map
	        if(supercl != null)
			types.put(className, new ObjectType(className, (ObjectType)types.get(supercl.getASTNode().getName())));
		else
			types.put(className, new ObjectType(className, (ObjectType)types.get("Object")));    
		
	    }

	    //printMap();

	    // Set field and method types 
	    for(SSAClass cl : prog.getClassesOrdered())
	    {
		for(SSAField field : cl.getFieldsOrdered())
		{
		    field.setType(types.get(field.getField().getType().getName()));
		}

		// Set method return types and parameter types
		for(SSAMethod method : cl.getMethodsOrdered())
		{
		    method.setRetType(types.get(method.getMethod().getType().getName()));

		    ArrayList<StaticType> params = new ArrayList<StaticType>();
		    for(Parameter parameter : method.getMethod().getParameters())
		    {
			params.add(types.get(parameter.getType().getName()));
		    }

		    method.setParamTypes(params);
		}
	    }

	    // Check main statements
	    for (SSAStatement statement : prog.getMain().getBody())
		StatementCheck(statement, null, prog.getMain());

	    // Check method statements
	    for(SSAClass cl : prog.getClassesOrdered())
		for(SSAMethod method : cl.getMethodsOrdered())
		    for(SSAStatement statement : method.getBody())
			StatementCheck(statement, cl, method);

	   
	}
	catch(TypeException e)
	{
	    System.out.println(e.message);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    // Switch based on SSAStatement operation
    private void StatementCheck(SSAStatement statement, SSAClass cl, SSAMethod method) throws TypeException
    {
	switch(statement.getOp())
    	{
	    case Unify:
		UnifyCheck(statement);
		break;

	    case Alias:
		statement.setType(statement.getRight().getType());
    		break;

	    case This:
		statement.setType(types.get(cl.getASTNode().getName()));
		break;

	    case Parameter:
		statement.setType(method.getParamType((Integer)statement.getSpecial()));
		break;

	    case Arg:
		statement.setType(statement.getLeft().getType());
    		break;

	    case Null:
		statement.setType(types.get(((Type)statement.getSpecial()).getName()));
		break;
			    
	    // Primitive types
	    case Int:
		statement.setType(types.get("int"));
    		break;

	    case Boolean:
		statement.setType(types.get("boolean"));
    		break;

	    // New objects
	    case NewObj:
		String typeName = (String)statement.getSpecial();
		statement.setType(types.get(typeName));
    		break;

	    case NewIntArray:
		NewIntArrayCheck(statement);
		break;
			
	    // Control flow
	    case Label:
		statement.setType(types.get("void"));
    		break;

	    case Goto:
		statement.setType(types.get("void"));
    		break;
			    
	    case Branch:
	    case NBranch:
		BranchCheck(statement);
		break;

	    // Function call
	    case Call:
		CallCheck(statement);
		break;
			    
	    // Print
	    case Print:
		PrintCheck(statement); 
    		break;

	    case Return:
		if(!statement.getLeft().getType().subtypeOf(method.getRetType()))
		{
		    throw new TypeException("Return type is invalid");
		}
		statement.setType(types.get("void"));
  		break;

	    // Member and index access
	    case Member:
		MemberCheck(statement);			
    		break;

	    case Index:
		IndexCheck(statement);
    		break;

	    // Assignment
	    case VarAssg:
		VarAssgCheck(statement, cl, method);
 		break;

	    case MemberAssg:
		MemberAssgCheck(statement);
		break;

	    case IndexAssg:
	    	IndexAssgCheck(statement);
		break;
			    
	    // Not
	    case Not:
	    	NotCheck(statement);
		break;

	    // Binary Expressions	
	    case Lt:
	    case Le:
	    case Gt:
	    case Ge:
	    	CompareCheck(statement);  
		break;

	    case Eq:
	    case Ne:
    		statement.setType(types.get("boolean"));   			    
		break;

	    case And:
	    case Or:
	    	LogicCheck(statement);  
		break;

	    case Plus:
	    case Minus:
	    case Mul:
	    case Div:
	    case Mod:
	    	ArithmeticCheck(statement);  			    
    		break;
	}

    }

    // Unify
    private void UnifyCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(leftType.equals(rightType))
	{
	    statement.setType(leftType);
	    return;
	}

	StaticType commonType = leftType.commonSupertype(rightType);

	if(commonType == null)
	{
	    throw new TypeException("Unify statements are not comparable");
	}

	statement.setType(commonType);
    }

    // NewIntArray
    private void NewIntArrayCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
				
	if(!leftType.equals(types.get("int")))
	{
	    throw new TypeException("Left operand to newIntArray must be int. Found: " + leftType);
	}
				
	statement.setType(types.get("int[]"));
    }

    // Branch, NBranch
    private void BranchCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();

	if(!leftType.equals(types.get("boolean")))
	{
	    throw new TypeException("Branch condition expected boolean but found " + leftType);
	}
	
	statement.setType(types.get("void"));
    }

    // Function calls
    private void CallCheck(SSAStatement statement) throws TypeException
    {
	SSAClass clazz = prog.getClass(statement.getLeft().getType().toString());
	SSACall call = (SSACall)statement.getSpecial();
    
	SSAMethod meth = clazz.getMethod(prog, call.getMethod());
	
	if(meth == null)
	{
	    throw new TypeException("No method exists in class");
	}

	// Get argument types	
	int i = 0;
	List<StaticType> paramType = meth.getParamTypes();
	List<StaticType> argType = new ArrayList<StaticType>();

	for(SSAStatement arg : call.getArgs())
	    argType.add(arg.getType());

	// Check for correct number of arguments
	if(paramType.size() != argType.size())
	{
	    throw new TypeException("Incorrect number of arguments to " + meth.getMethod().getName());
	}


	while(i < paramType.size())
	{
	    if(!argType.get(i).subtypeOf(paramType.get(i)))
	    {
		throw new TypeException("Invalid arguments to " + call.getMethod());
	    }
	    i++;
        }
	statement.setType(meth.getRetType());
    }

    // Print
    private void PrintCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
			       
	if(!leftType.equals(types.get("int")))
	{
	    throw new TypeException("Print target must be int but found " + leftType);
	}
	
	statement.setType(types.get("void"));
    }

    // Member
    private void MemberCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	
	if(leftType.toString().equals("int[]"))
	{
	    if(!((String)statement.getSpecial()).equals("length"))
	    {
		throw new TypeException("Invalid int[] member access");
	    }

	    statement.setType(types.get("int"));
	    return;
	}

	SSAClass clazz = prog.getClass(leftType.toString());
	String fieldName = (String)statement.getSpecial();
	
	SSAField field = clazz.getField(prog, fieldName);

	if(field == null)
	{
	    throw new TypeException("Field does not exist in class");
	}

	statement.setType(field.getType());
    }

    // Index
    private void IndexCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
    	StaticType rightType = statement.getRight().getType();
			        
	if(!leftType.equals(types.get("int[]"))
	    || !rightType.equals(types.get("int")))
	{			    
	    throw new TypeException("Invalid operands for index expression: " + leftType + " " + rightType);
	}
	
	statement.setType(types.get("int"));
    }

    // VarAssg
    private void VarAssgCheck(SSAStatement statement, SSAClass cl, SSAMethod method) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType varType = null;
	String varName = (String)statement.getSpecial();

	SSAField var = cl.getField(prog, varName);

	if(var == null)
	{
	    // Search vardecls for the given variable
	    for(VarDecl varDecl : method.getMethod().getVarDecls())
	    {	
		if(varDecl.getName().equals(varName))
		{
		    varType = types.get(varDecl.getType().getName());
		    break;
		}
	    }
	    if(varType == null)
	    {
		// Search parameters for the given variable
		for(Parameter param : method.getMethod().getParameters())
		{	
		    if(param.getName().equals(varName))
		    {
			varType = types.get(param.getType().getName());
			break;
		    }
		}

		// Throw exception if variable not found
		if(varType == null)
	        {
		  throw new TypeException("Local variable " + varName + " does not exist on line " + statement.getIndex());
		}
	    }
    	}
	else
	{
	    varType = var.getType();
	}

	if(leftType.subtypeOf(varType))
	{
	    statement.setType(varType);    
	}
	else
	{
	    throw new TypeException(leftType + " is not a subtype of " + var.getType());
	}
    }

    // MemberAssg
    private void MemberAssgCheck(SSAStatement statement) throws TypeException
    {
	SSAClass clazz = prog.getClass(statement.getLeft().getType().toString());
	
	String fieldName = (String)statement.getSpecial();
	
	SSAField field = clazz.getField(prog, fieldName);

	if(field == null)
	{
	    throw new TypeException("Field does not existin class");
	}

	statement.setType(statement.getRight().getType());
    }

    // IndexAssg
    private void IndexAssgCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("int[]"))
	    || !rightType.equals(types.get("int")))
	{
	    throw new TypeException("Invalid index assignment operands: " + leftType + " " + rightType);
	}
	
	statement.setType(types.get("int"));
    }

    // Not
    private void NotCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();

	if(!leftType.equals(types.get("boolean")))
	{
	    throw new TypeException("Not value must be boolean but found " + leftType);
	}

	statement.setType(types.get("boolean"));
    }

    // Lt, Le, Gt, Ge
    private void CompareCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();
				
	if(!leftType.equals(types.get("int"))
	    || !rightType.equals(types.get("int")))
	{
	    throw new TypeException("Invalid operand type for comparitive operator "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("boolean"));  
    }

    //And, Or
    private void LogicCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("boolean"))
	    || !rightType.equals(types.get("boolean")))
	{
	    throw new TypeException("Invalid operand for logic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("boolean")); 
    }

    // Mul, Div, Add, Minus, Mod
    private void ArithmeticCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("int"))
	    || !rightType.equals(types.get("int")))
	{
	    throw new TypeException("Invalid operand for arithmetic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("int")); 	
    }

    // Debug method to print out the type names in map
    public void printMap()
    {
	System.out.println("Type Table:");
	System.out.println("***************************");

	for(Map.Entry<String, StaticType> type : types.entrySet())
	    System.out.println(type.getKey());
    
	System.out.println("***************************");
    }
}

class TypeException extends Exception
{
    String message;
    TypeException(String message)
    {
	this.message = message;
    }
};
