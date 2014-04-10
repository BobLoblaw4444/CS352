package edu.purdue.cs352.minijava.backend;

import java.util.*;

import edu.purdue.cs352.minijava.ssa.*;

public class RegisterAllocator 
{
	/* a "variable" is just a set of SSA statements. We alias it (sort of) to
	 * make this explicit. A Variable is thus equivalently a definition of
	 * def(v). */
	static class Variable 
	{
		public final SSAStatement master; // just for debugging
		public final Set<SSAStatement> v;
		public Variable(SSAStatement s) 
		{
			master = s;
			v = new HashSet<SSAStatement>();
			v.add(s);
		}
	}

	/* you will additionally need classes for at least:
	 * Nodes in the control flow graph
	 * Nodes in the variable interference graph
	 */

	// a node in the control flow graph
	class CFNode 
	{
		// FILLIN...
		// will need at least pred, succ, def[n], use[n], in[n] and out[n]
		Set<CFNode> pred = new HashSet<CFNode>();
		Set<CFNode> succ = new HashSet<CFNode>();
		//Set<Variable> def = new HashSet<Variable>();
		Variable def;
		Set<Variable> use = new HashSet<Variable>();
		Set<Variable> in = new HashSet<Variable>();
		Set<Variable> out = new HashSet<Variable>();
	}

	// a node in the interference graph (a temporary)
	class TempNode 
	{
		// FILLIN...
		int registerNum = -1;
		public Variable var;
		public Set<TempNode> adjacentNodes;

		public TempNode(Variable var)
		{
			this.var = var;
		}

		public void addNode(TempNode node)
		{
			adjacentNodes.add(node);
		}
	}

	// the block we're performing allocation over
	List<SSAStatement> block;

	/* FILLIN: You may additionally need fields for, e.g.,
	 * The number of spills you have performed (to fill the special field of Store and Load operations),
	 * Your list of variables,
	 * Your SSAStatement->variable binding (def(v)),
	 * Your list of CFG nodes,
	 * Your SSAStatement->CFG node binding,
	 * use(v),
	 * Your list of interference graph nodes,
	 * Your binding of variables to nodes in the interference graph.
	 */
	int numSpillsPerformed;
	List<Variable> variableList;
	HashMap<SSAStatement, Variable> ssaVariableMap;
	Set<Variable> useSet;
	List<CFNode> cfgNodeList;
	HashMap<SSAStatement, CFNode> ssaCFGMap;
	List<TempNode> tempNodes;
	HashMap<Variable, TempNode> varTempMap;
	
	HashMap<String, CFNode> labelMap;


	private RegisterAllocator() {}

	// perform all register allocations for this program
	public static void alloc(SSAProgram prog, int freeRegisters) {
		// first main
		SSAMethod main = prog.getMain();
		main.setBody(alloc(main.getBody(), freeRegisters));

		// then each class
		for (SSAClass cl : prog.getClassesOrdered())
			alloc(cl, freeRegisters);
	}

	// perform all register allocations for this class
	public static void alloc(SSAClass cl, int freeRegisters) {
		for (SSAMethod m : cl.getMethodsOrdered())
			alloc(m, freeRegisters);
	}

	// perform register allocation for this method
	public static void alloc(SSAMethod m, int freeRegisters) {
		m.setBody(alloc(m.getBody(), freeRegisters));
	}

	// the register allocator itself
	public static List<SSAStatement> alloc(List<SSAStatement> block, int freeRegisters) 
	{
		Set<TempNode> actualSpills;

		RegisterAllocator ra = new RegisterAllocator();
		ra.block = block;

		while (true) 
		{
			/* FILLIN: This body may work fine, in which case you will have to
			 * write the relevant functions, or you may prefer to implement it
			 * differently */

			// prefill the variables with single statements
			ra.initVariables();

			// unify
			ra.unifyVariables();

			// now build the CF nodes
			ra.initCFNodes();

			// build the use[n] relationship from them
			ra.addUses();

			// build their successor/predecessor relationships
			ra.cfPredSucc();

			// liveness analysis
			ra.liveness();

			// build the temporaries
			ra.initTempNodes();

			// and figure out their interference
			ra.buildInterference();

			ra.simplify(freeRegisters);

			// do we need to spill?
			actualSpills = ra.select(freeRegisters);
			//if (actualSpills.size() == 0) break;

			// OK, rewrite to perform the spills
			ra.performSpills(actualSpills);
			break;
		}

		// FILLIN: now, using the information from the interference graph, assign the register for each SSA statement

		return ra.block;
	}

	// FILLIN: Implement the methods used by alloc() above

	private void initVariables()
	{
		variableList  = new ArrayList<Variable>();
		ssaVariableMap = new HashMap<SSAStatement, Variable>();

		for(SSAStatement statement : block)
		{
			Variable newVar = new Variable(statement);
			variableList.add(newVar);
			ssaVariableMap.put(statement, newVar);
		}

		/*System.out.println("********** InitVariables **********");
		for(Variable var : variableList)
		{
			Iterator it = var.v.iterator();
			while(it.hasNext())
			{
				System.out.println(it.next());
			}
		}
		System.out.println("********** InitVariables **********");*/

	}

