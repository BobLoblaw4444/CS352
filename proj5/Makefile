JAVAC=javac
JAVACC=./javacc/bin/javacc

BASE=edu/purdue/cs352/minijava

JJ_SOURCE= \
    $(BASE)/parser/Lexer.jj \
    $(BASE)/parser/Parser.jj \
    $(BASE)/parser/ParserAST.jj

JJ_BASES=$(JJ_SOURCE:.jj=)

JJ_JAVA_SOURCE= \
    $(JJ_SOURCE:.jj=.java) \
    $(JJ_SOURCE:.jj=Constants.java) \
    $(JJ_SOURCE:.jj=TokenManager.java)

JAVA_SOURCE=$(JJ_JAVA_SOURCE) \
    $(BASE)/ASTToSExp.java \
    $(BASE)/ParserFrontend.java \
    $(BASE)/SSACompiler.java \
    $(BASE)/SSACompilerFrontend.java \
    $(BASE)/TypeChecker.java \
    $(BASE)/MIPSCompilerFrontend.java \
    $(BASE)/X8664CompilerFrontend.java \
    \
    $(BASE)/interpreter/ASTInterpreter.java \
    $(BASE)/interpreter/SSAInterpreter.java \
    \
    $(BASE)/parser/JavaCharStream.java \
    $(BASE)/parser/LexerManual.java \
    $(BASE)/parser/ParseException.java \
    $(BASE)/parser/ParserManual.java \
    $(BASE)/parser/Token.java \
    $(BASE)/parser/TokenMgrError.java \
    \
    $(BASE)/ast/ASTNode.java \
    $(BASE)/ast/ASTVisitor.java \
    $(BASE)/ast/AssignExp.java \
    $(BASE)/ast/BinaryExp.java \
    $(BASE)/ast/BlockStatement.java \
    $(BASE)/ast/BooleanLiteralExp.java \
    $(BASE)/ast/CallExp.java \
    $(BASE)/ast/ClassDecl.java \
    $(BASE)/ast/Exp.java \
    $(BASE)/ast/ExpStatement.java \
    $(BASE)/ast/IfStatement.java \
    $(BASE)/ast/IndexExp.java \
    $(BASE)/ast/IntLiteralExp.java \
    $(BASE)/ast/Main.java \
    $(BASE)/ast/MemberExp.java \
    $(BASE)/ast/MethodDecl.java \
    $(BASE)/ast/NewIntArrayExp.java \
    $(BASE)/ast/NewObjectExp.java \
    $(BASE)/ast/NotExp.java \
    $(BASE)/ast/Parameter.java \
    $(BASE)/ast/PrintStatement.java \
    $(BASE)/ast/Program.java \
    $(BASE)/ast/Statement.java \
    $(BASE)/ast/ThisExp.java \
    $(BASE)/ast/Type.java \
    $(BASE)/ast/TypeBoolean.java \
    $(BASE)/ast/TypeInt.java \
    $(BASE)/ast/TypeIntArray.java \
    $(BASE)/ast/VarDecl.java \
    $(BASE)/ast/VarExp.java \
    $(BASE)/ast/WhileStatement.java \
    \
    $(BASE)/ssa/SSACall.java \
    $(BASE)/ssa/SSAClass.java \
    $(BASE)/ssa/SSAField.java \
    $(BASE)/ssa/SSAMethod.java \
    $(BASE)/ssa/SSAProgram.java \
    $(BASE)/ssa/SSAStatement.java \
    \
    $(BASE)/types/ObjectType.java \
    $(BASE)/types/PrimitiveType.java \
    $(BASE)/types/StaticType.java \
    $(BASE)/types/VoidType.java \
    \
    $(BASE)/backend/AsmMIPS.java \
    $(BASE)/backend/AsmX8664.java \
    $(BASE)/backend/ClassLayout.java \
    $(BASE)/backend/RegisterAllocator.java

CLASS_BASES=$(JAVA_SOURCE:.java=)

CLASSES=$(JAVA_SOURCE:.java=.class)

# The lexer is used as a sentinel
SENTINEL=edu/purdue/cs352/minijava/parser/Lexer.class
all: $(SENTINEL)

native: $(BINS)

# Build .java files from .jj files
%.java %Constants.java %TokenManager.java: %.jj
	$(JAVACC) -OUTPUT_DIRECTORY=$(BASE)/parser $<

# Build .class files from .java files
$(SENTINEL): $(JJ_JAVA_SOURCE) edu/purdue/cs352/minijava/*.java edu/purdue/cs352/minijava/*/*.java
	$(JAVAC) `find $(BASE) -name '*.java'`
%.class: $(SENTINEL)

# And some template rules for when .template files are provided
%.class: %.java-template
	touch $@
%.class: %.jj-template
	touch $@
%Constants.class: %.jj-template
	touch $@
%TokenManager.class: %.jj-template
	touch $@
%.java:
	@true

# Clean rule is complexified by the existence of templates: We don't want to
# remove .class files which have no corresponding .java/.jj file
clean:
	@for i in $(CLASS_BASES); do \
	    if [ -e "$$i".java ]; then \
	        echo rm -f "$$i".class "$$i"\$$*.class; \
	        rm -f "$$i".class "$$i"\$$*.class; \
	    fi; \
	done
	@for i in $(JJ_BASES); do \
	    if [ -e "$$i".jj ]; then \
		echo rm -f "$$i".java "$$i"Constants.java "$$i"TokenManager.java; \
		rm -f "$$i".java "$$i"Constants.java "$$i"TokenManager.java; \
	    fi; \
	done

# Keep all intermediate files
.SECONDARY:
