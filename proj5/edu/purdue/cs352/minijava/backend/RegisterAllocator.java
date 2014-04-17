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
		boolean isSpill = false;
		boolean isPinned = false;
		boolean isRemoved = false;
		
		public Variable var;
		public Set<TempNode> adjacentNodes = new HashSet<TempNode>();
		public Set<TempNode> tempAdjacentNodes = new HashSet<TempNode>();

		public TempNode(Variable var)
		{
			this.var = var;
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
	List<Variable> variableList;
	HashMap<SSAStatement, Variable> ssaVariableMap;
	Set<Variable> useSet;
	List<CFNode> cfgNodeList;
	HashMap<SSAStatement, CFNode> ssaCFGMap;
	List<TempNode> tempNodeList;
	HashMap<Variable, TempNode> varTempMap;
	
	HashMap<String, CFNode> labelMap;
	List<TempNode> potentialSpillList = new ArrayList<TempNode>();
	List<TempNode> pinnedNodeList = new ArrayList<TempNode>();

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
		Set<TempNode> actualSpills = new HashSet<TempNode>();

		RegisterAllocator ra = new RegisterAllocator();
		ra.block = block;

		while (true) 
		{
			/* FILLIN: This body may work fine, in which case you will have to
			 * write the relevant functions, or you may prefer to implement it
			 * differently */
			//ra.possibleSpillList.clear();
			ra.potentialSpillList.clear();
			ra.pinnedNodeList.clear();
			actualSpills.clear();

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
			if (actualSpills.size() == 0) break;
		
			// OK, rewrite to perform the spills
			ra.performSpills(actualSpills);
		}

		// FILLIN: now, using the information from the interference graph, assign the register for each SSA statement
		ra.setRegisters();

		return ra.block;
	}

	// FILLIN: Implement the methods used by alloc() above

	// Init Variables
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
	}

	// Unify
	private void unifyVariables()
	{
		for(SSAStatement statement : block)
		{
			// Add everything into everything else
			if(statement.getOp() == SSAStatement.Op.Unify)
			{
				SSAStatement left = statement.getLeft();
				SSAStatement right = statement.getRight();

				Variable unifyVar = ssaVariableMap.get(statement);
				Variable leftVar = ssaVariableMap.get(left);
				Variable rightVar = ssaVariableMap.get(right);
				
				unifyVar.v.addAll(leftVar.v);
				unifyVar.v.addAll(rightVar.v);

				leftVar.v.addAll(unifyVar.v);
				leftVar.v.addAll(rightVar.v);

				rightVar.v.addAll(unifyVar.v);
				rightVar.v.addAll(leftVar.v);

				for(SSAStatement state : unifyVar.v)
				{
					ssaVariableMap.put(state, unifyVar);
				}
			}
		}
	}

	// Init CFG Nodes
	private void initCFNodes()
	{
		cfgNodeList = new ArrayList<CFNode>();
		ssaCFGMap = new HashMap<SSAStatement, CFNode>();
		labelMap = new HashMap<String, CFNode>();

		for(SSAStatement statement : block)
		{
			CFNode node = new CFNode();
			cfgNodeList.add(node);
			node.def = ssaVariableMap.get(statement);
			ssaCFGMap.put(statement, node);
			
			// Save labels
			if(statement.getOp() == SSAStatement.Op.Label)
				labelMap.put(((String)statement.getSpecial()), node);
		}
	}

	// Uses
	private void addUses()
	{
		for(SSAStatement statement : block)
		{
			SSAStatement left = statement.getLeft();
			SSAStatement right = statement.getRight();

			if(left != null)
			{
				ssaCFGMap.get(statement).use.add(ssaVariableMap.get(left));
			}

			if(right != null)
			{
				ssaCFGMap.get(statement).use.add(ssaVariableMap.get(right));
			}
			
			if(statement.getOp() == SSAStatement.Op.Call)
			{
				for(SSAStatement arg : ((SSACall)statement.getSpecial()).getArgs())
				{
					ssaCFGMap.get(statement).use.add(ssaVariableMap.get(arg));
				}
			}
			
			if(statement.getOp() == SSAStatement.Op.IndexAssg)
			{
				ssaCFGMap.get(statement).use.add(ssaVariableMap.get((SSAStatement)statement.getSpecial()));
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
				
				// Add label to successor set and this to label's predecessor set
				CFNode label = labelMap.get(((String)statement.getSpecial()));
				
				node.succ.add(label);
				label.pred.add(node);
			}
			else if(statement.getOp() == SSAStatement.Op.Goto)
			{
				if(prev >= 0)
					node.pred.add(cfgNodeList.get(prev));
				
				// Add label to successor set and this to label's predecessor set
				CFNode label = labelMap.get(((String)statement.getSpecial()));
				
				node.succ.add(label);
				label.pred.add(node);
			}
			else if(statement.getOp() == SSAStatement.Op.Label)
			{
				// If preceding node has label as successor, add the node as predecessor
				if(prev >= 0 && cfgNodeList.get(prev).succ.contains(ssaCFGMap.get(statement)))
					node.pred.add(cfgNodeList.get(prev));
				
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
	}

	private void liveness()
	{
		// Flip list so we can do liveness bottom up
		Collections.reverse(cfgNodeList);

		while(true)
		{
			boolean outHasChanged = false;
			boolean inHasChanged = false;
		
			for(CFNode node : cfgNodeList)
			{
				if(node.out.add(node.def))
					outHasChanged = true;
				
				for(CFNode succNode : node.succ)
				{
					if(node.out.addAll(succNode.in))
						outHasChanged = true;
				}	
				
				Set<Variable> outTemp = new HashSet<Variable>();
				outTemp.addAll(node.out);
				
				outTemp.remove(node.def);
				
				if(node.in.addAll(outTemp))
					inHasChanged = true;
					
				if(node.in.addAll(node.use))
					inHasChanged = true;
			}
			
			if(!outHasChanged && !inHasChanged)
				break;
		}

		// Restore list
		Collections.reverse(cfgNodeList);
	}

	private void initTempNodes()
	{
		tempNodeList = new ArrayList<TempNode>();
		varTempMap = new HashMap<Variable, TempNode>();
		
		for(Variable var : variableList)
		{
			TempNode temp = new TempNode(var);
			tempNodeList.add(temp);
			
			// Account for pinned variables
			for(SSAStatement s : var.v)
			{
				if(s.registerPinned())
				{
					temp.registerNum = s.getRegister();
					temp.isPinned = true;
					pinnedNodeList.add(temp);
				}
			}
			varTempMap.put(var, temp);
		}
	}

	private void buildInterference()
	{
		for(CFNode node : cfgNodeList)
		{
			// Add in set nodes to adjacent nodes
			for(Variable inVar : node.in)
			{
				TempNode inTemp = varTempMap.get(inVar);
				
				for(Variable adjacentVar : node.in)
				{
					TempNode adjacentTemp = varTempMap.get(adjacentVar);
					
					if(adjacentVar != null && inVar != adjacentVar && !inTemp.adjacentNodes.contains(adjacentTemp))
						inTemp.adjacentNodes.add(adjacentTemp);
				}
				
				inTemp.tempAdjacentNodes.addAll(inTemp.adjacentNodes);
			}
			
			// Add out set nodes to adjacent nodes
			for(Variable outVar : node.out)
			{
				TempNode outTemp = varTempMap.get(outVar);
				
				for(Variable adjacentVar : node.out)
				{
					TempNode adjacentTemp = varTempMap.get(adjacentVar);
					
					if(adjacentVar != null && outVar != adjacentVar && !outTemp.adjacentNodes.contains(adjacentTemp))
						outTemp.adjacentNodes.add(adjacentTemp);
				}
				
				outTemp.tempAdjacentNodes.addAll(outTemp.adjacentNodes);
			}
		}
	}

	private void simplify(int freeRegisters)
	{		
		// Remove pinned nodes first
		for(TempNode pinned : pinnedNodeList)
		{
			if(!pinned.isRemoved && pinned.tempAdjacentNodes.size() < freeRegisters)
			{
				// Remove this node from the adjacent lists of all others
				for(TempNode removeNode : pinned.tempAdjacentNodes)
				{
					removeNode.tempAdjacentNodes.remove(pinned);
				}
				pinned.tempAdjacentNodes.clear();
				
				pinned.isRemoved = true;
			}
		}
		
		List<TempNode> interferenceGraph = new ArrayList<TempNode>();
		
		for(TempNode alive : tempNodeList)
		{
			if(!alive.isRemoved)
			{
				interferenceGraph.add(alive);
			}
		}
		
		// Remove any nodes that we can from interference graph
		for(TempNode node : interferenceGraph)
		{
			if(node.tempAdjacentNodes.size() < freeRegisters)
			{
				// Remove this node from the adjacent lists of all others
				for(TempNode removeNode : node.tempAdjacentNodes)
				{
					removeNode.tempAdjacentNodes.remove(node);
				}
			
				node.tempAdjacentNodes.clear();
				node.isRemoved = true;
			}
		}
		
		// If the graph isn't empty there are potential spills
		if(interferenceGraph.size() != 0)
		{
			TempNode spill = interferenceGraph.get(0);
			
			spill.isSpill = true;
			
			for(TempNode removeNode : spill.tempAdjacentNodes)
			{
				removeNode.tempAdjacentNodes.remove(spill);
			}
			
			spill.tempAdjacentNodes.clear();
			
			// Remove this node
			interferenceGraph.remove(spill);
			potentialSpillList.add(spill);
			
			// Repeat simplify
			simplify(freeRegisters);
		}
	}

	private Set<TempNode> select(int freeRegisters)
	{			
		Set<TempNode> spillSet = new HashSet<TempNode>();
		List<Integer> pinnedRegisters = new ArrayList<Integer>();
		
		for(TempNode pinned : pinnedNodeList)
		{
			pinnedRegisters.add(pinned.registerNum);
		}
		
		// Add safe nodes back into graph
		for(TempNode node : tempNodeList)
		{
			int currRegister = 0;
			
			if(!node.isSpill)
			{
				List<Integer> illegalRegisters = new ArrayList<Integer>();
				for(TempNode adj : node.adjacentNodes)
				{
					illegalRegisters.add(adj.registerNum);
				}
				
				while(pinnedRegisters.contains(currRegister) || illegalRegisters.contains(currRegister))
				{
					currRegister++;
					currRegister %= freeRegisters;
				}
				
				node.registerNum = currRegister;
			}
		}
		
		// try to add potential spills into graph
		for(TempNode potentialSpill : potentialSpillList)
		{
			int currRegister = 0;
		
			List<Integer> illegalRegisters = new ArrayList<Integer>();
			for(TempNode adj : potentialSpill.adjacentNodes)
			{
				illegalRegisters.add(adj.registerNum);
			}
	
			int spillCatch = currRegister;
			boolean spilled = false;
			
			while(pinnedRegisters.contains(currRegister) || illegalRegisters.contains(currRegister))
			{
				currRegister++;
				currRegister %= freeRegisters;
			
				if(spillCatch == currRegister)
				{
					spilled = true;
					spillSet.add(potentialSpill);
					break;	
				}
			}
			
			if(!spilled)
				potentialSpill.registerNum = currRegister;
		}
		
		return spillSet;
	}

	private void performSpills(Set<TempNode> actualSpills)
	{
		int stackOffset = 0;
		
		for(TempNode spill : actualSpills)
		{
			SSAStatement store = new SSAStatement(null, SSAStatement.Op.Store, spill.var.master, null, stackOffset);
			
			SSAStatement load = new SSAStatement(null, SSAStatement.Op.Load, null, null, stackOffset);
			stackOffset++;
			
			List<SSAStatement> fakeBlock = new ArrayList<SSAStatement>(block);
			
			// Loop through a cloned block to avoid a concurrent modification exception
			for(SSAStatement statement : fakeBlock)
			{
				if(spill.var.v.contains(statement))
				{
					block.add(fakeBlock.indexOf(statement)+1, store);
				}
				else
				{
					// Update variable references to point to the load
					if(spill.var.v.contains(statement.getLeft()))					
					{
						block.add(fakeBlock.indexOf(statement)+1, load);
						statement.setLeft(load);
					}
					if(spill.var.v.contains(statement.getRight()))
					{
						block.add(fakeBlock.indexOf(statement)+1, load);
						statement.setRight(load);
					}
					if(statement.getOp() == SSAStatement.Op.Call)
					{
						for(SSAStatement arg : ((SSACall)statement.getSpecial()).getArgs())
						{
							if(spill.var.v.contains(arg))
								arg = load;
						}
					}
					if(statement.getOp() == SSAStatement.Op.IndexAssg)
					{
						if(spill.var.v.contains(((SSAStatement)statement.getSpecial())))
							statement.setSpecial(load);
					}
				}
			}
		}
	}
	
	private void setRegisters()
	{
		for(SSAStatement statement : block)
		{
			if(!statement.registerPinned())
			{
				statement.setRegister(varTempMap.get(ssaVariableMap.get(statement)).registerNum);
			}
		}
	}
	
	private void printCFGNodes()
	{
		System.out.println("***** CFG Nodes *****");
		
		for(CFNode node : cfgNodeList)
		{
			System.out.print("CFGNode " + node.def.master.getIndex() + ": Use: ");
			
			Iterator it = node.use.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((Variable)it.next()).master.getIndex() + ", ");
			}
			
			System.out.print(" Pred: ");
			
			it = node.pred.iterator();
			
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
			
			System.out.print(" In: ");
			
			it = node.in.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((Variable)it.next()).master.getIndex() + ", ");
			}
			
			System.out.print(" Out: ");
			
			it = node.out.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((Variable)it.next()).master.getIndex() + ", ");
			}
			
			System.out.print("\n");
		}
		System.out.println("***** CFG Nodes *****");
	}
	
	private void printTempNodes()
	{
		System.out.println("***** Temp Nodes *****");
		
		for(TempNode node : tempNodeList)
		{
		
			System.out.print(" Temp Node " + node.var.master.getIndex() + ": ");
			Iterator it = node.tempAdjacentNodes.iterator();
			
			while(it.hasNext())
			{
				System.out.print(((TempNode)it.next()).var.master.getIndex() + ", ");
			}
			
			System.out.print("\n");
		}
		
		System.out.println("***** Temp Nodes *****");
	}
}