	private void unifyVariables()
	{
		for(Variable variable : variableList)
		{
			Iterator it = variable.v.iterator();
			if(it.hasNext())
			{
				SSAStatement temp = (SSAStatement) it.next();
				if(temp.getOp() == SSAStatement.Op.Unify)
				{
					SSAStatement left = temp.getLeft();
					SSAStatement right = temp.getRight();

					variable.v.add(left);
					variable.v.add(right);

					ssaVariableMap.remove(left);
					ssaVariableMap.remove(right);

					ssaVariableMap.put(left, variable);
					ssaVariableMap.put(right, variable);
				}
			}
		}

		/*System.out.println("********** UnifyVariables **********");
		for(Map.Entry<SSAStatement, Variable> temp : ssaVariableMap.entrySet())
		{
			System.out.println(temp.getKey() + " ---> " + temp.getValue().master);
		}
		System.out.println("********** UnifyVariables **********");*/
	}

	private void initCFNodes()
	{
		cfgNodeList = new ArrayList<CFNode>();
		ssaCFGMap = new HashMap<SSAStatement, CFNode>();
		labelMap = new HashMap<String, CFNode>();

		for(SSAStatement statement : block)
		{
			CFNode node = new CFNode();
			//node.def.add(ssaVariableMap.get(statement));
			cfgNodeList.add(node);
			node.def = ssaVariableMap.get(statement);
			ssaCFGMap.put(statement, node);
			
			if(statement.getOp() == SSAStatement.Op.Label)
				labelMap.put(((String)statement.getSpecial()), node);
		}
		/*
		System.out.println("********** InitCFNodes **********");
		for(Map.Entry<SSAStatement, CFNode> temp : ssaCFGMap.entrySet())
		{
			System.out.println(temp.getKey() + " ---> " + temp.getValue().def.get();
		}
		System.out.println("********** InitCFNodes **********");*/
	}

	private void addUses()
	{
		for(CFNode node : cfgNodeList)
		{
			Iterator iter = node.def.v.iterator();
			if(iter.hasNext())
			{
				SSAStatement statement = (SSAStatement)iter.next();
				if(statement.getOp() == SSAStatement.Op.Unify)
				{
					node.use.add(ssaVariableMap.get(statement));
				}
				else
				{
					SSAStatement left = statement.getLeft();
					SSAStatement right = statement.getRight();

					if(left != null)
					{
						node.use.add(ssaVariableMap.get(left));
					}

					if(right != null)
					{
						node.use.add(ssaVariableMap.get(right));
					}
				}
			}
		}
	}

	// Pred
	private void cfPredSucc()
	{	
		for(SSAStatement statement : block)
		{
			CFNode node = ssaCFGMap.get(statement);
			int next = cfgNodeList.indexOf(node) + 1;
			int prev = next - 2;
			
			if(statement.getOp() == SSAStatement.Op.Branch
				|| statement.getOp() == SSAStatement.Op.NBranch)
			{				
				if(prev >= 0)
					node.pred.add(cfgNodeList.get(prev));
				
				if(next < cfgNodeList.size())
					node.succ.add(cfgNodeList.get(next));
				
				CFNode label = labelMap.get(((String)statement.getSpecial()));
				node.succ.add(label);
				label.pred.add(node);
			}
			else if(statement.getOp() == SSAStatement.Op.Goto)
			{
				if(prev >= 0)
					node.pred.add(cfgNodeList.get(prev));
				
				CFNode label = labelMap.get(((String)statement.getSpecial()));
				node.succ.add(label);
				label.pred.add(node);
			}
			else if(statement.getOp() == SSAStatement.Op.Label)
			{
				if(prev >= 0 && cfgNodeList.get(prev).succ.contains(ssaCFGMap.get(statement)))
					node.pred.add(cfgNodeList.get(prev));
				
				//node.pred.add(labelMap.get(((String)statement.getSpecial())));
				
				if(next < cfgNodeList.size())
					node.succ.add(cfgNodeList.get(next));
			}
			else
			{
				if(next < cfgNodeList.size())
					node.succ.add(cfgNodeList.get(next));
				
				if(prev >= 0)
					node.pred.add(cfgNodeList.get(prev));
			}
		}
		
		System.out.println("***** CFG Nodes *****");
		
		for(CFNode node : cfgNodeList)
		{
			System.out.print("CFGNode " + node.def.master.getIndex() + ": Pred: ");
			
			Iterator it = node.pred.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((CFNode)it.next()).def.master.getIndex() + ", ");
			}
			
			System.out.print(" Succ: ");
			
			it = node.succ.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((CFNode)it.next()).def.master.getIndex() + ", ");
			}
			
			System.out.print("\n");
		}
		System.out.println("***** CFG Nodes *****");
	}

	private void liveness()
	{
		Collections.reverse(cfgNodeList);
		
		for(CFNode node : cfgNodeList)
		{
			
		}
		
		Collections.reverse(cfgNodeList);
	}

	private void initTempNodes()
	{
			
	}

	private void buildInterference()
	{
			
	}

	private void simplify(int freeRegisters)
	{
		
	}

	private Set<TempNode> select(int freeRegisters)
	{
		return null;
	}

	private void performSpills(Set<TempNode> actualSpills)
	{
		
	}
}
